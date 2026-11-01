package io.github.harieedawwn.clookie.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.mission.MissionRegistry
import io.github.harieedawwn.clookie.mission.MissionSession
import io.github.harieedawwn.clookie.model.Alarm
import io.github.harieedawwn.clookie.model.ChainProgress
import io.github.harieedawwn.clookie.model.ChainRules
import io.github.harieedawwn.clookie.model.canSnooze
import io.github.harieedawwn.clookie.model.complete
import io.github.harieedawwn.clookie.model.stop
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.ui.LocalNow
import io.github.harieedawwn.clookie.ui.components.RingingLayout
import io.github.harieedawwn.clookie.ui.components.RingingPill
import io.github.harieedawwn.clookie.ui.components.SlideToStop
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.biteAway
import java.time.LocalDate

/**
 * Ringing screen and mission host: runs the alarm's mission chain one by one in the
 * mission slot, then plays the dismissal bite. [preview] is the editor's "Thử" (shows the
 * alarm time instead of the clock). Snooze shows while snoozes are left; with "snooze after
 * mission" only once a mission is solved, and a solved chain then offers stop or snooze.
 * [chainKey] identifies a real ring so its progress survives leaving the screen.
 */
@Composable
fun RingingScreen(
    alarm: Alarm,
    date: LocalDate,
    onDismissed: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier,
    preview: Boolean = true,
    onInteract: () -> Unit = {},
    chainKey: String? = null,
) {
    val bite = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }
    val rules = ChainRules.of(alarm)
    // A real ring keeps its progress per ring in the process, so leaving and returning to the
    // ringing screen does not restart the chain.
    var progress by rememberSaveable(chainKey, stateSaver = ChainProgressSaver) {
        mutableStateOf(chainKey?.let { ChainMemory[it] } ?: ChainProgress())
    }
    LaunchedEffect(progress) { chainKey?.let { ChainMemory[it] = progress } }
    val reduced = Clookie.motion.reduced
    val dismissed by rememberUpdatedState(onDismissed)
    LaunchedEffect(progress.finished) { if (progress.finished) dismissing = true }
    LaunchedEffect(dismissing) {
        if (!dismissing) return@LaunchedEffect
        if (reduced) bite.snapTo(1f) else bite.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        dismissed()
    }
    val weekdays = stringArrayResource(R.array.weekday_full)
    val months = stringArrayResource(R.array.month_names)
    val dateText = stringResource(R.string.date_format, weekdays[date.dayOfWeek.value - 1], date.dayOfMonth, date.monthValue, months[date.monthValue - 1])
    val now = LocalNow.current
    // Derived, so the screen (and the mission in it, camera included) recomposes once a minute,
    // not on every tick.
    val nowMinute by remember(now) { derivedStateOf { now.value.hour * 60 + now.value.minute } }
    val hour = if (preview) alarm.hour else nowMinute / 60
    val minute = if (preview) alarm.minute else nowMinute % 60
    val chain = alarm.missions
    val c = Clookie.colors

    RingingLayout(
        hour = hour,
        minute = minute,
        dateText = dateText,
        label = alarm.label,
        modifier = modifier.biteAway({ bite.value }),
        snooze = if (progress.canSnooze(rules)) {
            { RingingPill(stringResource(R.string.snooze_action, alarm.snoozeMinutes), onSnooze) }
        } else null,
    ) {
        if (progress.choosing) {
            // Snooze after mission: the chain is solved, stop or snooze.
            Txt(stringResource(R.string.missions_done).uppercase(), Clookie.type.eyebrow, color = c.onRinging)
            Spacer(Modifier.height(Space.s3))
            SlideToStop(stringResource(R.string.slide_to_stop), onDone = { progress = progress.stop() }, onDrag = onInteract)
            return@RingingLayout
        }
        val step = progress.step.coerceIn(0, chain.lastIndex)
        if (chain.size > 1) {
            Txt(stringResource(R.string.mission_step, step + 1, chain.size), Clookie.type.eyebrow, color = c.onRinging)
            Spacer(Modifier.height(Space.s3))
        }
        val (mission, spec) = MissionRegistry.resolve(chain[step])
        val session = remember(step, spec, preview) {
            HostSession(spec, preview, onInteract) { progress = progress.complete(rules) }
        }
        key(step) { mission.Content(session, Modifier.fillMaxWidth()) }
    }
}

/** Chain progress of the rings this process has shown, keyed by ring (alarm id + trigger time). */
private val ChainMemory = mutableMapOf<String, ChainProgress>()

private val ChainProgressSaver = listSaver<ChainProgress, Int>(
    save = { listOf(it.step, it.solved, if (it.choosing) 1 else 0, if (it.finished) 1 else 0) },
    restore = { ChainProgress(it[0], it[1], it[2] == 1, it[3] == 1) },
)

private class HostSession(
    override val spec: MissionSpec,
    override val preview: Boolean,
    private val onInteract: () -> Unit,
    private val onComplete: () -> Unit,
) : MissionSession {
    private var done = false

    override fun interact() = onInteract()

    override fun complete() {
        if (done) return
        done = true
        onComplete()
    }
}
