package io.github.harieedawwn.clookie.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.DisplayFamily

/** Plain text in Clookie styles (no Material Text, no LocalContentColor). */
@Composable
fun Txt(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Clookie.colors.ink,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Unspecified,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = color, textAlign = align),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

enum class TimeField { Hour, Minute }

/**
 * Huge display numerals in fixed-width digit slots, so nothing shifts while time ticks.
 * [size] is in dp and ignores font scale (the numerals are already huge). When the given
 * width is too small the numerals scale down to fit.
 */
@Composable
fun TimeDisplay(
    hour: Int,
    minute: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    color: Color = Clookie.colors.ink,
    hourColor: Color = color,
    minuteColor: Color = color,
    onFieldClick: ((TimeField) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer(cacheSize = 16)
    BoxWithConstraints(modifier.semantics(mergeDescendants = true) { contentDescription = "%02d:%02d".format(hour, minute) }) {
        val base = remember(size, density) { styleFor(with(density) { size.toSp() }) }
        val (slotPx, colonPx) = remember(base) {
            val digit = ('0'..'9').maxOf { measurer.measure(it.toString(), base).size.width }
            digit * 1.02f to measurer.measure(":", base).size.width * 0.9f
        }
        val totalPx = slotPx * 4 + colonPx
        val maxPx = with(density) { maxWidth.toPx() }
        val scale = if (constraints.hasBoundedWidth && totalPx > maxPx) maxPx / totalPx else 1f
        val style = if (scale < 1f) styleFor(with(density) { (size * scale).toSp() }) else base
        val slot = with(density) { (slotPx * scale).toDp() }
        val colon = with(density) { (colonPx * scale).toDp() }

        Row(verticalAlignment = Alignment.CenterVertically) {
            DigitGroup(hour, slot, style, hourColor, onFieldClick?.let { { it(TimeField.Hour) } })
            Box(Modifier.width(colon), contentAlignment = Alignment.Center) {
                BasicText(":", style = style.merge(color = color))
            }
            DigitGroup(minute, slot, style, minuteColor, onFieldClick?.let { { it(TimeField.Minute) } })
        }
    }
}

@Composable
private fun DigitGroup(value: Int, slot: Dp, style: TextStyle, color: Color, onClick: (() -> Unit)?) {
    val text = "%02d".format(value)
    Row(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier) {
        for (c in text) {
            Box(Modifier.width(slot), contentAlignment = Alignment.Center) {
                BasicText(c.toString(), style = style.merge(color = color), softWrap = false)
            }
        }
    }
}

private fun styleFor(fontSize: TextUnit) = TextStyle(
    fontFamily = DisplayFamily,
    fontSize = fontSize,
    lineHeight = 1.02.em,
    letterSpacing = 0.em,
    fontFeatureSettings = "tnum, lnum",
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)
