package io.github.harieedawwn.clookie.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.ui.screenBackground

/**
 * Square camera window: [content] (the camera) clipped to the large shape, corner brackets in
 * [ink], [required] progress dots filled up to [streak] and a "wrong code" pill when [wrong].
 * [fill] shows before the first frame. Square; [modifier] sets the size. Used on the ringing
 * color and on normal screens.
 */
@Composable
fun ScanWindow(
    streak: Int,
    required: Int,
    wrong: String?,
    ink: Color,
    fill: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = Clookie.shapes.large
    val c = Clookie.colors
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(fill)
            .border(Stroke.regular, ink, shape),
    ) {
        content()
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    val inset = 22.dp.toPx()
                    val arm = 30.dp.toPx()
                    val w = 4.dp.toPx()
                    val l = inset
                    val t = inset
                    val r = size.width - inset
                    val b = size.height - inset
                    for ((x, y, dx, dy) in listOf(
                        listOf(l, t, 1f, 1f), listOf(r, t, -1f, 1f), listOf(r, b, -1f, -1f), listOf(l, b, 1f, -1f),
                    )) {
                        drawLine(ink, Offset(x, y), Offset(x + dx * arm, y), w, StrokeCap.Round)
                        drawLine(ink, Offset(x, y), Offset(x, y + dy * arm), w, StrokeCap.Round)
                    }
                },
        )
        if (wrong != null) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Space.s4)
                    .clip(pillShape())
                    .background(c.accent)
                    .padding(horizontal = Space.s4, vertical = Space.s2),
            ) { Txt(wrong, Clookie.type.bodyStrong, color = c.onAccent, maxLines = 1) }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = Space.s4),
            horizontalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            for (i in 0 until required) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (i < streak) ink else Color.Transparent)
                        .border(Stroke.regular, ink, CircleShape),
                )
            }
        }
    }
}

/** Full-screen layer over everything (its own window), on the screen background. */
@Composable
fun ClookieOverlay(onDismiss: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Box(Modifier.fillMaxSize().screenBackground().statusBarsPadding().navigationBarsPadding()) { content() }
    }
}
