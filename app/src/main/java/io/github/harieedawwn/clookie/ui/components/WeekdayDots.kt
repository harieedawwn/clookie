package io.github.harieedawwn.clookie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.rememberHaptics
import io.github.harieedawwn.clookie.model.Days

/**
 * Seven weekday dots, Monday first (T2 ... CN). Read-only on slabs; pass [onToggle] for the
 * editor's picker. [activeColor]/[onActiveColor] let slabs invert on dark fills.
 */
@Composable
fun WeekdayDots(
    days: Days,
    modifier: Modifier = Modifier,
    dotSize: Dp = 28.dp,
    onToggle: ((Int) -> Unit)? = null,
    activeColor: Color = Clookie.colors.ink,
    onActiveColor: Color = Clookie.colors.background,
    inactiveText: Color = Clookie.colors.inkMuted,
) {
    val labels = stringArrayResource(R.array.weekday_short)
    val haptics = rememberHaptics()
    val crisp = Clookie.shapes.smallRadius < 6.dp
    val shape = if (crisp) RoundedCornerShape(3.dp) else CircleShape
    val interactive = onToggle != null
    Row(modifier, horizontalArrangement = if (interactive) Arrangement.SpaceBetween else Arrangement.spacedBy(5.dp)) {
        labels.forEachIndexed { i, label ->
            val on = days.isSet(i)
            Box(
                Modifier
                    .size(dotSize)
                    .clip(shape)
                    .background(if (on) activeColor else Color.Transparent)
                    .then(if (interactive && !on) Modifier.border(Stroke.thin, Clookie.colors.outline, shape) else Modifier)
                    .then(
                        if (onToggle != null) {
                            Modifier.clickable(role = Role.Checkbox) { haptics.tick(); onToggle(i) }
                        } else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                val style = Clookie.type.caption.copy(fontSize = if (interactive) 13.sp else 10.sp, letterSpacing = 0.sp)
                Txt(label, style, color = if (on) onActiveColor else inactiveText, maxLines = 1)
            }
        }
    }
}
