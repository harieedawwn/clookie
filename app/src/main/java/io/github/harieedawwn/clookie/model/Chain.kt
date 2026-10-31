package io.github.harieedawwn.clookie.model

/**
 * Progress through an alarm's mission chain (pure). [solved] missions are done; [choosing]
 * means the whole chain is solved and "snooze after mission" offers stop or snooze;
 * [finished] means the alarm is dismissed.
 */
data class ChainProgress(
    val step: Int = 0,
    val solved: Int = 0,
    val choosing: Boolean = false,
    val finished: Boolean = false,
)

/** What the chain needs to know about the alarm: its length and the snooze rules. */
data class ChainRules(val size: Int, val snoozesLeft: Int, val snoozeAfterMission: Boolean) {
    companion object {
        fun of(alarm: Alarm) = ChainRules(alarm.missions.size.coerceIn(1, Alarm.MaxMissions), alarm.snoozeLimit - alarm.snoozeCount, alarm.snoozeAfterMission)
    }
}

/** Snooze shows while snoozes are left; with "snooze after mission" only once a mission is solved. */
fun ChainProgress.canSnooze(r: ChainRules): Boolean =
    !finished && r.snoozesLeft > 0 && (!r.snoozeAfterMission || solved > 0)

/** The current mission was solved: next mission, the stop-or-snooze choice, or dismissal. */
fun ChainProgress.complete(r: ChainRules): ChainProgress = when {
    finished || choosing -> this
    step + 1 < r.size -> copy(step = step + 1, solved = solved + 1)
    r.snoozeAfterMission && r.snoozesLeft > 0 -> copy(solved = solved + 1, choosing = true)
    else -> copy(solved = solved + 1, finished = true)
}

/** "Tắt" on the stop-or-snooze choice. */
fun ChainProgress.stop(): ChainProgress = if (choosing) copy(choosing = false, finished = true) else this
