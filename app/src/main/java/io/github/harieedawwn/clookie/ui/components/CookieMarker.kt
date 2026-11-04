package io.github.harieedawwn.clookie.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.harieedawwn.clookie.design.Presets
import io.github.harieedawwn.clookie.model.CookiePattern

/** Drawing-guide steps: the frame, then one step per pattern cell. */
fun guideSteps(pattern: Int): List<List<Int>> =
    listOf(CookiePattern.frameOrder()) + CookiePattern.patternOrder(pattern).map { listOf(it) }

/**
 * A Cookie Code on a sheet of the owner's notebook paper: 2 mm squares (dotted) grouped 4x4 by
 * printed 8 mm lines (solid); one code cell is [cellSquares] x [cellSquares] small squares
 * (outlined, so 3 and 5 can be counted where they cross the printed lines), one empty cell
 * around the marker, ballpoint ink. Steps before [step] are
 * drawn; step [step] is being hatched by the pen up to [progress] (read in the draw phase);
 * later cells show as faint targets; the cell being drawn (or the next one) is outlined in red pen.
 * Colors are the Giấy ô li paper in every preset, because it shows the real page.
 */
@Composable
fun CookieGuide(pattern: Int, step: Int, progress: () -> Float, modifier: Modifier = Modifier, cellSquares: Int = 4) {
    val paper = Presets.GiayOLi.colors
    val steps = guideSteps(pattern)
    Canvas(modifier.aspectRatio(1f)) {
        val n = CookiePattern.Size + 2
        val cell = size.minDimension / n
        val square = cell / cellSquares
        drawRect(paper.surface)
        val hair = 1.dp.toPx()
        val dots = PathEffect.dashPathEffect(floatArrayOf(hair, 2.5.dp.toPx()))
        for (k in 0..n * cellSquares) {
            val p = k * square
            // Printed bold lines every 4 small squares, as on the real page.
            val major = k % 4 == 0
            val color = if (major) paper.gridMajor else paper.grid
            val effect = if (major) null else dots
            drawLine(color, Offset(p, 0f), Offset(p, size.height), if (major) hair * 1.5f else hair, pathEffect = effect)
            drawLine(color, Offset(0f, p), Offset(size.width, p), if (major) hair * 1.5f else hair, pathEffect = effect)
        }
        if (cellSquares != 4) {
            // Code cells do not sit on the printed lines: outline them so they can be counted.
            for (k in 1..CookiePattern.Size + 1) {
                val p = k * cell
                drawLine(paper.inkMuted, Offset(p, cell), Offset(p, size.height - cell), hair)
                drawLine(paper.inkMuted, Offset(cell, p), Offset(size.width - cell, p), hair)
            }
        }
        fun rect(index: Int): Rect {
            val r = index / CookiePattern.Size + 1
            val c = index % CookiePattern.Size + 1
            return Rect(Offset(c * cell, r * cell), Size(cell, cell))
        }
        val inkColor = paper.ink
        // Faint targets for everything not drawn yet.
        for (s in steps.indices) if (s > step) steps[s].forEach { drawRect(paper.accentSoft, rect(it).topLeft, rect(it).size) }
        for (s in 0 until minOf(step, steps.size)) steps[s].forEach { hatch(rect(it), 1f, inkColor) }
        if (step < steps.size) {
            val cells = steps[step]
            val p = progress().coerceIn(0f, 1f) * cells.size
            val current = p.toInt().coerceAtMost(cells.size - 1)
            cells.forEachIndexed { i, idx ->
                when {
                    i < current -> hatch(rect(idx), 1f, inkColor)
                    i == current -> {
                        drawRect(paper.accentSoft, rect(idx).topLeft, rect(idx).size)
                        val tip = hatch(rect(idx), p - current, inkColor)
                        if (p < cells.size) pen(tip, cell, inkColor, paper.surface)
                    }
                    else -> drawRect(paper.accentSoft, rect(idx).topLeft, rect(idx).size)
                }
            }
            // Red pen marks the cell being drawn, or the next one once this step is done.
            val next = when {
                p < cells.size -> cells[current]
                step + 1 < steps.size -> steps[step + 1].first()
                else -> null
            }
            if (next != null) drawRect(paper.accent, rect(next).topLeft, rect(next).size, style = Stroke(2.dp.toPx()))
        }
    }
}

/** Diagonal ballpoint hatching of [r] up to [amount]; returns the pen tip. */
private fun DrawScope.hatch(r: Rect, amount: Float, color: Color): Offset {
    val lines = 9
    val width = r.width / lines * 1.25f
    val total = lines * 2 - 1
    val drawn = amount.coerceIn(0f, 1f) * total
    var tip = r.topLeft
    clipRect(r.left, r.top, r.right, r.bottom) {
        for (k in 0 until total) {
            if (k > drawn) break
            // Lines run from bottom-left to top-right, sweeping from the top-left corner.
            val t = (k + 0.5f) / total * 2f
            val start = Offset(r.left, r.top + t * r.height)
            val end = Offset(r.left + t * r.width, r.top)
            val f = (drawn - k).coerceAtMost(1f)
            val stop = Offset(start.x + (end.x - start.x) * f, start.y + (end.y - start.y) * f)
            drawLine(color, start, stop, width, StrokeCap.Round)
            tip = stop
        }
    }
    return tip
}

/** Flat ballpoint: barrel and nib, leaning right, nib at [tip]. */
private fun DrawScope.pen(tip: Offset, cell: Float, ink: Color, paper: Color) {
    val back = Offset(tip.x + cell * 0.55f, tip.y - cell * 0.95f)
    drawLine(paper, tip, back, cell * 0.24f, StrokeCap.Round)
    drawLine(ink, tip, back, cell * 0.16f, StrokeCap.Round)
    drawCircle(ink, cell * 0.07f, tip)
}

/** Small solid marker (lists, chips): frame plus pattern in [color]. */
@Composable
fun CookieCodeIcon(pattern: Int, color: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    Canvas(modifier.size(size)) {
        val c = this.size.minDimension / CookiePattern.Size
        for (r in 0 until CookiePattern.Size) for (col in 0 until CookiePattern.Size) {
            if (CookiePattern.markerCell(pattern, r, col)) drawRect(color, Offset(col * c, r * c), Size(c + 1f, c + 1f))
        }
    }
}
