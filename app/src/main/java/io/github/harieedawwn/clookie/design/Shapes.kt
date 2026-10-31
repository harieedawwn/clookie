package io.github.harieedawwn.clookie.design

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

enum class BiteCorner { TopStart, TopEnd, BottomEnd, BottomStart }

/**
 * The signature motif: a rounded rectangle (or circle, with a huge [cornerRadius]) with a
 * circular notch bitten out near one corner. [biteCenterInset] moves the bite center inward
 * from the corner; 0 puts it exactly on the corner.
 */
class BiteShape(
    private val cornerRadius: Dp,
    private val biteRadius: Dp,
    private val corner: BiteCorner = BiteCorner.TopEnd,
    private val biteCenterInset: Dp = 0.dp,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { cornerRadius.toPx() }.coerceAtMost(size.minDimension / 2)
        val br = with(density) { biteRadius.toPx() }
        val inset = with(density) { biteCenterInset.toPx() }
        val body = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(r))) }
        val rtl = layoutDirection == LayoutDirection.Rtl
        val atEnd = (corner == BiteCorner.TopEnd || corner == BiteCorner.BottomEnd) != rtl
        val cx = if (atEnd) size.width - inset else inset
        val cy = if (corner == BiteCorner.TopStart || corner == BiteCorner.TopEnd) inset else size.height - inset
        val bite = Path().apply { addOval(Rect(Offset(cx, cy), br)) }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, bite))
    }

    override fun equals(other: Any?) = other is BiteShape && other.cornerRadius == cornerRadius &&
        other.biteRadius == biteRadius && other.corner == corner && other.biteCenterInset == biteCenterInset

    override fun hashCode() = listOf(cornerRadius, biteRadius, corner, biteCenterInset).hashCode()
}

/**
 * Solid, zero-blur offset shadow in the theme's shadow color. Draws nothing when the active
 * style has hard shadows off. Place it before background/clip modifiers.
 */
fun Modifier.hardShadow(shape: Shape, enabled: Boolean = true): Modifier =
    this then HardShadowElement(shape, enabled)

private data class HardShadowElement(val shape: Shape, val enabled: Boolean) : ModifierNodeElement<HardShadowNode>() {
    override fun create() = HardShadowNode(shape, enabled)
    override fun update(node: HardShadowNode) {
        node.shape = shape
        node.enabled = enabled
        node.invalidateDraw()
    }
    override fun InspectorInfo.inspectableProperties() { name = "hardShadow" }
}

private class HardShadowNode(var shape: Shape, var enabled: Boolean) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        val style = currentValueOf(LocalClookieStyle)
        if (enabled && style.hardShadow) {
            val o = style.shadowOffset.toPx()
            val outline = shape.createOutline(size, layoutDirection, this)
            translate(o, o) { drawOutline(outline, currentValueOf(LocalClookieColors).shadow) }
        }
        drawContent()
    }
}

/** Pressed state as a flat solid overlay: no ripple, no glow. Clip the element to its shape. */
object FlatIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = FlatIndicationNode(interactionSource)
    override fun equals(other: Any?) = other === this
    override fun hashCode() = 7
}

private class FlatIndicationNode(private val source: InteractionSource) :
    Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    private var pressed = false

    override fun onAttach() {
        coroutineScope.launch {
            var count = 0
            source.interactions.collect {
                when (it) {
                    is PressInteraction.Press -> count++
                    is PressInteraction.Release, is PressInteraction.Cancel -> count--
                }
                val now = count > 0
                if (now != pressed) {
                    pressed = now
                    invalidateDraw()
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (pressed) {
            val colors = currentValueOf(LocalClookieColors)
            drawRect(colors.ink.copy(alpha = if (colors.isDark) 0.14f else 0.08f))
        }
    }
}

/**
 * Printed notebook grid (Giấy ô li): dotted minor lines, solid major lines every 4 cells and a
 * red margin line, like the real paper. Built once per size into one path per line weight.
 */
fun Modifier.paperGrid(colors: ClookieColors, cell: Dp = 8.dp): Modifier =
    if (!colors.hasGrid) this else drawWithCache {
        val step = cell.toPx()
        val minor = Path()
        val major = Path()
        var i = 0
        var x = 0f
        while (x <= size.width) {
            (if (i % 4 == 0) major else minor).apply { moveTo(x, 0f); lineTo(x, size.height) }
            x += step; i++
        }
        i = 0
        var y = 0f
        while (y <= size.height) {
            (if (i % 4 == 0) major else minor).apply { moveTo(0f, y); lineTo(size.width, y) }
            y += step; i++
        }
        val hair = 1.dp.toPx()
        val dots = PathEffect.dashPathEffect(floatArrayOf(hair, 2.dp.toPx()))
        val marginX = step * 5
        onDrawBehind {
            drawRect(colors.background)
            drawPath(minor, colors.grid, style = DrawStroke(hair, pathEffect = dots))
            drawPath(major, colors.gridMajor, style = DrawStroke(hair))
            drawLine(colors.margin, Offset(marginX, 0f), Offset(marginX, size.height), hair * 1.5f)
        }
    }