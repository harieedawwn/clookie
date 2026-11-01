package io.github.harieedawwn.clookie.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.design.BiteCorner
import io.github.harieedawwn.clookie.design.BiteShape
import io.github.harieedawwn.clookie.design.Clookie
import io.github.harieedawwn.clookie.design.Space
import io.github.harieedawwn.clookie.design.Stroke
import io.github.harieedawwn.clookie.design.TouchTarget
import io.github.harieedawwn.clookie.design.hardShadow
import io.github.harieedawwn.clookie.design.icons.ClookieIcon
import io.github.harieedawwn.clookie.design.icons.ClookieIcons
import io.github.harieedawwn.clookie.design.rememberHaptics

/**
 * Tactile press: with hard shadows on, the element slides onto its shadow while pressed;
 * otherwise it shrinks slightly. Animated values are read in the layer block only.
 */
@Composable
fun Modifier.pressable(
    shape: Shape,
    onClick: () -> Unit,
    shadow: Boolean = true,
    enabled: Boolean = true,
    role: Role = Role.Button,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, Clookie.motion.snappy(), label = "press")
    val style = Clookie.style
    val haptics = rememberHaptics()
    val slides = shadow && style.hardShadow
    return this
        .hardShadow(shape, enabled = shadow)
        .graphicsLayer {
            if (slides) {
                val o = style.shadowOffset.toPx() * press
                translationX = o
                translationY = o
            } else {
                val s = 1f - 0.04f * press
                scaleX = s
                scaleY = s
            }
        }
        .clip(shape)
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = role) {
            haptics.press()
            onClick()
        }
}

enum class ButtonKind { Primary, Secondary, Quiet }

@Composable
fun CookieButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = Clookie.colors
    val shape = Clookie.shapes.medium
    val (bg, fg) = when (kind) {
        ButtonKind.Primary -> c.accent to c.onAccent
        ButtonKind.Secondary -> c.surface to c.ink
        ButtonKind.Quiet -> Color.Transparent to c.ink
    }
    Row(
        modifier
            .heightIn(min = 56.dp)
            .pressable(shape, onClick, shadow = kind != ButtonKind.Quiet, enabled = enabled)
            .background(bg)
            .then(if (kind != ButtonKind.Quiet) Modifier.border(Stroke.regular, c.outline, shape) else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
            .padding(horizontal = Space.s6, vertical = Space.s3),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            ClookieIcon(icon, null, tint = fg, size = 22.dp)
            Spacer(Modifier.width(Space.s2))
        }
        Txt(text, Clookie.type.heading, color = fg, maxLines = 2, align = TextAlign.Center)
    }
}

/** Round icon button; [filled] uses ink with background-colored glyph. */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    size: Dp = TouchTarget,
    tint: Color? = null,
) {
    val c = Clookie.colors
    Box(
        modifier
            .size(size)
            .pressable(CircleShape, onClick, shadow = false)
            .background(if (filled) c.ink else Color.Transparent)
            .border(Stroke.regular, c.outline, CircleShape)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        ClookieIcon(icon, null, tint = tint ?: if (filled) c.background else c.ink, size = 22.dp)
    }
}

/** Shape for pill-like elements: a full pill except in the Crisp corner style. */
@Composable
fun pillShape(): Shape =
    if (Clookie.shapes.smallRadius < 6.dp) RoundedCornerShape(Clookie.shapes.smallRadius) else CircleShape

@Composable
fun PillChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val c = Clookie.colors
    val shape = pillShape()
    Row(
        modifier
            .heightIn(min = 44.dp)
            .pressable(shape, onClick, shadow = false, role = Role.Button)
            .background(if (selected) c.ink else c.surface)
            .border(Stroke.regular, c.outline, shape)
            .padding(horizontal = Space.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (selected) c.background else c.ink
        if (icon != null) {
            ClookieIcon(icon, null, tint = fg, size = 18.dp)
            Spacer(Modifier.width(Space.s1 + 2.dp))
        }
        Txt(text, Clookie.type.bodyStrong, color = fg, maxLines = 1)
    }
}

/** The add button: an accent square with a bite out of its top-right corner. */
@Composable
fun BiteAddButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 60.dp) {
    val c = Clookie.colors
    val radius = if (Clookie.shapes.smallRadius < 6.dp) 6.dp else Clookie.shapes.mediumRadius
    val shape = BiteShape(radius, size * 0.26f, BiteCorner.TopEnd, size * 0.06f)
    Box(
        modifier
            .size(size)
            .pressable(shape, onClick)
            .background(c.accent)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        ClookieIcon(ClookieIcons.Plus, null, tint = c.onAccent, size = 28.dp)
    }
}

/** Segmented choice (e.g. language). Not a stock segmented button: flat blocks, ink selection. */
@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Clookie.colors
    val shape = Clookie.shapes.small
    Row(
        modifier
            .clip(shape)
            .border(Stroke.regular, c.outline, shape)
            .padding(Space.s1),
        horizontalArrangement = Arrangement.spacedBy(Space.s1),
    ) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clip(if (Clookie.shapes.smallRadius < 6.dp) shape else RoundedCornerShape(Clookie.shapes.smallRadius - 4.dp))
                    .background(if (isSel) c.ink else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .padding(horizontal = Space.s3),
                contentAlignment = Alignment.Center,
            ) {
                Txt(label, Clookie.type.bodyStrong, color = if (isSel) c.background else c.ink, maxLines = 2, align = TextAlign.Center)
            }
        }
    }
}
