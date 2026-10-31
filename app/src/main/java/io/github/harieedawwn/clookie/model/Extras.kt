package io.github.harieedawwn.clookie.model

import androidx.compose.runtime.Immutable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

// --- Wake log -----------------------------------------------------------------------------

/** One finished ring of a regular alarm (naps and timers are not logged). Times are epoch ms. */
@Immutable
data class WakeEntry(
    val label: String,
    val scheduledAt: Long,
    val firedAt: Long,
    val endedAt: Long,
    val outcome: RingOutcome,
    val snoozes: Int,
) {
    val timeToDismissMs: Long get() = (endedAt - firedAt).coerceAtLeast(0)

    /** On time = stopped without snoozing, within [WakeStats.OnTimeMs] of ringing. */
    val onTime: Boolean get() = outcome == RingOutcome.Dismissed && snoozes == 0 && timeToDismissMs <= WakeStats.OnTimeMs
}

/** A day of the 14-day chart: the first wake of that date, or null when no alarm rang. */
data class WakeDay(val date: LocalDate, val entry: WakeEntry?)

object WakeStats {
    const val OnTimeMs = 5 * 60_000L
    const val ChartDays = 14

    private fun WakeEntry.date(zone: ZoneId): LocalDate = Instant.ofEpochMilli(scheduledAt).atZone(zone).toLocalDate()

    /** The last [ChartDays] dates ending [today], each with its first ring (by scheduled time). */
    fun days(entries: List<WakeEntry>, today: LocalDate, zone: ZoneId): List<WakeDay> {
        val first = entries.groupBy { it.date(zone) }.mapValues { (_, e) -> e.minBy { it.scheduledAt } }
        return (ChartDays - 1 downTo 0).map { back -> today.minusDays(back.toLong()).let { WakeDay(it, first[it]) } }
    }

    /**
     * On-time streak: consecutive days with a ring whose first ring was on time, newest first.
     * Days without any alarm (weekends off) neither count nor break it; a late or missed day ends it.
     */
    fun streak(entries: List<WakeEntry>, zone: ZoneId): Int {
        val byDay = entries.groupBy { it.date(zone) }.toSortedMap(compareByDescending { it })
        var n = 0
        for ((_, list) in byDay) {
            if (list.minBy { it.scheduledAt }.onTime) n++ else break
        }
        return n
    }
}

// --- Stopwatch ----------------------------------------------------------------------------

/**
 * Stopwatch on the monotonic clock (SystemClock.elapsedRealtime, includes deep sleep), so it
 * keeps counting while the app is in the background and ignores wall-clock changes.
 * [startedAt] is the elapsed time when it last started (null = paused); [laps] are totals.
 */
@Immutable
data class Stopwatch(val accumulated: Long = 0, val startedAt: Long? = null, val laps: List<Long> = emptyList()) {
    val running: Boolean get() = startedAt != null
    val isReset: Boolean get() = accumulated == 0L && startedAt == null && laps.isEmpty()

    fun elapsed(now: Long): Long = accumulated + (startedAt?.let { (now - it).coerceAtLeast(0) } ?: 0)
    fun start(now: Long) = if (running) this else copy(startedAt = now)
    fun pause(now: Long) = if (!running) this else copy(accumulated = elapsed(now), startedAt = null)
    fun lap(now: Long) = if (!running) this else copy(laps = laps + elapsed(now))
    fun reset() = Stopwatch()

    /** Split of lap [i] (0-based). */
    fun split(i: Int): Long = laps[i] - (if (i == 0) 0 else laps[i - 1])

    /** Elapsed clocks restart at 0 on reboot: a start time in the future means the phone restarted. */
    fun afterReboot(now: Long): Stopwatch = if (startedAt != null && startedAt > now) copy(startedAt = now) else this

    fun encode(): String = listOf(accumulated, startedAt ?: -1).joinToString(",") + "|" + laps.joinToString(",")

    companion object {
        fun decode(s: String?): Stopwatch {
            if (s.isNullOrEmpty()) return Stopwatch()
            return runCatching {
                val (head, laps) = s.split('|', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                val (acc, start) = head.split(',').map(String::toLong)
                Stopwatch(acc, start.takeIf { it >= 0 }, laps.split(',').filter(String::isNotEmpty).map(String::toLong))
            }.getOrDefault(Stopwatch())
        }
    }
}

// --- Timer --------------------------------------------------------------------------------

/**
 * Countdown timer. Running: [endAt] (wall clock, rings through the alarm engine as alarm
 * [alarmId]). Paused: [remaining]. Idle: neither, ready with [duration].
 */
@Immutable
data class TimerState(
    val duration: Long = 5 * 60_000L,
    val endAt: Long? = null,
    val remaining: Long? = null,
    val alarmId: Long? = null,
) {
    val running: Boolean get() = endAt != null
    val paused: Boolean get() = endAt == null && remaining != null

    fun left(now: Long): Long = when {
        endAt != null -> (endAt - now).coerceAtLeast(0)
        remaining != null -> remaining
        else -> duration
    }

    fun encode(): String = listOf(duration, endAt ?: -1, remaining ?: -1, alarmId ?: -1).joinToString(",")

    companion object {
        val PresetMinutes = listOf(1, 3, 5, 10, 15, 25, 45, 60)

        fun decode(s: String?): TimerState = runCatching {
            val v = s!!.split(',').map(String::toLong)
            TimerState(v[0], v[1].takeIf { it >= 0 }, v[2].takeIf { it >= 0 }, v[3].takeIf { it >= 0 })
        }.getOrDefault(TimerState())
    }
}

/** Splits [ms] (rounded up to the second, like a countdown) into h, m, s. */
fun countdownParts(ms: Long): Triple<Int, Int, Int> {
    val s = (ms + 999) / 1000
    return Triple((s / 3600).toInt(), ((s % 3600) / 60).toInt(), (s % 60).toInt())
}

// --- Bedtime ------------------------------------------------------------------------------

object Bedtime {
    val GoalMinutes = listOf(360, 390, 420, 450, 480, 510, 540)
    const val DefaultGoal = 480

    /** Remind at the next alarm minus the sleep goal; null once that moment has passed. */
    fun reminderAt(nextAlarm: ZonedDateTime?, goalMinutes: Int, now: ZonedDateTime): ZonedDateTime? {
        val at = nextAlarm?.minusMinutes(goalMinutes.toLong()) ?: return null
        return at.takeIf { it.isAfter(now) }
    }
}
