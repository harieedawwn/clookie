package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AwakeCheckTest {
    private val t0 = 1_800_000_000_000L
    private val min = 60_000L

    private fun waiting() = AwakeState.Idle.on(AwakeEvent.Dismissed(7, check = true, minutes = 4, now = t0))

    @Test
    fun asksFourMinutesAfterDismissal() {
        val (s, fx) = waiting()
        assertEquals(AwakeState.Waiting(7, t0 + 4 * min), s)
        assertTrue(fx.contains(AwakeEffect.ScheduleAt(t0 + 4 * min)))
        // Woken early (clock or reboot): just registers the same time again.
        assertEquals(s to listOf(AwakeEffect.ScheduleAt(t0 + 4 * min)), s.on(AwakeEvent.Due(t0 + 3 * min)))
        val (asking, ask) = s.on(AwakeEvent.Due(t0 + 4 * min))
        assertEquals(AwakeState.Asking(7, t0 + 4 * min + 90_000), asking)
        assertEquals(listOf(AwakeEffect.Ask(t0 + 4 * min + 90_000), AwakeEffect.ScheduleAt(t0 + 4 * min + 90_000)), ask)
    }

    @Test
    fun answerWithin90SecondsEndsTheCheck() {
        val asking = waiting().first.on(AwakeEvent.Due(t0 + 4 * min)).first
        val (s, fx) = asking.on(AwakeEvent.Confirmed)
        assertEquals(AwakeState.Idle, s)
        assertEquals(listOf(AwakeEffect.ClearAsk, AwakeEffect.CancelSchedule), fx)
        assertEquals(AwakeState.Idle to emptyList<AwakeEffect>(), s.on(AwakeEvent.Due(t0 + 6 * min)))
    }

    @Test
    fun noAnswerRingsTheSameAlarmAgainAfter90Seconds() {
        val asking = waiting().first.on(AwakeEvent.Due(t0 + 4 * min)).first
        assertEquals(asking, asking.on(AwakeEvent.Due(t0 + 4 * min + 89_000)).first)
        val (s, fx) = asking.on(AwakeEvent.Due(t0 + 4 * min + 90_000))
        assertEquals(AwakeState.Idle, s)
        assertEquals(listOf(AwakeEffect.ClearAsk, AwakeEffect.Ring(7)), fx)
    }

    @Test
    fun offAlarmsAndSnoozesDoNotCheck() {
        assertEquals(AwakeState.Idle to emptyList<AwakeEffect>(), AwakeState.Idle.on(AwakeEvent.Dismissed(7, false, 4, t0)))
        // Snoozing or ringing again during the wait cancels the pending check.
        assertEquals(AwakeState.Idle, waiting().first.on(AwakeEvent.Confirmed).first)
    }

    @Test
    fun staleChecksAfterThePhoneWasOffAreDropped() {
        val (s, fx) = waiting().first.on(AwakeEvent.Due(t0 + 4 * min + AwakeRules.StaleMs + 1))
        assertEquals(AwakeState.Idle, s)
        assertEquals(listOf(AwakeEffect.CancelSchedule), fx)
    }
}
