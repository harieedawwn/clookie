package io.github.harieedawwn.clookie.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.harieedawwn.clookie.R
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.ClookieColors
import io.github.harieedawwn.clookie.design.rememberHaptics
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class Ring { Minute, Hour }

/**
 * The Cookie Dial: an outer dough ring for minutes (dragging past 12 o'clock carries the
 * hour) and an inner ring for the 24 hours, around a cookie whose chocolate hands show the
 * time. One light haptic per minute, a stronger one every 5 minutes and every hour.
 * Everything is drawn in the draw phase; recomposition happens only when a value changes.
 */
@Composable
fun CookieDial(
    hour: Int,
    minute: Int,
    onChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Clookie.colors
    val hardShadow = Clookie.style.hardShadow
    val haptics = rememberHaptics()
    val measurer = rememberTextMeasurer(cacheSize = 8)
    val type = Clookie.type
    val thumbStyle = type.bodyStrong.copy(fontSize = 15.sp, lineHeight = 15.sp)
    val markStyle = type.caption.copy(fontSize = 11.sp, lineHeight = 11.sp, color = c.inkMuted)
    val hourLabel = remember(hour, thumbStyle, c) { measurer.measure("%02d".format(hour), thumbStyle.copy(color = c.background)) }
    val minuteLabel = remember(minute, thumbStyle, c) { measurer.measure("%02d".format(minute), thumbStyle.copy(color = c.onAccent)) }
    val marks = remember(markStyle) { listOf("0", "6", "12", "18").map { measurer.measure(it, markStyle) } }
    val currentHour by rememberUpdatedState(hour)
    val currentMinute by rememberUpdatedState(minute)
    val change by rememberUpdatedState(onChange)
    val description = stringResource(R.string.cd_dial)

    Canvas(
        modifier
            .widthIn(max = 340.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val geo = DialGeometry(size.width.toFloat(), density)
                    val dist = (down.position - center).getDistance()
                    val ring = when {
                        dist >= geo.outerInner - 12.dp.toPx() -> Ring.Minute
                        dist >= geo.innerInner - 12.dp.toPx() -> Ring.Hour
                        else -> null
                    } ?: return@awaitEachGesture
                    down.consume()
                    var dragging = false
                    fun apply(p: Offset) {
                        val a = angleOf(p - center)
                        if (ring == Ring.Minute) {
                            val m = (a / 6f).roundToInt() % 60
                            val old = currentMinute
                            if (m == old) return
                            var h = currentHour
                            if (dragging && old >= 45 && m < 15) h = (h + 1) % 24
                            if (dragging && old < 15 && m >= 45) h = (h + 23) % 24
                            if (m % 5 == 0) haptics.strongTick() else haptics.tick()
                            change(h, m)
                        } else {
                            val h = (a / 15f).roundToInt() % 24
                            if (h == currentHour) return
                            haptics.strongTick()
                            change(h, currentMinute)
                        }
                    }
                    apply(down.position)
                    dragging = true
                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) break
                        apply(pointer.position)
                        pointer.consume()
                    }
                }
            },
    ) {
        val geo = DialGeometry(size.width, density)
        drawDial(geo, c, hour, minute, hardShadow, hourLabel, minuteLabel, marks)
    }
}

/** Ring radii derived from the dial size. */
private class DialGeometry(widthPx: Float, dp: Float) {
    val r = widthPx / 2
    val outerOuter = r - 2 * dp
    val outerInner = r - 50 * dp
    val outerMid = (outerOuter + outerInner) / 2
    val innerOuter = outerInner
    val innerInner = innerOuter - 42 * dp
    val innerMid = (innerOuter + innerInner) / 2
    val cookie = innerInner
}

private fun angleOf(v: Offset): Float {
    val deg = Math.toDegrees(atan2(v.x.toDouble(), -v.y.toDouble())).toFloat()
    return if (deg < 0) deg + 360f else deg
}

private fun polar(center: Offset, radius: Float, degrees: Float): Offset {
    val rad = degrees * PI.toFloat() / 180f
    return Offset(center.x + radius * sin(rad), center.y - radius * cos(rad))
}

private fun DrawScope.drawDial(
    g: DialGeometry,
    c: ClookieColors,
    hour: Int,
    minute: Int,
    hardShadow: Boolean,
    hourLabel: TextLayoutResult,
    minuteLabel: TextLayoutResult,
    marks: List<TextLayoutResult>,
) {
    val center = this.center
    val stroke = 2.dp.toPx()
    val shadowOffset = Offset(4.dp.toPx(), 4.dp.toPx())

    // Outer dough ring with chips every 5 minutes, inner hour ring sharing its edge: three lines total.
    drawCircle(c.slab, g.outerMid, center, style = DrawStroke(g.outerOuter - g.outerInner))
    drawCircle(c.surface, g.innerMid, center, style = DrawStroke(g.innerOuter - g.innerInner))
    drawCircle(c.outline, g.outerOuter, center, style = DrawStroke(stroke))
    drawCircle(c.outline, g.outerInner, center, style = DrawStroke(stroke * 0.75f))
    for (i in 0 until 60) {
        val p = polar(center, g.outerMid, i * 6f)
        if (i % 5 == 0) drawCircle(c.chip, 3.2.dp.toPx(), p) else drawCircle(c.inkMuted, 1.2.dp.toPx(), p)
    }
    for (i in 0 until 24) {
        if (i % 6 == 0) {
            val label = marks[i / 6]
            val p = polar(center, g.innerMid, i * 15f)
            drawText(label, topLeft = Offset(p.x - label.size.width / 2f, p.y - label.size.height / 2f))
        } else {
            drawCircle(c.inkMuted, 1.2.dp.toPx(), polar(center, g.innerMid, i * 15f))
        }
    }

    // Center cookie with chocolate hands.
    drawCircle(c.accentSoft, g.cookie, center)
    drawCircle(c.outline, g.cookie, center, style = DrawStroke(stroke))
    drawCircle(c.chip, g.cookie * 0.07f, polar(center, g.cookie * 0.62f, 200f))
    drawCircle(c.chip, g.cookie * 0.06f, polar(center, g.cookie * 0.55f, 130f))
    drawCircle(c.chip, g.cookie * 0.05f, polar(center, g.cookie * 0.7f, 250f))
    val handWidth = 6.dp.toPx()
    drawLine(c.chip, center, polar(center, g.cookie * 0.48f, ((hour % 12) + minute / 60f) * 30f), handWidth, StrokeCap.Round)
    drawLine(c.chip, center, polar(center, g.cookie * 0.74f, minute * 6f), handWidth * 0.8f, StrokeCap.Round)
    drawCircle(c.ink, 4.dp.toPx(), center)

    // Hour thumb (ink) and minute thumb (accent).
    val hp = polar(center, g.innerMid, hour * 15f)
    val hr = 19.dp.toPx()
    drawCircle(c.ink, hr, hp)
    drawText(hourLabel, topLeft = Offset(hp.x - hourLabel.size.width / 2f, hp.y - hourLabel.size.height / 2f))

    val mp = polar(center, g.outerMid, minute * 6f)
    val mr = 22.dp.toPx()
    if (hardShadow) drawCircle(c.shadow, mr, mp + shadowOffset)
    drawCircle(c.accent, mr, mp)
    drawCircle(c.outline, mr, mp, style = DrawStroke(stroke))
    drawText(minuteLabel, topLeft = Offset(mp.x - minuteLabel.size.width / 2f, mp.y - minuteLabel.size.height / 2f))
}
