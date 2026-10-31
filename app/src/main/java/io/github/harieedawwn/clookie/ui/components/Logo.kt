package io.github.harieedawwn.clookie.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.design.Clookie
import kotlin.math.cos
import kotlin.math.sin

/**
 * The Clookie mark ("bitten clock"): a cookie whose bite forms the C, with two chocolate-chip
 * hands at 10:10. Geometry lives in the 108-unit adaptive-icon space so the launcher icon
 * (res/drawable/ic_launcher_*.xml) and the in-app mark stay identical.
 */
object MarkGeometry {
    const val CX = 54f
    const val CY = 54f
    const val R = 30f
    const val BITE_R = 13f
    private const val BITE_D = 31f
    private const val BITE_ANGLE = 12.0 // degrees below 3 o'clock
    val biteX = CX + BITE_D * cos(Math.toRadians(BITE_ANGLE)).toFloat()
    val biteY = CY + BITE_D * sin(Math.toRadians(BITE_ANGLE)).toFloat()
    val hourTip = Offset(41.88f, 47f)
    val minuteTip = Offset(68.72f, 45.5f)
    const val HAND_W = 7f
    const val PIN_R = 5f
    val chips = listOf(Triple(40.5f, 63f, 3.6f), Triple(53f, 74.5f, 2.9f), Triple(64.5f, 68.5f, 2.6f))

    /** Visible square of the mark inside the 108 space (cookie plus a small margin). */
    const val CROP_START = 22f
    const val CROP_SIZE = 64f
}

/** Draws the mark into a square of [side] px at [topLeft]; [cookiePath] is reused. */
fun DrawScope.drawClookieMark(cookie: Color, chips: Color, topLeft: Offset, side: Float, cookiePath: Path) {
    val g = MarkGeometry
    val k = side / g.CROP_SIZE
    fun p(x: Float, y: Float) = Offset(topLeft.x + (x - g.CROP_START) * k, topLeft.y + (y - g.CROP_START) * k)
    val body = Path().apply { addOval(Rect(p(g.CX, g.CY), g.R * k)) }
    val bite = Path().apply { addOval(Rect(p(g.biteX, g.biteY), g.BITE_R * k)) }
    cookiePath.rewind()
    cookiePath.op(body, bite, PathOperation.Difference)
    drawPath(cookiePath, cookie)
    val center = p(g.CX, g.CY)
    drawLine(chips, center, p(g.hourTip.x, g.hourTip.y), g.HAND_W * k, StrokeCap.Round)
    drawLine(chips, center, p(g.minuteTip.x, g.minuteTip.y), g.HAND_W * k, StrokeCap.Round)
    drawCircle(chips, g.PIN_R * k, center)
    g.chips.forEach { (x, y, r) -> drawCircle(chips, r * k, p(x, y)) }
}

@Composable
fun ClookieMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    cookie: Color = Clookie.colors.accent,
    chips: Color = Clookie.colors.onAccent,
) {
    val path = remember { Path() }
    Canvas(modifier.size(size)) { drawClookieMark(cookie, chips, Offset.Zero, this.size.minDimension, path) }
}

/** Mark + "Clookie" wordmark in the display face. */
@Composable
fun ClookieLockup(modifier: Modifier = Modifier, markSize: Dp = 32.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        ClookieMark(size = markSize)
        Spacer(Modifier.width(10.dp))
        Txt("Clookie", Clookie.type.displaySmall)
    }
}
