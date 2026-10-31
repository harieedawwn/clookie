package io.github.harieedawwn.clookie.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.rememberHaptics

/**
 * On: a whole cookie sits on the accent track. Off: the cookie slides back and a bite is
 * taken out of it. All motion is read in the draw phase; the Paths are reused (no per-frame
 * allocation).
 */
@Composable
fun BiteToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val c = Clookie.colors
    val motion = Clookie.motion
    val haptics = rememberHaptics()
    val pos by animateFloatAsState(if (checked) 1f else 0f, motion.playful(), label = "thumb")
    val bite by animateFloatAsState(if (checked) 0f else 1f, motion.standard(), label = "bite")
    val crisp = Clookie.shapes.smallRadius < 6.dp
    val thumbPath = remember { Path() }
    val bitePath = remember { Path() }
    val bitten = remember { Path() }

    Box(
        modifier
            .toggleable(value = checked, role = Role.Switch) {
                haptics.toggle(it)
                onCheckedChange(it)
            }
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 60.dp, height = 34.dp)) {
            val stroke = Stroke.regular.toPx()
            val h = size.height
            val trackRadius = if (crisp) 4.dp.toPx() else h / 2
            val inset = stroke / 2
            val track = Rect(inset, inset, size.width - inset, h - inset)
            drawRoundRect(
                color = if (pos > 0.5f) c.accent else c.surface,
                topLeft = track.topLeft, size = track.size, cornerRadius = CornerRadius(trackRadius),
            )
            drawRoundRect(
                color = c.outline, topLeft = track.topLeft, size = track.size,
                cornerRadius = CornerRadius(trackRadius), style = DrawStroke(stroke),
            )
            val r = (h - 10.dp.toPx()) / 2
            val cx = lerp(5.dp.toPx() + r, size.width - 5.dp.toPx() - r, pos)
            val cy = h / 2
            thumbPath.rewind()
            if (crisp) {
                thumbPath.addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset(cx, cy), r), CornerRadius(3.dp.toPx())))
            } else {
                thumbPath.addOval(Rect(Offset(cx, cy), r))
            }
            bitePath.rewind()
            bitePath.addOval(Rect(Offset(cx + r * 0.78f, cy - r * 0.62f), r * 0.55f * bite))
            bitten.op(thumbPath, bitePath, PathOperation.Difference)
            clipPath(bitten) {
                drawRect(if (pos > 0.5f) c.surface else c.inkMuted, Rect(Offset(cx, cy), r).topLeft, Rect(Offset(cx, cy), r).size)
                val chip = if (pos > 0.5f) c.chip else c.surface
                drawCircle(chip, r * 0.16f, Offset(cx - r * 0.35f, cy - r * 0.15f))
                drawCircle(chip, r * 0.13f, Offset(cx + r * 0.2f, cy + r * 0.38f))
                drawCircle(chip, r * 0.11f, Offset(cx - r * 0.25f, cy + r * 0.45f))
            }
        }
    }
}
