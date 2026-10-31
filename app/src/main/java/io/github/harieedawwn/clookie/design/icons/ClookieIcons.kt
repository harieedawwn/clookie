package io.github.harieedawwn.clookie.design.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.design.Clookie

/**
 * Clookie's own icon set: 24 grid, 2 px round-capped strokes, optional solid "chip" fills.
 * Default Material icons are not used on any main screen.
 */
object ClookieIcons {
    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r},${cy}a$r,$r 0 1,0 ${2 * r},0a$r,$r 0 1,0 ${-2 * r},0"

    private fun icon(name: String, stroke: String, fill: String? = null): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            if (stroke.isNotEmpty()) {
                addPath(
                    addPathNodes(stroke), stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
                )
            }
            if (fill != null) addPath(addPathNodes(fill), fill = SolidColor(Color.Black))
        }.build()

    val Alarm by lazy { icon("alarm", circle(12f, 13f, 7.5f) + "M12,9.5V13l2.5,2M4.5,5.5L7,3.5M19.5,5.5L17,3.5M7,19.5L5.5,21M17,19.5l1.5,1.5") }
    val Timer by lazy { icon("timer", circle(12f, 13.5f, 7.5f) + "M12,13.5V9.5M9.5,2.5h5M12,2.5V6") }
    val Stopwatch by lazy { icon("stopwatch", circle(12f, 13.5f, 7.5f) + "M12,13.5l3,-3M10,2.5h4M12,2.5V6M18.5,6.5l1.5,-1.5") }
    val Settings by lazy { icon("settings", "M4,7h9M17,7h3M4,17h4M12,17h8" + circle(15f, 7f, 2f) + circle(10f, 17f, 2f)) }
    val Plus by lazy { icon("plus", "M12,5v14M5,12h14") }
    val Close by lazy { icon("close", "M6,6l12,12M18,6L6,18") }
    val Check by lazy { icon("check", "M5,12.5l4.5,4.5L19,7.5") }
    val Back by lazy { icon("back", "M14.5,5L7.5,12l7,7") }
    val Chevron by lazy { icon("chevron", "M9.5,5l7,7-7,7") }
    val Snooze by lazy { icon("snooze", "M5,9h6l-6,8h6M14,4.5h5l-5,6h5") }
    val Bell by lazy { icon("bell", "M6,16.5V11a6,6 0 0,1 12,0v5.5l1.5,2h-15zM10,21.5h4") }
    val Sound by lazy { icon("sound", "M4,9.5h3.5L12,5.5v13l-4.5,-4H4zM15.5,9.5a3.5,3.5 0 0,1 0,5M18,7a7,7 0 0,1 0,10") }
    val Vibrate by lazy { icon("vibrate", "M9,4.5h6a1.5,1.5 0 0,1 1.5,1.5v12a1.5,1.5 0 0,1 -1.5,1.5h-6a1.5,1.5 0 0,1 -1.5,-1.5v-12a1.5,1.5 0 0,1 1.5,-1.5zM4,9v6M20,9v6") }
    val Repeat by lazy { icon("repeat", "M4.5,11V9.5a3,3 0 0,1 3,-3H19M16,3.5l3,3-3,3M19.5,13v1.5a3,3 0 0,1 -3,3H5M8,20.5l-3,-3 3,-3") }
    val Label by lazy { icon("label", "M4,4.5h7.5l8.5,8.5-7,7-8.5,-8.5z" + circle(8.5f, 9f, 1.2f)) }
    val Shield by lazy { icon("shield", "M12,3l7.5,3v5.5c0,4.6 -3.2,8.3 -7.5,9.8c-4.3,-1.5 -7.5,-5.2 -7.5,-9.8V6zM8.8,12.2l2.2,2.2 4.2,-4.2") }
    val Camera by lazy { icon("camera", "M4,7.5h3l1.5,-2.5h7L17,7.5h3a1,1 0 0,1 1,1V18a1,1 0 0,1 -1,1H4a1,1 0 0,1 -1,-1V8.5a1,1 0 0,1 1,-1z" + circle(12f, 13f, 3.5f)) }
    val Qr by lazy { icon("qr", "M4,4h6v6H4zM14,4h6v6h-6zM4,14h6v6H4zM14,14h2.5v2.5M20,14v0.01M14,20h6v-3.5") }
    val Math by lazy { icon("math", "M5,8h5M7.5,5.5v5M14,8h5M5.5,15.5l4,4M9.5,15.5l-4,4M14,16h5M14,19.5h5") }
    val Shake by lazy { icon("shake", "M9,4h6a1.5,1.5 0 0,1 1.5,1.5v13a1.5,1.5 0 0,1 -1.5,1.5H9a1.5,1.5 0 0,1 -1.5,-1.5v-13A1.5,1.5 0 0,1 9,4zM4,7.5l-1.5,2 1.5,2 -1.5,2 1.5,2M20,7.5l1.5,2 -1.5,2 1.5,2 -1.5,2") }
    val Steps by lazy { icon("steps", "M8,3.5c1.9,0 3,1.9 3,4.6 0,2.4 -1,4.4 -3,4.4s-3,-2 -3,-4.4C5,5.4 6.1,3.5 8,3.5zM6,16h4M16,8.5c1.9,0 3,1.9 3,4.6 0,2.4 -1,4.4 -3,4.4s-3,-2 -3,-4.4c0,-2.7 1.1,-4.6 3,-4.6zM14,21h4") }
    val Keyboard by lazy { icon("keyboard", "M4,6.5h16a1,1 0 0,1 1,1v9a1,1 0 0,1 -1,1H4a1,1 0 0,1 -1,-1v-9a1,1 0 0,1 1,-1zM7,10h0.01M10.5,10h0.01M14,10h0.01M17.5,10h0.01M8,14h8") }
    val Memory by lazy { icon("memory", "M4,4h7v7H4zM13,4h7v7h-7zM4,13h7v7H4zM13,13h7v7h-7z", "M14.5,14.5h4v4h-4z") }
    val Crumbs by lazy { icon("crumbs", circle(8f, 9f, 3.2f) + circle(16.5f, 7.5f, 1.8f) + circle(14.5f, 16f, 3.6f) + circle(6.5f, 17.5f, 1.6f)) }
    val Play by lazy { icon("play", "M8,5.5v13l10.5,-6.5z") }
    val Pause by lazy { icon("pause", "M8.5,5.5v13M15.5,5.5v13") }
    val Torch by lazy { icon("torch", "M8,3h8v4l-2,4v10h-4V11L8,7zM8,7h8M12,13.5v2") }
    val Globe by lazy { icon("globe", circle(12f, 12f, 8.5f) + "M3.5,12h17M12,3.5c2.6,2.4 3.8,5.2 3.8,8.5s-1.2,6.1 -3.8,8.5c-2.6,-2.4 -3.8,-5.2 -3.8,-8.5S9.4,5.9 12,3.5z") }
    val Trash by lazy { icon("trash", "M4.5,7h15M9.5,7V4.5h5V7M6.5,7l1,13h9l1,-13") }
    val Edit by lazy { icon("edit", "M4.5,19.5l1,-4L16,5l3,3L8.5,18.5zM14,7l3,3") }
    val Backspace by lazy { icon("backspace", "M9,6h11v12H9l-6,-6zM12.5,9.5l5,5M17.5,9.5l-5,5") }
    val Download by lazy { icon("download", "M12,4v11M7,10l5,5 5,-5M5,20h14") }
    val Lock by lazy { icon("lock", "M6.5,11h11a1,1 0 0,1 1,1v7a1,1 0 0,1 -1,1h-11a1,1 0 0,1 -1,-1v-7a1,1 0 0,1 1,-1zM8.5,11V8a3.5,3.5 0 0,1 7,0v3") }
    val Moon by lazy { icon("moon", "M19,14.5A7.5,7.5 0 0,1 9.5,5a7.5,7.5 0 1,0 9.5,9.5z") }
    val Ramp by lazy { icon("ramp", "M4,18.5h16V5.5z") }
    val Skip by lazy { icon("skip", "M5,6l7,6-7,6zM13,6l7,6-7,6z") }
    val Mission by lazy { icon("mission", "M5,20V4.5M5,5h12l-2.5,4 2.5,4H5") }
    val Battery by lazy { icon("battery", "M4,8h13a1,1 0 0,1 1,1v6a1,1 0 0,1 -1,1H4a1,1 0 0,1 -1,-1V9a1,1 0 0,1 1,-1zM21,10.5v3", "M5.5,10.5h6v3h-6z") }
    val Power by lazy { icon("power", "M12,3.5v8M7.5,6.5a7,7 0 1,0 9,0") }
    val Fullscreen by lazy { icon("fullscreen", "M4,9V4h5M15,4h5v5M20,15v5h-5M9,20H4v-5") }
    val Layers by lazy { icon("layers", "M12,4l8.5,4.5L12,13 3.5,8.5zM3.5,12.5L12,17l8.5,-4.5M3.5,16.5L12,21l8.5,-4.5") }

    /** Cookie with a bite and chips: themes, Crumbs, brand moments. */
    val Cookie by lazy {
        icon(
            "cookie", "M20.05,9.26A8.5,8.5 0 1,1 14.74,3.95A4,4 0 0,0 20.05,9.26z",
            circle(9f, 10f, 1.3f) + circle(13.5f, 15f, 1.3f) + circle(8f, 15.5f, 1f),
        )
    }

    /** Cookie Code marker: solid frame, pattern cells. */
    val CookieCode by lazy { icon("cookie_code", "M4,4h16v16H4z", "M7.5,7.5h3v3h-3zM13.5,13.5h3v3h-3zM13.5,7.5h3v3h-3z") }
}

@Composable
fun ClookieIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Clookie.colors.ink,
    size: Dp = 24.dp,
) {
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
