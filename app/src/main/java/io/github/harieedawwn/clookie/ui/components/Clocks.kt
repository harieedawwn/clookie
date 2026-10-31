package io.github.harieedawwn.clookie.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.harieedawwn.clookie.design.ClockFont
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.UiFamily
import io.github.harieedawwn.clookie.model.WordClock
import io.github.harieedawwn.clookie.ui.LocalNow
import kotlin.math.cos
import kotlin.math.sin

/** Chip sizes (x radius) for the 12 hour marks: irregular on purpose, 12 o'clock the largest. */
private val ChipSizes = floatArrayOf(0.085f, 0.058f, 0.064f, 0.075f, 0.056f, 0.066f, 0.078f, 0.06f, 0.07f, 0.074f, 0.057f, 0.067f)

/**
 * Analog "cookie" clock: a bitten cookie with chocolate chips as hour marks and chip-colored
 * hands. The wall time is read in the draw phase only, so ticking never recomposes.
 */
@Composable
fun CookieClock(modifier: Modifier = Modifier, size: Dp = 232.dp) {
    val now = LocalNow.current
    val c = Clookie.colors
    val words by remember { derivedStateOf { WordClock.vi(now.value.hour, now.value.minute) } }
    val body = remember { Path() }
    val bite = remember { Path() }
    val cookie = remember { Path() }
    val stroke = with(androidx.compose.ui.platform.LocalDensity.current) { Stroke.regular.toPx() }
    Canvas(modifier.size(size).semantics { contentDescription = words }) {
        val r = this.size.minDimension / 2f - stroke
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        // Bite near 1:30, outside the chip ring.
        val biteCenter = center + Offset(sin(Math.toRadians(45.0)).toFloat(), -cos(Math.toRadians(45.0)).toFloat()) * (r * 1.02f)
        val biteR = r * 0.25f
        body.rewind(); body.addOval(Rect(center, r))
        bite.rewind(); bite.addOval(Rect(biteCenter, biteR))
        cookie.rewind(); cookie.op(body, bite, PathOperation.Difference)
        drawPath(cookie, c.accentSoft)
        drawPath(cookie, c.outline, style = DrawStroke(stroke))

        for (h in 0 until 12) {
            val a = Math.toRadians(h * 30.0)
            val p = center + Offset(sin(a).toFloat(), -cos(a).toFloat()) * (r * 0.79f)
            val cr = r * ChipSizes[h]
            if ((p - biteCenter).getDistance() < biteR + cr * 1.6f) continue
            // Slightly squashed, tilted dots read as chips rather than plain markers.
            rotate(h * 47f, p) { drawOval(c.chip, p - Offset(cr * 1.15f, cr * 0.9f), Size(cr * 2.3f, cr * 1.8f)) }
        }

        val t = now.value
        val minuteAngle = t.minute * 6f + t.second * 0.1f
        val hourAngle = (t.hour % 12) * 30f + t.minute * 0.5f
        rotate(hourAngle, center) {
            drawLine(c.ink, center, center - Offset(0f, r * 0.46f), r * 0.1f, StrokeCap.Round)
        }
        rotate(minuteAngle, center) {
            drawLine(c.ink, center, center - Offset(0f, r * 0.66f), r * 0.065f, StrokeCap.Round)
        }
        drawCircle(c.ink, r * 0.095f, center)
        drawCircle(c.accent, r * 0.045f, center)
    }
}

/**
 * Vietnamese word clock ("hai mươi ba giờ kém mười lăm"): the clock words "giờ", "kém",
 * "rưỡi", "đúng" in the accent. Recomposes once a minute.
 */
@Composable
fun WordClockText(modifier: Modifier = Modifier) {
    val now = LocalNow.current
    val c = Clookie.colors
    val minuteOfDay by remember { derivedStateOf { now.value.hour * 60 + now.value.minute } }
    val text = remember(minuteOfDay, c) {
        val words = WordClock.vi(minuteOfDay / 60, minuteOfDay % 60).split(' ')
        buildAnnotatedString {
            words.forEachIndexed { i, w ->
                if (i > 0) append(' ')
                if (w in ClockWords) withStyle(SpanStyle(color = c.accent)) { append(w) } else append(w)
            }
        }
    }
    val style = Clookie.type.displayTitle.let { base ->
        if (Clookie.style.clockFont == ClockFont.Ui) base.copy(fontFamily = UiFamily, fontWeight = FontWeight.Bold) else base
    }.copy(fontSize = 46.sp, lineHeight = 64.sp, color = c.ink)
    BasicText(text, modifier.semantics { contentDescription = text.text }, style = style)
}

private val ClockWords = setOf("giờ", "kém", "rưỡi", "đúng")

/** Current wall time in display numerals; recomposes once a minute. */
@Composable
fun NowNumerals(size: Dp, modifier: Modifier = Modifier) {
    val now = LocalNow.current
    val hm by remember { derivedStateOf { now.value.hour to now.value.minute } }
    TimeDisplay(hm.first, hm.second, size, modifier)
}
