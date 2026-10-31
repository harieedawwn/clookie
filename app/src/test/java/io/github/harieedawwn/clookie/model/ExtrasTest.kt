package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ExtrasTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    private fun wake(day: Int, stopAfterMin: Long, snoozes: Int = 0, outcome: RingOutcome = RingOutcome.Dismissed): WakeEntry {
        val at = ZonedDateTime.of(2026, 11, day, 6, 30, 0, 0, zone).toInstant().toEpochMilli()
        return WakeEntry("", at, at, at + stopAfterMin * 60_000, outcome, snoozes)
    }

    @Test
    fun streakCountsOnTimeDaysAndSkipsDaysWithoutAlarms() {
        // Nov 2 late, 3-4 on time, 5 no alarm, 6 on time.
        val log = listOf(wake(2, 12), wake(3, 1), wake(4, 2), wake(6, 0))
        assertEquals(3, WakeStats.streak(log, zone))
        assertEquals(0, WakeStats.streak(log + wake(7, 1, snoozes = 2), zone))
        assertEquals(0, WakeStats.streak(listOf(wake(7, 0, outcome = RingOutcome.Missed)), zone))
        assertFalse(wake(1, 6).onTime)
    }

    @Test
    fun chartHasFourteenDaysEndingToday() {
        val days = WakeStats.days(listOf(wake(3, 1), wake(6, 0)), LocalDate.of(2026, 11, 6), zone)
        assertEquals(14, days.size)
        assertEquals(LocalDate.of(2026, 11, 6), days.last().date)
        assertEquals(LocalDate.of(2026, 10, 24), days.first().date)
        assertEquals(2, days.count { it.entry != null })
    }

    @Test
    fun stopwatchUsesElapsedTimeAndSurvivesItsCodec() {
        var s = Stopwatch().start(1_000)
        s = s.lap(3_500).lap(4_000).pause(5_250)
        assertEquals(4_250, s.elapsed(99_999))
        assertEquals(listOf(2_500L, 500L), s.laps.indices.map(s::split))
        s = s.start(10_000)
        assertEquals(4_250 + 750, s.elapsed(10_750))
        assertEquals(s, Stopwatch.decode(s.encode()))
        // Reboot: elapsedRealtime restarted below the stored start.
        assertEquals(4_250L, s.afterReboot(200).elapsed(200))
        assertTrue(Stopwatch.decode("garbage").isReset)
    }

    @Test
    fun timerStateAndBedtime() {
        val t = TimerState(duration = 300_000, endAt = 1_000_000, alarmId = 4)
        assertEquals(t, TimerState.decode(t.encode()))
        assertEquals(120_000, t.left(880_000))
        assertEquals(0, t.left(2_000_000))
        assertEquals(Triple(1, 0, 1), countdownParts(3_600_500))

        val alarm = ZonedDateTime.of(2026, 11, 3, 6, 30, 0, 0, zone)
        val now = ZonedDateTime.of(2026, 11, 2, 21, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 2, 22, 30, 0, 0, zone), Bedtime.reminderAt(alarm, 480, now))
        assertNull(Bedtime.reminderAt(alarm, 480, now.plusHours(2)))
        assertNull(Bedtime.reminderAt(null, 480, now))
    }
}
