package io.github.harieedawwn.clookie.model

/**
 * Ring state machine (pure). The service persists [RingState] after every transition, so a
 * ringing alarm survives process death and reboot until it is dismissed, snoozed or times out.
 * Times are epoch milliseconds.
 */
sealed interface RingState {
    data object Idle : RingState

    /** [triggerAt] is the scheduled instant that fired; [startedAt] is when ringing began. */
    data class Ringing(val alarmId: Long, val triggerAt: Long, val startedAt: Long) : RingState
}

sealed interface RingEvent {
    data class Fire(val alarmId: Long, val triggerAt: Long, val now: Long) : RingEvent

    /** [count] snoozes already used; [limit] 0 means snooze is off. */
    data class Snooze(val now: Long, val minutes: Int, val count: Int, val limit: Int) : RingEvent
    data class Dismiss(val now: Long) : RingEvent

    /** Periodic check while ringing; ends an unattended ring after [RingRules.AutoSilenceMs]. */
    data class Tick(val now: Long) : RingEvent

    /** Process restarted or device rebooted with a persisted state. */
    data class Restore(val now: Long) : RingEvent
}

enum class RingOutcome { Dismissed, Missed, Replaced }

sealed interface RingEffect {
    data class StartRinging(val alarmId: Long) : RingEffect
    data object StopRinging : RingEffect
    data class ScheduleSnooze(val alarmId: Long, val at: Long, val count: Int) : RingEffect

    /** The ring for [alarmId] is over: clear snooze, disable one-time alarms, log it. */
    data class Finish(val alarmId: Long, val outcome: RingOutcome, val triggerAt: Long) : RingEffect
}

data class RingTransition(val state: RingState, val effects: List<RingEffect> = emptyList())

object RingRules {
    /** An unattended alarm rings this long, then stops and posts a missed-alarm notice. */
    const val AutoSilenceMs = 30 * 60_000L

    /** After reboot or a clock jump, an alarm this late still rings; later ones are reported missed. */
    const val LateGraceMs = 15 * 60_000L
}

fun RingState.on(event: RingEvent): RingTransition = when (event) {
    is RingEvent.Fire -> when (this) {
        RingState.Idle -> RingTransition(
            RingState.Ringing(event.alarmId, event.triggerAt, event.now),
            listOf(RingEffect.StartRinging(event.alarmId)),
        )
        is RingState.Ringing ->
            if (alarmId == event.alarmId && triggerAt == event.triggerAt) RingTransition(this)
            else RingTransition(
                RingState.Ringing(event.alarmId, event.triggerAt, event.now),
                listOf(RingEffect.Finish(alarmId, RingOutcome.Replaced, triggerAt), RingEffect.StartRinging(event.alarmId)),
            )
    }
    is RingEvent.Snooze -> when (this) {
        RingState.Idle -> RingTransition(this)
        is RingState.Ringing ->
            if (event.limit <= 0 || event.count >= event.limit || event.minutes <= 0) RingTransition(this)
            else RingTransition(
                RingState.Idle,
                listOf(RingEffect.StopRinging, RingEffect.ScheduleSnooze(alarmId, event.now + event.minutes * 60_000L, event.count + 1)),
            )
    }
    is RingEvent.Dismiss -> when (this) {
        RingState.Idle -> RingTransition(this)
        is RingState.Ringing -> RingTransition(
            RingState.Idle,
            listOf(RingEffect.StopRinging, RingEffect.Finish(alarmId, RingOutcome.Dismissed, triggerAt)),
        )
    }
    is RingEvent.Tick -> when (this) {
        RingState.Idle -> RingTransition(this)
        is RingState.Ringing ->
            if (event.now - startedAt < RingRules.AutoSilenceMs) RingTransition(this)
            else RingTransition(
                RingState.Idle,
                listOf(RingEffect.StopRinging, RingEffect.Finish(alarmId, RingOutcome.Missed, triggerAt)),
            )
    }
    is RingEvent.Restore -> when (this) {
        RingState.Idle -> RingTransition(this)
        is RingState.Ringing ->
            if (event.now - startedAt < RingRules.AutoSilenceMs) RingTransition(this, listOf(RingEffect.StartRinging(alarmId)))
            else RingTransition(RingState.Idle, listOf(RingEffect.Finish(alarmId, RingOutcome.Missed, triggerAt)))
    }
}

enum class LateFire { NotDue, RingNow, Missed }

/**
 * What to do with an alarm registered for [scheduledAt] that has not fired yet, checked at
 * [now] (after boot, a clock change or an app update).
 */
fun lateFire(scheduledAt: Long, now: Long): LateFire = when {
    scheduledAt > now -> LateFire.NotDue
    now - scheduledAt <= RingRules.LateGraceMs -> LateFire.RingNow
    else -> LateFire.Missed
}
