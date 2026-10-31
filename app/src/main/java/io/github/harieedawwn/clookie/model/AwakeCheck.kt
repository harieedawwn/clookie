package io.github.harieedawwn.clookie.model

/**
 * Awake check (pure): [Alarm.awakeCheckMinutes] after a dismissal a notification asks
 * "Còn thức không?"; no answer within [AwakeRules.AnswerMs] and the alarm rings again with the
 * same missions. Persisted after every transition; times are epoch milliseconds.
 */
sealed interface AwakeState {
    data object Idle : AwakeState
    data class Waiting(val alarmId: Long, val askAt: Long) : AwakeState
    data class Asking(val alarmId: Long, val deadline: Long) : AwakeState
}

sealed interface AwakeEvent {
    data class Dismissed(val alarmId: Long, val check: Boolean, val minutes: Int, val now: Long) : AwakeEvent

    /** The registered time came (or a reboot asks to re-check). */
    data class Due(val now: Long) : AwakeEvent

    /** The user answered "Còn thức", or the alarm rang again / was snoozed. */
    data object Confirmed : AwakeEvent
}

sealed interface AwakeEffect {
    data class ScheduleAt(val at: Long) : AwakeEffect
    data object CancelSchedule : AwakeEffect
    data class Ask(val until: Long) : AwakeEffect
    data object ClearAsk : AwakeEffect
    data class Ring(val alarmId: Long) : AwakeEffect
}

object AwakeRules {
    const val AnswerMs = 90_000L
    const val DefaultMinutes = 4
    val minutes = listOf(2, 4, 6, 10)

    /** Checks this overdue (phone was off) are dropped instead of ringing out of the blue. */
    const val StaleMs = 30 * 60_000L
}

fun AwakeState.on(event: AwakeEvent): Pair<AwakeState, List<AwakeEffect>> = when (event) {
    is AwakeEvent.Dismissed ->
        if (!event.check) this to emptyList()
        else {
            val at = event.now + event.minutes.coerceAtLeast(1) * 60_000L
            AwakeState.Waiting(event.alarmId, at) to listOf(AwakeEffect.ClearAsk, AwakeEffect.ScheduleAt(at))
        }
    is AwakeEvent.Due -> when (this) {
        AwakeState.Idle -> this to emptyList()
        is AwakeState.Waiting -> when {
            event.now < askAt -> this to listOf(AwakeEffect.ScheduleAt(askAt))
            event.now - askAt > AwakeRules.StaleMs -> AwakeState.Idle to listOf(AwakeEffect.CancelSchedule)
            else -> {
                val deadline = event.now + AwakeRules.AnswerMs
                AwakeState.Asking(alarmId, deadline) to listOf(AwakeEffect.Ask(deadline), AwakeEffect.ScheduleAt(deadline))
            }
        }
        is AwakeState.Asking -> when {
            event.now < deadline -> this to listOf(AwakeEffect.ScheduleAt(deadline))
            event.now - deadline > AwakeRules.StaleMs -> AwakeState.Idle to listOf(AwakeEffect.ClearAsk, AwakeEffect.CancelSchedule)
            else -> AwakeState.Idle to listOf(AwakeEffect.ClearAsk, AwakeEffect.Ring(alarmId))
        }
    }
    AwakeEvent.Confirmed ->
        if (this == AwakeState.Idle) this to emptyList()
        else AwakeState.Idle to listOf(AwakeEffect.ClearAsk, AwakeEffect.CancelSchedule)
}
