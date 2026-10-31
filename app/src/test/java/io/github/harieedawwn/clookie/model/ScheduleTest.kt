package io.github.harieedawwn.clookie.model

import io.github.harieedawwn.clookie.ui.components.keypadAccepts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ScheduleTest {
    private val hcm = ZoneId.of("Asia/Ho_Chi_Minh")
    private val berlin = ZoneId.of("Europe/Berlin")

    // 2026-11-02 is a Monday.
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int, zone: ZoneId = hcm) = ZonedDateTime.of(y, mo, d, h, mi, 0, 0, zone)
    private val mondayNight = at(2026, 11, 2, 23, 18)

    @Test
    fun oneTimeAlarmRingsNextOccurrence() {
        assertEquals(at(2026, 11, 3, 6, 30), Alarm(1, 6, 30).nextTrigger(mondayNight))
        assertEquals(at(2026, 11, 2, 23, 30), Alarm(2, 23, 30).nextTrigger(mondayNight))
    }

    @Test
    fun oneTimeAlreadyPastTodayRingsTomorrow() {
        val sevenAm = at(2026, 11, 2, 7, 0)
        assertEquals(at(2026, 11, 3, 6, 30), Alarm(1, 6, 30).nextTrigger(sevenAm))
        // Exactly now is "already past": strictly after.
        assertEquals(at(2026, 11, 3, 7, 0), Alarm(1, 7, 0).nextTrigger(sevenAm))
    }

    @Test
    fun everyRepeatPattern() {
        val friday7 = at(2026, 11, 6, 7, 0)
        assertEquals(at(2026, 11, 9, 6, 30), Alarm(1, 6, 30, days = Days.Weekdays).nextTrigger(friday7))
        assertEquals(at(2026, 11, 7, 6, 30), Alarm(1, 6, 30, days = Days.Weekend).nextTrigger(friday7))
        assertEquals(at(2026, 11, 7, 6, 30), Alarm(1, 6, 30, days = Days.Every).nextTrigger(friday7))
        val monWedFri = Days(0b0010101)
        assertEquals(at(2026, 11, 9, 6, 30), Alarm(1, 6, 30, days = monWedFri).nextTrigger(friday7))
        assertEquals(at(2026, 11, 4, 6, 30), Alarm(1, 6, 30, days = monWedFri).nextTrigger(mondayNight))
        // Later the same day still counts for a repeating alarm.
        assertEquals(at(2026, 11, 6, 22, 0), Alarm(1, 22, 0, days = Days.Weekdays).nextTrigger(friday7))
    }

    @Test
    fun weekStartsMonday() {
        assertTrue(Days.Weekdays.isSet(0))
        assertFalse(Days.Weekdays.isSet(6))
        assertTrue(Days.Weekend.isSet(6))
        assertEquals(Days(0b1), Days.Once.toggle(0))
        val sundayOnly = Days(1 shl 6)
        // From Monday night the next Sunday is 2026-11-08.
        assertEquals(at(2026, 11, 8, 9, 0), Alarm(1, 9, 0, days = sundayOnly).nextTrigger(mondayNight))
        assertTrue(java.time.DayOfWeek.MONDAY in Days(0b1))
    }

    @Test
    fun crossingMidnight() {
        val beforeMidnight = at(2026, 11, 2, 23, 50)
        assertEquals(at(2026, 11, 3, 0, 10), Alarm(1, 0, 10).nextTrigger(beforeMidnight))
        // Friday 23:50, weekday alarm at 00:10: Saturday is not a weekday, so Monday.
        val friday = at(2026, 11, 6, 23, 50)
        assertEquals(at(2026, 11, 9, 0, 10), Alarm(1, 0, 10, days = Days.Weekdays).nextTrigger(friday))
        // Sunday 23:59 -> Monday 00:00 for a Monday-only alarm.
        assertEquals(at(2026, 11, 9, 0, 0), Alarm(1, 0, 0, days = Days(0b1)).nextTrigger(at(2026, 11, 8, 23, 59)))
    }

    @Test
    fun skipNextSkipsExactlyOneOccurrence() {
        val a = Alarm(1, 6, 30, days = Days.Every).withSkipNext(true, mondayNight)
        assertEquals(LocalDate.of(2026, 11, 3), a.skipDate)
        assertTrue(a.isSkippingNext(mondayNight))
        assertEquals(at(2026, 11, 4, 6, 30), a.nextTrigger(mondayNight))
        // Once the skipped day has passed the alarm rings normally again (flag needs no reset).
        val wednesdayNight = at(2026, 11, 4, 23, 0)
        assertEquals(at(2026, 11, 5, 6, 30), a.nextTrigger(wednesdayNight))
        assertFalse(a.isSkippingNext(wednesdayNight))
        // Turning it off restores the next occurrence; one-time alarms cannot skip.
        assertEquals(at(2026, 11, 3, 6, 30), a.withSkipNext(false, mondayNight).nextTrigger(mondayNight))
        assertNull(Alarm(2, 6, 30).withSkipNext(true, mondayNight).skipDate)
    }

    @Test
    fun skipNextOnWeekdaysSkipsWeekendGap() {
        val friday7 = at(2026, 11, 6, 7, 0)
        val a = Alarm(1, 6, 30, days = Days.Weekdays).withSkipNext(true, friday7)
        assertEquals(LocalDate.of(2026, 11, 9), a.skipDate)
        assertEquals(at(2026, 11, 10, 6, 30), a.nextTrigger(friday7))
    }

    @Test
    fun snoozeComesBeforeNextOccurrence() {
        val ringing = at(2026, 11, 3, 6, 30)
        val snoozed = Alarm(1, 6, 30, days = Days.Every, snoozedUntil = ringing.plusMinutes(9).toInstant())
        assertEquals(ringing.plusMinutes(9), snoozed.nextRing(ringing.plusMinutes(1)))
        // A snooze in the past is ignored; the next day's occurrence remains.
        assertEquals(at(2026, 11, 4, 6, 30), snoozed.nextRing(ringing.plusMinutes(10)))
        // Disabling drops everything.
        assertNull(snoozed.copy(enabled = false).nextRing(ringing))
        // One-time alarm snoozed after it fired still rings at the snooze.
        val once = Alarm(2, 6, 30, snoozedUntil = ringing.plusMinutes(9).toInstant())
        assertEquals(ringing.plusMinutes(9), once.nextRing(ringing.plusMinutes(2)))
    }

    @Test
    fun napIsAnExactInstant() {
        val now = at(2026, 11, 2, 13, 0)
        val nap = Alarm(1, 13, 20, kind = AlarmKind.Nap, oneShotAt = now.plusMinutes(20).plusSeconds(30).toInstant())
        assertEquals(now.plusMinutes(20).plusSeconds(30), nap.nextTrigger(now))
        assertNull(nap.nextTrigger(now.plusMinutes(21)))
    }

    @Test
    fun timezoneChangeKeepsWallClockForAlarmsAndInstantForNaps() {
        val instant = Instant.parse("2026-11-02T16:00:00Z") // 23:00 in Hồ Chí Minh, 17:00 in Berlin
        val alarm = Alarm(1, 7, 0, days = Days.Every)
        assertEquals(ZonedDateTime.of(2026, 11, 3, 7, 0, 0, 0, hcm), alarm.nextTrigger(instant.atZone(hcm)))
        assertEquals(ZonedDateTime.of(2026, 11, 3, 7, 0, 0, 0, berlin), alarm.nextTrigger(instant.atZone(berlin)))
        val nap = Alarm(2, 0, 0, oneShotAt = instant.plusSeconds(600))
        assertEquals(nap.nextTrigger(instant.atZone(hcm))!!.toInstant(), nap.nextTrigger(instant.atZone(berlin))!!.toInstant())
    }

    @Test
    fun daylightSavingGapAndOverlap() {
        // 2026-03-29 02:00 -> 03:00 in Berlin: 02:30 does not exist, rings at 03:30 CEST.
        val gap = Alarm(1, 2, 30).nextTrigger(at(2026, 3, 29, 1, 0, berlin))!!
        assertEquals(LocalDateTime.of(2026, 3, 29, 3, 30), gap.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(2), gap.offset)
        // 2026-10-25 03:00 -> 02:00: 02:30 happens twice; ring once, at the earlier offset.
        val first = Alarm(1, 2, 30, days = Days.Every).nextTrigger(at(2026, 10, 25, 1, 0, berlin))!!
        assertEquals(ZoneOffset.ofHours(2), first.offset)
        val afterFirst = ZonedDateTime.ofInstant(first.toInstant().plusSeconds(600), berlin)
        assertEquals(LocalDateTime.of(2026, 10, 26, 2, 30), Alarm(1, 2, 30, days = Days.Every).nextTrigger(afterFirst)!!.toLocalDateTime())
    }

    @Test
    fun disabledAlarmHasNoTrigger() {
        assertNull(Alarm(1, 6, 30, enabled = false).nextTrigger(mondayNight))
    }

    @Test
    fun nextUpPicksEarliestEnabled() {
        val up = SampleAlarms.list.nextUp(mondayNight.toLocalDateTime(), hcm)!!
        assertEquals(1L, up.first.id)
    }

    @Test
    fun lateAlarmsRingWithinGraceElseMissed() {
        val t = 1_000_000_000L
        assertEquals(LateFire.NotDue, lateFire(t + 1, t))
        assertEquals(LateFire.RingNow, lateFire(t - 14 * 60_000L, t))
        assertEquals(LateFire.Missed, lateFire(t - 16 * 60_000L, t))
    }

    @Test
    fun countdownRoundsUpToMinute() {
        val now = LocalDateTime.of(2026, 11, 2, 23, 18)
        assertEquals(Countdown(0, 7, 12), countdown(now.plusSeconds(1), LocalDateTime.of(2026, 11, 3, 6, 30)))
        assertEquals(Countdown(1, 1, 0), countdown(now, now.plusHours(25)))
    }

    @Test
    fun missionChainRoundTripsAndNeverEmpty() {
        val chain = listOf(MissionSpec(MissionType.Classic, mapOf("slides" to 2)), MissionSpec(MissionType.Math, mapOf("level" to 3, "count" to 5)))
        assertEquals(chain, MissionCodec.decode(MissionCodec.encode(chain)))
        assertEquals(listOf(MissionSpec(MissionType.Classic)), MissionCodec.decode(""))
        assertEquals(listOf(MissionSpec(MissionType.Classic)), MissionCodec.decode("Teleport{x=1}"))
    }

    @Test
    fun updaterComparesVersions() {
        assertTrue(isNewerVersion("v0.3.0", "0.2.0"))
        assertTrue(isNewerVersion("0.10.0", "0.9.9"))
        assertFalse(isNewerVersion("v0.2.0", "0.2.0"))
        assertFalse(isNewerVersion("0.1.9", "0.2.0"))
        assertFalse(isNewerVersion("nightly", "0.2.0"))
    }

    @Test
    fun keypadRejectsImpossibleTimes() {
        assertTrue(keypadAccepts("", 2))
        assertFalse(keypadAccepts("", 3))
        assertFalse(keypadAccepts("2", 4))
        assertTrue(keypadAccepts("1", 9))
        assertFalse(keypadAccepts("23", 6))
        assertTrue(keypadAccepts("235", 9))
        assertFalse(keypadAccepts("2359", 0))
    }
}
