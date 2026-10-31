package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mission chain and snooze state machine (ringing host + ring machine). */
class ChainTest {
    private val three = listOf(MissionSpec(MissionType.CookieCode), MissionSpec(MissionType.Math), MissionSpec(MissionType.Crumbs))

    @Test
    fun chainOfThreeRunsInOrderThenDismisses() {
        val r = ChainRules(3, snoozesLeft = 2, snoozeAfterMission = false)
        var p = ChainProgress()
        assertTrue(p.canSnooze(r))
        p = p.complete(r)
        assertEquals(1, p.step)
        p = p.complete(r)
        assertEquals(2, p.step)
        assertFalse(p.finished)
        p = p.complete(r)
        assertTrue(p.finished)
        assertFalse(p.canSnooze(r))
        // Completing again (double callback) changes nothing.
        assertEquals(p, p.complete(r))
    }

    @Test
    fun snoozeAfterMissionWaitsForTheFirstSolvedMission() {
        val r = ChainRules(2, snoozesLeft = 1, snoozeAfterMission = true)
        var p = ChainProgress()
        assertFalse(p.canSnooze(r))
        p = p.complete(r)
        assertTrue(p.canSnooze(r))
        // Solving the whole chain offers stop or snooze instead of dismissing at once.
        p = p.complete(r)
        assertTrue(p.choosing)
        assertFalse(p.finished)
        assertTrue(p.canSnooze(r))
        p = p.stop()
        assertTrue(p.finished)
    }

    @Test
    fun singleMissionWithSnoozeAfterMissionOffersTheChoice() {
        val r = ChainRules(1, snoozesLeft = 3, snoozeAfterMission = true)
        val p = ChainProgress().complete(r)
        assertTrue(p.choosing)
        assertTrue(p.canSnooze(r))
    }

    @Test
    fun noSnoozesLeftDismissesWithoutAChoice() {
        val r = ChainRules(1, snoozesLeft = 0, snoozeAfterMission = true)
        var p = ChainProgress()
        assertFalse(p.canSnooze(r))
        p = p.complete(r)
        assertTrue(p.finished)
        assertFalse(p.choosing)
    }

    @Test
    fun rulesFollowTheAlarm() {
        val a = Alarm(id = 1, hour = 6, minute = 0, missions = three, snoozeLimit = 3, snoozeCount = 2, snoozeAfterMission = true)
        assertEquals(ChainRules(3, 1, true), ChainRules.of(a))
    }

    @Test
    fun snoozeRespectsTheLimitThenTheNextRingStartsTheChainAgain() {
        var state: RingState = RingState.Idle
        state = state.on(RingEvent.Fire(1, 1000, 1000)).state
        val snoozed = state.on(RingEvent.Snooze(2000, minutes = 9, count = 1, limit = 2))
        assertEquals(RingState.Idle, snoozed.state)
        assertTrue(snoozed.effects.contains(RingEffect.ScheduleSnooze(1, 2000 + 9 * 60_000L, 2)))
        // Limit reached: snoozing does nothing, the alarm keeps ringing.
        val ringing = RingState.Idle.on(RingEvent.Fire(1, 3000, 3000)).state
        assertEquals(ringing, ringing.on(RingEvent.Snooze(4000, 9, count = 2, limit = 2)).state)
        // A new ring (new trigger time) has fresh progress: ChainProgress() starts at step 0.
        assertEquals(0, ChainProgress().step)
    }

    @Test
    fun ownPhraseSurvivesStorage() {
        val chain = listOf(
            MissionSpec(MissionType.Typing, mapOf(MissionKeys.Count to 2), "Dậy đi; ~ 100% nhé"),
            MissionSpec(MissionType.Math, mapOf(MissionKeys.Level to 3)),
        )
        assertEquals(chain, MissionCodec.decode(MissionCodec.encode(chain)))
        // Older stored chains (no text) still decode.
        assertEquals(listOf(MissionSpec(MissionType.Math, mapOf("count" to 3))), MissionCodec.decode("Math{count=3}"))
    }

    @Test
    fun profilesApplyAndMatch() {
        val base = Alarm(id = 1, hour = 6, minute = 0)
        for (p in SleeperProfile.entries) assertEquals(p, SleeperProfiles.matching(SleeperProfiles.apply(base, p)))
        assertEquals(SleeperProfile.Medium, SleeperProfiles.matching(SleeperProfiles.apply(base, SleeperProfile.Medium, crumbs = true)))
        val deep = SleeperProfiles.apply(base, SleeperProfile.Deep)
        assertEquals(listOf(MissionType.CookieCode, MissionType.Math), deep.missionTypes)
        assertTrue(deep.volumeLock && deep.snoozeAfterMission && deep.awakeCheck && deep.rampSeconds == 0 && deep.volume == 100)
        // Editing a field afterwards turns it into a custom setup.
        assertEquals(null, SleeperProfiles.matching(deep.copy(volumeLock = false)))
    }
}
