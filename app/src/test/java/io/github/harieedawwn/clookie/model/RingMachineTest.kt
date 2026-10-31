package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RingMachineTest {
    private val t0 = 1_800_000_000_000L
    private val ringing = RingState.Ringing(alarmId = 7, triggerAt = t0, startedAt = t0)
    private val min = 60_000L

    @Test
    fun fireFromIdleStartsRinging() {
        val t = RingState.Idle.on(RingEvent.Fire(7, t0, t0 + 5))
        assertEquals(RingState.Ringing(7, t0, t0 + 5), t.state)
        assertEquals(listOf(RingEffect.StartRinging(7)), t.effects)
    }

    @Test
    fun duplicateFireIsIgnored() {
        assertEquals(RingTransition(ringing), ringing.on(RingEvent.Fire(7, t0, t0 + 1000)))
    }

    @Test
    fun anotherAlarmReplacesTheRingingOne() {
        val t = ringing.on(RingEvent.Fire(8, t0 + min, t0 + min))
        assertEquals(RingState.Ringing(8, t0 + min, t0 + min), t.state)
        assertEquals(listOf(RingEffect.Finish(7, RingOutcome.Replaced, t0), RingEffect.StartRinging(8)), t.effects)
    }

    @Test
    fun snoozeWithinLimitStopsAndSchedules() {
        val t = ringing.on(RingEvent.Snooze(t0 + min, minutes = 9, count = 1, limit = 3))
        assertEquals(RingState.Idle, t.state)
        assertEquals(listOf(RingEffect.StopRinging, RingEffect.ScheduleSnooze(7, t0 + 10 * min, 2)), t.effects)
    }

    @Test
    fun snoozeAtLimitOrOffKeepsRinging() {
        assertEquals(RingTransition(ringing), ringing.on(RingEvent.Snooze(t0, 9, count = 3, limit = 3)))
        assertEquals(RingTransition(ringing), ringing.on(RingEvent.Snooze(t0, 9, count = 0, limit = 0)))
    }

    @Test
    fun dismissFinishes() {
        val t = ringing.on(RingEvent.Dismiss(t0 + min))
        assertEquals(RingState.Idle, t.state)
        assertEquals(listOf(RingEffect.StopRinging, RingEffect.Finish(7, RingOutcome.Dismissed, t0)), t.effects)
    }

    @Test
    fun unattendedRingTimesOutAsMissed() {
        assertEquals(RingTransition(ringing), ringing.on(RingEvent.Tick(t0 + 29 * min)))
        val t = ringing.on(RingEvent.Tick(t0 + 30 * min))
        assertEquals(RingState.Idle, t.state)
        assertEquals(listOf(RingEffect.StopRinging, RingEffect.Finish(7, RingOutcome.Missed, t0)), t.effects)
    }

    @Test
    fun restoreAfterProcessDeathOrRebootResumesOrReportsMissed() {
        assertEquals(RingTransition(ringing, listOf(RingEffect.StartRinging(7))), ringing.on(RingEvent.Restore(t0 + 3 * min)))
        val late = ringing.on(RingEvent.Restore(t0 + 45 * min))
        assertEquals(RingState.Idle, late.state)
        assertEquals(listOf(RingEffect.Finish(7, RingOutcome.Missed, t0)), late.effects)
    }

    @Test
    fun idleIgnoresEverythingButFire() {
        listOf(
            RingEvent.Snooze(t0, 9, 0, 3), RingEvent.Dismiss(t0), RingEvent.Tick(t0), RingEvent.Restore(t0),
        ).forEach { e ->
            val t = RingState.Idle.on(e)
            assertEquals(RingState.Idle, t.state)
            assertTrue(t.effects.isEmpty())
        }
    }

    @Test
    fun fullCycleFireSnoozeFireDismiss() {
        var s: RingState = RingState.Idle
        s = s.on(RingEvent.Fire(7, t0, t0)).state
        val snooze = s.on(RingEvent.Snooze(t0 + min, 5, 0, 1))
        s = snooze.state
        val at = (snooze.effects[1] as RingEffect.ScheduleSnooze).at
        s = s.on(RingEvent.Fire(7, at, at)).state
        // Limit reached: snooze is refused, only dismiss ends it.
        assertEquals(s, s.on(RingEvent.Snooze(at + min, 5, 1, 1)).state)
        assertEquals(RingState.Idle, s.on(RingEvent.Dismiss(at + 2 * min)).state)
    }
}
