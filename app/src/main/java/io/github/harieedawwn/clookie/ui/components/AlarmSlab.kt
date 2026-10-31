package io.github.harieedawwn.clookie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.TimeSize
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.model.Alarm

/**
 * An alarm as a tall "slab": oversized time, label and missions, weekday dots, bite toggle.
 * Enabled slabs are solid biscuit blocks with a hard shadow; disabled ones are outlined and muted.
 */
@Composable
fun AlarmSlab(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Clookie.colors
    val shape = Clookie.shapes.large
    val on = alarm.enabled
    val ink = if (on) c.ink else c.inkMuted
    Column(
        modifier
            .fillMaxWidth()
            .pressable(shape, onClick, shadow = on)
            .background(if (on) c.slab else c.surface)
            .border(if (on) Stroke.regular else Stroke.thin, if (on) c.outline else c.inkMuted, shape)
            .padding(start = Space.s5, end = Space.s4, top = Space.s4, bottom = Space.s5),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeDisplay(alarm.hour, alarm.minute, TimeSize.Slab.dp, Modifier.weight(1f, fill = false), color = ink)
            Spacer(Modifier.weight(1f))
            BiteToggle(on, onToggle, contentDescription = stringResource(R.string.cd_alarm_toggle))
        }
        Spacer(Modifier.height(Space.s1))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
            if (alarm.label.isNotEmpty()) {
                Txt(alarm.label, Clookie.type.heading, color = ink, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            }
            alarm.missionTypes.forEach { m ->
                ClookieIcon(m.icon, stringResource(m.nameRes), tint = ink, size = 18.dp)
            }
        }
        Spacer(Modifier.height(Space.s3))
        if (alarm.days.isOneTime) {
            Txt(stringResource(R.string.repeat_once), Clookie.type.label, color = c.inkMuted)
        } else {
            WeekdayDots(
                alarm.days,
                dotSize = 26.dp,
                activeColor = ink,
                onActiveColor = if (on) c.slab else c.surface,
                inactiveText = c.inkMuted,
            )
        }
    }
}
