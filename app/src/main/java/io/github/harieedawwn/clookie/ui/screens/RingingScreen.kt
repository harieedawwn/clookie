package io.github.harieedawwn.clookie.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import io.github.harieedawwn.clookie.model.MissionSpec
import io.github.harieedawwn.clookie.ui.LocalNow
import io.github.harieedawwn.clookie.ui.components.RingingLayout
import io.github.harieedawwn.clookie.ui.components.RingingPill
import io.github.harieedawwn.clookie.ui.components.Txt
import io.github.harieedawwn.clookie.ui.components.biteAway
import java.time.LocalDate

/**
 * Ringing screen and mission host: runs the alarm's mission chain one by one in the
 * mission slot, then plays the dismissal bite. [preview] is the editor's "Thử" (shows the
 * alarm time instead of the clock). Snooze shows while snoozes are left.
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
) {
    val bite = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    val reduced = Clookie.motion.reduced
    val dismissed by rememberUpdatedState(onDismissed)
    LaunchedEffect(dismissing) {
        if (!dismissing) return@LaunchedEffect
        if (reduced) bite.snapTo(1f) else bite.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        dismissed()
    }
    val weekdays = stringArrayResource(R.array.weekday_full)
    val months = stringArrayResource(R.array.month_names)
    val dateText = stringResource(R.string.date_format, weekdays[date.dayOfWeek.value - 1], date.dayOfMonth, date.monthValue, months[date.monthValue - 1])
    val now = LocalNow.current
    val hour = if (preview) alarm.hour else now.value.hour
    val minute = if (preview) alarm.minute else now.value.minute
    val chain = alarm.missions
    val snoozesLeft = alarm.snoozeLimit - alarm.snoozeCount
    val canSnooze = snoozesLeft > 0 && (!alarm.snoozeAfterMission || step > 0)
    val c = Clookie.colors

    RingingLayout(
        hour = hour,
        minute = minute,
        dateText = dateText,
        label = alarm.label,
        modifier = modifier.biteAway({ bite.value }),
        snooze = if (canSnooze) {
            { RingingPill(stringResource(R.string.snooze_action, alarm.snoozeMinutes), onSnooze) }
        } else null,
    ) {
        if (chain.size > 1) {
            Txt(
                stringResource(R.string.mission_step, (step + 1).coerceAtMost(chain.size), chain.size),
                Clookie.type.eyebrow, color = c.onRinging,
            )
            Spacer(Modifier.height(Space.s3))
        }
        val (mission, spec) = MissionRegistry.resolve(chain[step.coerceIn(0, chain.lastIndex)])
        val session = remember(step, spec, preview) {
            HostSession(spec, preview, onInteract) {
                if (step + 1 < chain.size) step++ else dismissing = true
            }
        }
        key(step) { mission.Content(session, Modifier.fillMaxWidth()) }
    }
}

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
