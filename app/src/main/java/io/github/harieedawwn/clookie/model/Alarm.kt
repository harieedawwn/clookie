package io.github.harieedawwn.clookie.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class MissionType { Classic, CookieCode, Qr, Math, Shake, Steps, Typing, Memory, Crumbs }

enum class SleeperProfile { Light, Medium, Deep }

enum class VibrationPattern { Off, Heartbeat, Steady, Crescendo }

/** Regular alarm, quick nap (one exact instant, deleted after it rings), timer (Stage 5). */
enum class AlarmKind { Alarm, Nap, Timer }

/** Repeat days as a bitmask, bit 0 = Monday ... bit 6 = Sunday (week starts Monday). */
@JvmInline
value class Days(val mask: Int) {
    operator fun contains(day: DayOfWeek) = mask and (1 shl (day.value - 1)) != 0
    fun isSet(index: Int) = mask and (1 shl index) != 0
    fun toggle(index: Int) = Days(mask xor (1 shl index))
    val isOneTime: Boolean get() = mask == 0

    companion object {
        val Weekdays = Days(0b0011111)
        val Weekend = Days(0b1100000)
        val Every = Days(0b1111111)
        val Once = Days(0)
    }
}

/**
 * One alarm. Wall-clock fields ([hour], [minute], [days]) follow the device time zone;
 * [oneShotAt] (naps, timers) and [snoozedUntil] are absolute instants.
 * [skipDate] is the date of the one occurrence skipped by "Bỏ qua lần tới".
 */
@Immutable
data class Alarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val days: Days = Days.Once,
    val label: String = "",
    val sound: String = DefaultSound,
    val volume: Int = 80,
    val rampSeconds: Int = 20,
    val vibration: VibrationPattern = VibrationPattern.Heartbeat,
    val snoozeMinutes: Int = 9,
    val snoozeLimit: Int = 3,
    val missions: List<MissionSpec> = listOf(MissionSpec(MissionType.Classic)),
    val profile: SleeperProfile? = null,
    val skipDate: LocalDate? = null,
    val enabled: Boolean = true,
    val kind: AlarmKind = AlarmKind.Alarm,
    val oneShotAt: Instant? = null,
    val snoozedUntil: Instant? = null,
    val snoozeCount: Int = 0,
    val volumeLock: Boolean = false,
    val snoozeAfterMission: Boolean = false,
    val awakeCheck: Boolean = false,
    val awakeCheckMinutes: Int = 4,
) {
    val minutesOfDay: Int get() = hour * 60 + minute
    val missionTypes: List<MissionType> get() = missions.map { it.type }

    companion object {
        const val DefaultSound = "clookie:crunch"
        const val MaxMissions = 3
    }
}

/** Search window for repeat patterns: two weeks covers any day mask plus one skip. */
private const val SearchDays = 15L

/**
 * Next scheduled occurrence strictly after [now], honoring repeat days and [Alarm.skipDate].
 * Snoozes are not included (see [nextRing]). DST: a time inside a spring-forward gap rings at
 * the shifted time; a time inside a fall-back overlap rings once, at the earlier offset.
 */
fun Alarm.nextTrigger(now: ZonedDateTime): ZonedDateTime? {
    if (!enabled) return null
    oneShotAt?.let { at -> return at.atZone(now.zone).takeIf { it.isAfter(now) } }
    val time = LocalTime.of(hour, minute)
    val today = now.toLocalDate()
    for (offset in 0L until SearchDays) {
        val date = today.plusDays(offset)
        if (!days.isOneTime && date.dayOfWeek !in days) continue
        val candidate = ZonedDateTime.of(date, time, now.zone)
        if (!candidate.isAfter(now)) continue
        if (!days.isOneTime && date == skipDate) continue
        return candidate
    }
    return null
}

/** Next time the alarm actually rings: a pending snooze or the next occurrence, whichever is first. */
fun Alarm.nextRing(now: ZonedDateTime): ZonedDateTime? {
    if (!enabled) return null
    val next = nextTrigger(now)
    val snooze = snoozedUntil?.atZone(now.zone)?.takeIf { it.isAfter(now) } ?: return next
    return if (next == null || snooze.isBefore(next)) snooze else next
}

/** Wall-clock convenience for UI code that ticks a [LocalDateTime] (device zone). */
fun Alarm.nextRing(now: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime? =
    nextRing(now.atZone(zone))?.withZoneSameInstant(zone)?.toLocalDateTime()

/** True when "skip next" currently skips the upcoming occurrence. */
fun Alarm.isSkippingNext(now: ZonedDateTime): Boolean {
    val skip = skipDate ?: return false
    if (days.isOneTime || oneShotAt != null) return false
    val unskipped = copy(skipDate = null, enabled = true).nextTrigger(now) ?: return false
    return unskipped.toLocalDate() == skip
}

/** Turns "skip next" on (skips the upcoming occurrence) or off. One-time alarms cannot skip. */
fun Alarm.withSkipNext(on: Boolean, now: ZonedDateTime): Alarm {
    if (!on || days.isOneTime || oneShotAt != null) return copy(skipDate = null)
    val upcoming = copy(skipDate = null, enabled = true).nextTrigger(now) ?: return copy(skipDate = null)
    return copy(skipDate = upcoming.toLocalDate())
}

/** Earliest upcoming ring and its time. */
fun List<Alarm>.nextUp(now: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Pair<Alarm, LocalDateTime>? =
    mapNotNull { a -> a.nextRing(now, zone)?.let { a to it } }.minByOrNull { it.second }

data class Countdown(val days: Long, val hours: Long, val minutes: Long)

/** Time left rounded up to the whole minute, like a stock clock ("in 7 h 12 min"). */
fun countdown(now: LocalDateTime, target: LocalDateTime): Countdown {
    val seconds = Duration.between(now, target).seconds.coerceAtLeast(0)
    val totalMinutes = (seconds + 59) / 60
    return Countdown(totalMinutes / 1440, (totalMinutes % 1440) / 60, totalMinutes % 60)
}
