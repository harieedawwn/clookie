package io.github.harieedawwn.clookie.data

import android.content.Context
import io.github.harieedawwn.clookie.alarm.AlarmScheduler
import io.github.harieedawwn.clookie.data.db.AlarmEntity
import io.github.harieedawwn.clookie.data.db.ClookieDatabase
import io.github.harieedawwn.clookie.data.db.WakeLogEntity
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.AlarmKind
import io.github.harieedawwn.clookie.model.Days
import io.github.harieedawwn.clookie.model.LateFire
import io.github.harieedawwn.clookie.model.MissionCodec
import io.github.harieedawwn.clookie.model.RingOutcome
import io.github.harieedawwn.clookie.model.SleeperProfile
import io.github.harieedawwn.clookie.model.VibrationPattern
import io.github.harieedawwn.clookie.model.lateFire
import io.github.harieedawwn.clookie.model.nextRing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The only writer of alarms. Every change re-registers the alarm with AlarmManager and
 * records the registered instant ([AlarmEntity.pendingTriggerAt]) so a reschedule after
 * boot or a clock change can tell which alarms were missed.
 */
class AlarmRepository private constructor(context: Context) {
    private val dao = ClookieDatabase.get(context).alarms()
    private val scheduler = AlarmScheduler(context)
    private val lock = Mutex()

    val alarms: Flow<List<Alarm>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun get(id: Long): Alarm? = dao.get(id)?.toModel()

    /** Insert or update; returns the id. */
    suspend fun save(alarm: Alarm): Long = lock.withLock {
        val existing = if (alarm.id != 0L) dao.get(alarm.id) else null
        val clean = if (alarm.enabled) alarm else alarm.copy(snoozedUntil = null, snoozeCount = 0)
        val id = dao.upsert(clean.toEntity(existing?.pendingTriggerAt, existing?.createdAt ?: System.currentTimeMillis()))
        val savedId = if (alarm.id != 0L) alarm.id else id
        sync(clean.copy(id = savedId))
        savedId
    }

    suspend fun setEnabled(alarm: Alarm, on: Boolean) {
        save(alarm.copy(enabled = on))
    }

    suspend fun delete(id: Long) = lock.withLock {
        scheduler.cancel(id)
        dao.delete(id)
    }

    /** Quick nap: a one-shot alarm [minutes] from now, removed after it rings. */
    suspend fun addNap(minutes: Int, label: String, defaults: Alarm): Long {
        val at = Instant.now().plusSeconds(minutes * 60L)
        val local = at.atZone(ZoneId.systemDefault())
        return save(
            defaults.copy(
                id = 0, hour = local.hour, minute = local.minute, days = Days.Once, label = label,
                kind = AlarmKind.Nap, oneShotAt = at, enabled = true, skipDate = null,
            ),
        )
    }

    /**
     * Re-register everything (boot, time or zone change, app update, exact-alarm grant).
     * Returns alarms that should ring right now (slightly late) and alarms that were missed.
     */
    suspend fun rescheduleAll(now: Long = System.currentTimeMillis()): Reschedule = lock.withLock {
        val late = mutableListOf<Pair<AlarmEntity, Long>>()
        val missed = mutableListOf<Pair<Alarm, Long>>()
        for (e in dao.all()) {
            val pending = e.pendingTriggerAt
            when (if (pending == null) LateFire.NotDue else lateFire(pending, now)) {
                LateFire.RingNow -> late += e to pending!!
                LateFire.Missed -> missed += e.toModel() to pending!!
                LateFire.NotDue -> sync(e.toModel(), now)
            }
        }
        // Only the most recent late alarm rings (it keeps pendingTriggerAt for the service to claim).
        val ringNow = late.maxByOrNull { it.second }
        late.filter { it !== ringNow }.forEach { (e, at) -> missed += e.toModel() to at }
        missed.forEach { (a, at) -> finishLocked(a.id, RingOutcome.Missed, at, firedAt = now, now = now) }
        Reschedule(ringNow?.let { (e, at) -> e.toModel() to at }, missed)
    }

    data class Reschedule(val ringNow: Pair<Alarm, Long>?, val missed: List<Pair<Alarm, Long>>)

    /**
     * The ring service claims a fired alarm. Returns null for a stale fire (alarm changed,
     * deleted or already handled). Registers the following occurrence right away.
     */
    suspend fun claimFire(id: Long, triggerAt: Long, now: Long = System.currentTimeMillis()): Alarm? = lock.withLock {
        val e = dao.get(id) ?: return null
        if (e.pendingTriggerAt != triggerAt) return null
        var alarm = e.toModel()
        val snooze = alarm.snoozedUntil
        if (snooze != null && snooze.toEpochMilli() <= triggerAt) alarm = alarm.copy(snoozedUntil = null)
        dao.upsert(alarm.toEntity(null, e.createdAt))
        sync(alarm, maxOf(now, triggerAt) + 1000)
        alarm
    }

    suspend fun snooze(id: Long, at: Long, count: Int) = lock.withLock {
        val e = dao.get(id) ?: return
        val alarm = e.toModel().copy(snoozedUntil = Instant.ofEpochMilli(at), snoozeCount = count)
        dao.upsert(alarm.toEntity(e.pendingTriggerAt, e.createdAt))
        sync(alarm)
    }

    /** End of a ring: clear snooze, disable one-time alarms, delete naps, write the wake log. */
    suspend fun finish(id: Long, outcome: RingOutcome, triggerAt: Long, firedAt: Long) = lock.withLock {
        finishLocked(id, outcome, triggerAt, firedAt, System.currentTimeMillis())
    }

    private suspend fun finishLocked(id: Long, outcome: RingOutcome, triggerAt: Long, firedAt: Long, now: Long) {
        val e = dao.get(id) ?: return
        val alarm = e.toModel()
        dao.log(
            WakeLogEntity(
                alarmId = id, label = alarm.label, scheduledAt = triggerAt, firedAt = firedAt, endedAt = now,
                outcome = outcome.name, snoozeCount = alarm.snoozeCount, missions = e.missions,
            ),
        )
        if (alarm.kind == AlarmKind.Nap) {
            scheduler.cancel(id)
            dao.delete(id)
            return
        }
        val oneTime = alarm.days.isOneTime || alarm.oneShotAt != null
        val done = alarm.copy(snoozedUntil = null, snoozeCount = 0, enabled = alarm.enabled && !oneTime)
        dao.upsert(done.toEntity(null, e.createdAt))
        sync(done, now)
    }

    /** Registers the next ring with AlarmManager (or cancels) and records it. Call under [lock]. */
    private suspend fun sync(alarm: Alarm, now: Long = System.currentTimeMillis()) {
        val next = alarm.nextRing(ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault()))
        if (next == null) {
            scheduler.cancel(alarm.id)
            dao.setPending(alarm.id, null)
        } else {
            val at = next.toInstant().toEpochMilli()
            scheduler.schedule(alarm.id, at)
            dao.setPending(alarm.id, at)
        }
    }

    companion object {
        @Volatile private var instance: AlarmRepository? = null

        fun get(context: Context): AlarmRepository = instance ?: synchronized(this) {
            instance ?: AlarmRepository(context.applicationContext).also { instance = it }
        }
    }
}

fun AlarmEntity.toModel() = Alarm(
    id = id,
    hour = hour,
    minute = minute,
    days = Days(daysMask),
    label = label,
    sound = sound,
    volume = volume,
    rampSeconds = rampSeconds,
    vibration = VibrationPattern.entries.firstOrNull { it.name == vibration } ?: VibrationPattern.Heartbeat,
    snoozeMinutes = snoozeMinutes,
    snoozeLimit = snoozeLimit,
    missions = MissionCodec.decode(missions),
    profile = SleeperProfile.entries.firstOrNull { it.name == profile },
    skipDate = skipDate?.let(LocalDate::ofEpochDay),
    enabled = enabled,
    kind = AlarmKind.entries.firstOrNull { it.name == kind } ?: AlarmKind.Alarm,
    oneShotAt = oneShotAt?.let(Instant::ofEpochMilli),
    snoozedUntil = snoozedUntil?.let(Instant::ofEpochMilli),
    snoozeCount = snoozeCount,
    volumeLock = volumeLock,
    snoozeAfterMission = snoozeAfterMission,
    awakeCheck = awakeCheck,
    awakeCheckMinutes = awakeCheckMinutes,
)

fun Alarm.toEntity(pendingTriggerAt: Long?, createdAt: Long) = AlarmEntity(
    id = id,
    hour = hour,
    minute = minute,
    daysMask = days.mask,
    label = label,
    sound = sound,
    volume = volume,
    rampSeconds = rampSeconds,
    vibration = vibration.name,
    snoozeMinutes = snoozeMinutes,
    snoozeLimit = snoozeLimit,
    missions = MissionCodec.encode(missions),
    profile = profile?.name,
    skipDate = skipDate?.toEpochDay(),
    enabled = enabled,
    kind = kind.name,
    oneShotAt = oneShotAt?.toEpochMilli(),
    snoozedUntil = snoozedUntil?.toEpochMilli(),
    snoozeCount = snoozeCount,
    volumeLock = volumeLock,
    snoozeAfterMission = snoozeAfterMission,
    awakeCheck = awakeCheck,
    awakeCheckMinutes = awakeCheckMinutes,
    pendingTriggerAt = pendingTriggerAt,
    createdAt = createdAt,
)
