package io.github.harieedawwn.clookie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.DisplayFamily
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.rememberHaptics

/** True when [digit] may follow [typed] in an HHMM 24-hour entry. Pure, unit-tested. */
fun keypadAccepts(typed: String, digit: Int): Boolean = when (typed.length) {
    0 -> digit <= 2
    1 -> typed[0] != '2' || digit <= 3
    2 -> digit <= 5
    3 -> true
    else -> false
}

/** Type a time on a keypad: four slots fill left to right; impossible digits are disabled. */
@Composable
fun TimeKeypad(
    initialHour: Int,
    initialMinute: Int,
    onDone: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Clookie.colors
    val haptics = rememberHaptics()
    var typed by rememberSaveable { mutableStateOf("") }
    val placeholder = "%02d%02d".format(initialHour, initialMinute)

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
            for (i in 0 until 4) {
                if (i == 2) Txt(":", Clookie.type.displayTitle.copy(fontSize = 40.sp, lineHeight = 48.sp))
                val filled = i < typed.length
                val active = i == typed.length
                Box(
                    Modifier
                        .size(width = 56.dp, height = 72.dp)
                        .border(if (active) 3.dp else Stroke.thin, if (active) c.accent else c.outline, Clookie.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Txt(
                        (if (filled) typed[i] else placeholder[i]).toString(),
                        Clookie.type.displayTitle.copy(fontFamily = DisplayFamily, fontSize = 40.sp, lineHeight = 48.sp),
                        color = if (filled) c.ink else c.inkMuted.copy(alpha = 0.45f),
                    )
                }
            }
        }
        Box(Modifier.height(Space.s5))
        val rows = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9), listOf(-1, 0, -2))
        Column(verticalArrangement = Arrangement.spacedBy(Space.s2)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.s2)) {
                    row.forEach { key ->
                        when (key) {
                            -1 -> KeypadKey(Modifier.weight(1f), enabled = typed.isNotEmpty(), bg = Color.Transparent,
                                cd = stringResource(R.string.cd_backspace), onClick = { typed = typed.dropLast(1) }) {
                                ClookieIcon(ClookieIcons.Backspace, null, tint = c.ink)
                            }
                            -2 -> KeypadKey(Modifier.weight(1f), enabled = typed.length == 4, bg = c.accent,
                                cd = stringResource(R.string.action_done), onClick = {
                                    haptics.confirm()
                                    onDone(typed.take(2).toInt(), typed.drop(2).toInt())
                                }) {
                                ClookieIcon(ClookieIcons.Check, null, tint = c.onAccent)
                            }
                            else -> KeypadKey(Modifier.weight(1f), enabled = keypadAccepts(typed, key), bg = c.surface,
                                cd = key.toString(), onClick = { haptics.tick(); typed += key }) {
                                Txt(key.toString(), Clookie.type.title.copy(fontSize = 26.sp, lineHeight = 34.sp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(
    modifier: Modifier,
    enabled: Boolean,
    bg: Color,
    cd: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = Clookie.shapes.medium
    Box(
        modifier
            .height(60.dp)
            .width(96.dp)
            .pressable(shape, onClick, shadow = false, enabled = enabled)
            .background(bg)
            .border(Stroke.thin, Clookie.colors.outline, shape)
            .graphicsLayer { alpha = if (enabled) 1f else 0.3f }
            .semantics { contentDescription = cd },
        contentAlignment = Alignment.Center,
    ) { content() }
}
