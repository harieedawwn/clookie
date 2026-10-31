package io.github.harieedawwn.clookie.model

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.CubicCurve2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Synthetic camera frames built from the owner's notebook photo (ref/notebook-paper.jpg):
 * ink is drawn onto the paper aligned with its printed 8 mm squares (k = 4), then the page is
 * seen through a random camera (perspective, rotation, marker size, optics blur, uneven light,
 * shadows, dim exposure with sensor noise) at the analysis resolution. Generated at test time.
 */
object SyntheticCamera {
    const val FrameW = 640
    const val FrameH = 480

    /** Grayscale image with float pixels. */
    class Img(val w: Int, val h: Int, val px: FloatArray = FloatArray(w * h)) {
        fun half(): Img {
            val o = Img(max(1, w / 2), max(1, h / 2))
            for (y in 0 until o.h) for (x in 0 until o.w) {
                val i = 2 * y * w + 2 * x
                o.px[y * o.w + x] = 0.25f * (px[i] + px[i + 1] + px[i + w] + px[i + w + 1])
            }
            return o
        }

        /** Bilinear with mirrored borders (paper continues past the photo edge). */
        fun mirror(x: Double, y: Double): Float {
            val xx = reflect(x, w)
            val yy = reflect(y, h)
            val x0 = floor(xx).toInt().coerceIn(0, w - 1)
            val y0 = floor(yy).toInt().coerceIn(0, h - 1)
            val x1 = min(x0 + 1, w - 1)
            val y1 = min(y0 + 1, h - 1)
            val fx = (xx - x0).toFloat().coerceIn(0f, 1f)
            val fy = (yy - y0).toFloat().coerceIn(0f, 1f)
            val a = px[y0 * w + x0] + (px[y0 * w + x1] - px[y0 * w + x0]) * fx
            val b = px[y1 * w + x0] + (px[y1 * w + x1] - px[y1 * w + x0]) * fx
            return a + (b - a) * fy
        }

        private fun reflect(v: Double, n: Int): Double {
            val period = 2.0 * (n - 1)
            var t = v % period
            if (t < 0) t += period
            return if (t > n - 1) period - t else t
        }
    }

    private val photoFile: File by lazy {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val f = File(dir, "ref/notebook-paper.jpg")
            if (f.exists()) return@lazy f
            dir = dir.parentFile
        }
        error("ref/notebook-paper.jpg not found")
    }

    /** Luminance of the photo (BT.601, like the camera's Y plane). */
    val photo: Img by lazy {
        val im = ImageIO.read(photoFile)
        val img = Img(im.width, im.height)
        val row = IntArray(im.width)
        for (y in 0 until im.height) {
            im.getRGB(0, y, im.width, 1, row, 0, im.width)
            for (x in 0 until im.width) {
                val p = row[x]
                img.px[y * im.width + x] = 0.299f * (p shr 16 and 255) + 0.587f * (p shr 8 and 255) + 0.114f * (p and 255)
            }
        }
        img
    }

    private val paperLevels: List<Img> by lazy {
        val l = mutableListOf(photo)
        repeat(6) { l += l.last().half() }
        l
    }

    /** Printed bold lines: vertical x = a + b*y, horizontal y = a + b*x, sorted. */
    val verticalLines: List<DoubleArray> by lazy { findLines(vertical = true) }
    val horizontalLines: List<DoubleArray> by lazy { findLines(vertical = false) }

    private fun findLines(vertical: Boolean): List<DoubleArray> {
        val p = photo
        val along = if (vertical) p.h else p.w
        val across = if (vertical) p.w else p.h
        val bands = listOf(0.2, 0.5, 0.8).map { (it * along).toInt() }
        val found = bands.map { center ->
            val prof = FloatArray(across)
            for (q in 0 until across) {
                var s = 0f
                for (t in center - 80 until center + 80) s += if (vertical) p.px[t * p.w + q] else p.px[q * p.w + t]
                prof[q] = s / 160
            }
            val minima = mutableListOf<Double>()
            for (q in 90 until across - 90) {
                var isMin = true
                var mean = 0f
                for (k in q - 80..q + 80) {
                    mean += prof[k]
                    if (abs(k - q) <= 50 && prof[k] < prof[q]) isMin = false
                }
                mean /= 161
                if (isMin && prof[q] < mean - 6) minima += q.toDouble()
            }
            minima
        }
        val lines = mutableListOf<DoubleArray>()
        for (m in found[1]) {
            val pts = mutableListOf(bands[1].toDouble() to m)
            found[0].minByOrNull { abs(it - m) }?.takeIf { abs(it - m) < 30 }?.let { pts += bands[0].toDouble() to it }
            found[2].minByOrNull { abs(it - m) }?.takeIf { abs(it - m) < 30 }?.let { pts += bands[2].toDouble() to it }
            if (pts.size < 2) continue
            val mt = pts.sumOf { it.first } / pts.size
            val mp = pts.sumOf { it.second } / pts.size
            val b = pts.sumOf { (it.first - mt) * (it.second - mp) } / pts.sumOf { (it.first - mt) * (it.first - mt) }
            lines += doubleArrayOf(mp - b * mt, b)
        }
        return lines.sortedBy { it[0] }
    }

    /** Point where vertical line [v] meets horizontal line [h]. */
    private fun cross(v: DoubleArray, h: DoubleArray): DoubleArray {
        val x = (v[0] + v[1] * h[0]) / (1 - v[1] * h[1])
        return doubleArrayOf(x, h[0] + h[1] * x)
    }

    /** A 5x5 block of printed 8 mm squares: corners[r][c] for r, c in 0..5. */
    class Grid(val pts: Array<Array<DoubleArray>>) {
        fun at(u: Double, v: Double): DoubleArray {
            // Bilinear inside the cell containing (u, v); extrapolates past the edge.
            val c = floor(u).toInt().coerceIn(0, 4)
            val r = floor(v).toInt().coerceIn(0, 4)
            val fu = u - c
            val fv = v - r
            val p00 = pts[r][c]
            val p10 = pts[r][c + 1]
            val p01 = pts[r + 1][c]
            val p11 = pts[r + 1][c + 1]
            return DoubleArray(2) { k ->
                (1 - fu) * (1 - fv) * p00[k] + fu * (1 - fv) * p10[k] + (1 - fu) * fv * p01[k] + fu * fv * p11[k]
            }
        }

        val cellPx: Double get() = hypot(pts[0][5][0] - pts[0][0][0], pts[0][5][1] - pts[0][0][1]) / 5
    }

    fun randomGrid(rnd: Random): Grid {
        val vs = verticalLines.filter { it[0] > 140 }
        val hs = horizontalLines
        fun okRun(lines: List<DoubleArray>, i: Int) =
            (0 until 5).all { k -> (lines[i + k + 1][0] - lines[i + k][0]) in 130.0..200.0 }
        val vi = (0..vs.size - 6).filter { okRun(vs, it) }
        val hi = (0..hs.size - 6).filter { okRun(hs, it) }
        check(vi.isNotEmpty() && hi.isNotEmpty()) { "grid lines not found in photo: ${vs.size} x ${hs.size}" }
        val i = vi.random(rnd)
        val j = hi.random(rnd)
        return Grid(Array(6) { r -> Array(6) { c -> cross(vs[i + c], hs[j + r]) } })
    }

    enum class Ink(val lo: Float, val hi: Float) { Blue(0.22f, 0.45f), Black(0.12f, 0.3f) }

    /** Ink coverage drawn at photo resolution; transmittance applied when composited. */
    class InkLayer(val x0: Int, val y0: Int, val w: Int, val h: Int, val transmittance: Float) {
        val image = BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)
        val g: Graphics2D = image.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            color = Color.WHITE
            translate(-x0.toDouble(), -y0.toDouble())
        }

        fun opacity(a: Float) {
            g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a.coerceIn(0f, 1f))
        }

        /** Multiplicative factor levels (1 = no ink), origin at (x0, y0). */
        fun levels(count: Int): List<Img> {
            val base = Img(w, h)
            val raster = image.raster
            val row = IntArray(w)
            for (y in 0 until h) {
                raster.getSamples(0, y, w, 1, 0, row)
                for (x in 0 until w) base.px[y * w + x] = 1f - row[x] / 255f * (1f - transmittance)
            }
            val l = mutableListOf(base)
            repeat(count - 1) { l += l.last().half() }
            return l
        }
    }

    fun inkLayer(grid: Grid, margin: Double, ink: Ink, rnd: Random): InkLayer {
        val a = grid.at(-margin, -margin)
        val b = grid.at(5 + margin, 5 + margin)
        val c = grid.at(5 + margin, -margin)
        val d = grid.at(-margin, 5 + margin)
        val minX = (min(min(a[0], b[0]), min(c[0], d[0])).toInt() / 64) * 64
        val minY = (min(min(a[1], b[1]), min(c[1], d[1])).toInt() / 64) * 64
        val maxX = max(max(a[0], b[0]), max(c[0], d[0])).toInt() + 64
        val maxY = max(max(a[1], b[1]), max(c[1], d[1])).toInt() + 64
        val w = ((maxX - minX + 63) / 64) * 64
        val h = ((maxY - minY + 63) / 64) * 64
        return InkLayer(minX, minY, w, h, ink.lo + rnd.nextFloat() * (ink.hi - ink.lo))
    }

    /** Ballpoint stroke width in photo pixels (about 0.4-0.6 mm at ~20 px/mm). */
    private fun penWidth(rnd: Random) = 8f + rnd.nextFloat() * 4f

    /**
     * Fill cell (r, c) the way people fill a square with a ballpoint: hatching, sometimes an
     * outline first, [coverage] 0.7-0.95 with gaps, strokes overflowing slightly past the edge.
     */
    fun fillCell(layer: InkLayer, grid: Grid, r: Int, c: Int, rnd: Random, angle: Double, coverage: Double) {
        val pen = penWidth(rnd)
        val g = layer.g
        g.stroke = BasicStroke(pen, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        val cell = grid.cellPx
        val ws = pen / cell
        val spacing = ws / coverage
        val dx = cos(angle)
        val dy = sin(angle)
        val over = rnd.nextDouble() * 0.1
        val path = Path2D.Double()
        var first = true
        var flip = false
        var o = -0.75
        while (o <= 0.75) {
            val skip = rnd.nextDouble() < 0.06
            val oo = o + (rnd.nextDouble() - 0.5) * spacing * 0.4
            val cxp = 0.5 - dy * oo
            val cyp = 0.5 + dx * oo
            val seg = clip(cxp, cyp, dx, dy, -over * rnd.nextDouble(), 1 + over * rnd.nextDouble())
            if (seg != null && !skip) {
                var (t0, t1) = seg
                if (flip) t0 = t1.also { t1 = t0 }
                val p0 = grid.at(c + cxp + dx * t0, r + cyp + dy * t0)
                val p1 = grid.at(c + cxp + dx * t1, r + cyp + dy * t1)
                if (first) path.moveTo(p0[0], p0[1]) else path.lineTo(p0[0], p0[1])
                path.lineTo(p1[0], p1[1])
                first = false
                flip = !flip
            }
            o += spacing
        }
        layer.opacity(0.8f + rnd.nextFloat() * 0.2f)
        g.draw(path)
        if (rnd.nextDouble() < 0.5) {
            val outline = Path2D.Double()
            val j = { (rnd.nextDouble() - 0.5) * 0.06 }
            val p = listOf(grid.at(c + 0.03 + j(), r + 0.03 + j()), grid.at(c + 0.97 + j(), r + 0.03 + j()),
                grid.at(c + 0.97 + j(), r + 0.97 + j()), grid.at(c + 0.03 + j(), r + 0.97 + j()))
            outline.moveTo(p[0][0], p[0][1])
            for (k in 1..4) outline.lineTo(p[k % 4][0], p[k % 4][1])
            layer.opacity(0.85f)
            g.draw(outline)
        }
    }

    /** Parameter range where the line (px, py) + t*(dx, dy) is inside [lo, hi]^2. */
    private fun clip(px: Double, py: Double, dx: Double, dy: Double, lo: Double, hi: Double): Pair<Double, Double>? {
        var t0 = -10.0
        var t1 = 10.0
        for ((p, d) in listOf(px to dx, py to dy)) {
            if (abs(d) < 1e-9) {
                if (p < lo || p > hi) return null
            } else {
                val a = (lo - p) / d
                val b = (hi - p) / d
                t0 = max(t0, min(a, b))
                t1 = min(t1, max(a, b))
            }
        }
        return if (t1 - t0 > 0.02) t0 to t1 else null
    }

    /** Draw a 5x5 marker for [pattern] the way the guide teaches. */
    fun drawMarker(layer: InkLayer, grid: Grid, pattern: Int, rnd: Random) {
        val base = listOf(0.0, PI / 4, -PI / 4, PI / 2).random(rnd)
        val coverage = 0.7 + rnd.nextDouble() * 0.25
        for (r in 0 until 5) for (c in 0 until 5) {
            if (CookiePattern.markerCell(pattern, r, c)) {
                fillCell(layer, grid, r, c, rnd, base + (rnd.nextDouble() - 0.5) * 0.5, coverage + (rnd.nextDouble() - 0.5) * 0.06)
            }
        }
    }

    /** n x n framed grid with random inner cells (other people's markers, wrong sizes). */
    fun drawFramedGrid(layer: InkLayer, grid: Grid, n: Int, rnd: Random) {
        val s = 5.0 / n
        for (r in 0 until n) for (c in 0 until n) {
            val frame = r == 0 || c == 0 || r == n - 1 || c == n - 1
            if (frame || rnd.nextDouble() < 0.4) fillRect(layer, grid, c * s, r * s, (c + 1) * s, (r + 1) * s, rnd)
        }
    }

    /** Fill the rectangle (u0, v0)-(u1, v1), in cell units of [grid], with hatching. */
    fun fillRect(layer: InkLayer, grid: Grid, u0: Double, v0: Double, u1: Double, v1: Double, rnd: Random, coverage: Double = 0.85) {
        // A grid whose cell (0, 0) is the rectangle.
        val rect = Grid(Array(6) { r -> Array(6) { c -> grid.at(u0 + (u1 - u0) * c, v0 + (v1 - v0) * r) } })
        fillCell(layer, rect, 0, 0, rnd, rnd.nextDouble() * PI, coverage)
    }

    fun scribbles(layer: InkLayer, grid: Grid, rnd: Random) {
        val g = layer.g
        repeat(8 + rnd.nextInt(20)) {
            g.stroke = BasicStroke(penWidth(rnd), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            layer.opacity(0.85f)
            val p = List(4) { grid.at(rnd.nextDouble() * 6 - 0.5, rnd.nextDouble() * 6 - 0.5) }
            g.draw(CubicCurve2D.Double(p[0][0], p[0][1], p[1][0], p[1][1], p[2][0], p[2][1], p[3][0], p[3][1]))
        }
        repeat(rnd.nextInt(4)) {
            val u = rnd.nextDouble() * 4
            val v = rnd.nextDouble() * 4
            fillRect(layer, grid, u, v, u + 0.5 + rnd.nextDouble() * 1.5, v + 0.5 + rnd.nextDouble() * 1.5, rnd)
        }
    }

    private val words = listOf(
        "Thứ hai", "đi chợ", "mua bánh", "học bài", "Toán", "Tiếng Việt", "ngủ sớm", "dậy lúc 6 giờ",
        "cà phê sữa đá", "bún chả", "nhớ gọi mẹ", "Ôn tập", "trả sách", "giặt đồ", "Clookie", "22/10",
    )

    fun handwriting(layer: InkLayer, grid: Grid, rnd: Random) {
        val g = layer.g
        val cell = grid.cellPx
        val font = Font(if (rnd.nextBoolean()) Font.SERIF else Font.SANS_SERIF, Font.ITALIC, (cell * (0.6 + rnd.nextDouble() * 0.5)).toInt())
        layer.opacity(0.9f)
        var v = 0.6
        while (v < 5.6) {
            val text = List(2 + rnd.nextInt(3)) { words.random(rnd) }.joinToString(" ")
            val origin = grid.at(-0.3 + rnd.nextDouble() * 0.5, v)
            val layout = TextLayout(text, font, g.fontRenderContext)
            val angle = (rnd.nextDouble() - 0.5) * 0.08
            val shape = layout.getOutline(AffineTransform.getRotateInstance(angle))
            g.fill(AffineTransform.getTranslateInstance(origin[0], origin[1]).createTransformedShape(shape))
            v += 0.9 + rnd.nextDouble() * 0.5
        }
    }

    fun checkerboard(layer: InkLayer, grid: Grid, n: Int, rnd: Random) {
        val s = 5.0 / n
        for (r in 0 until n) for (c in 0 until n) if ((r + c) % 2 == 0) fillRect(layer, grid, c * s, r * s, (c + 1) * s, (r + 1) * s, rnd)
    }

    /**
     * Scene = paper + ink seen by a pinhole camera (focal [Focal] px) tilted [tilt] rad about an
     * axis in the paper plane, rolled [roll] rad, at a distance where the marker is about [size] px.
     */
    class Scene(
        val grid: Grid,
        val ink: List<Img>?,
        val inkX0: Int,
        val inkY0: Int,
        val size: Double,
        val tilt: Double,
        val axis: Double,
        val roll: Double,
        val center: DoubleArray,
        val blur: Double,
        val light: Light,
    )

    class Light(
        val paperLevel: Double,
        val gx: Double,
        val gy: Double,
        val vignette: Double,
        val torch: Boolean,
        val shadow: DoubleArray?,
        val noise: Double,
    )

    private const val Focal = 500.0
    private const val CellMm = 8.0

    /** Image position of marker point (u, v) in cells, with a small hand shake. */
    private fun project(
        size: Double, tilt: Double, axis: Double, roll: Double, cx: Double, cy: Double, u: Double, v: Double,
    ): DoubleArray {
        val x0 = (u - 2.5) * CellMm
        val y0 = (v - 2.5) * CellMm
        val x1 = x0 * cos(roll) - y0 * sin(roll)
        val y1 = x0 * sin(roll) + y0 * cos(roll)
        // Rodrigues rotation about a = (cos axis, sin axis, 0).
        val ax = cos(axis)
        val ay = sin(axis)
        val dot = ax * x1 + ay * y1
        val crossZ = ax * y1 - ay * x1
        val c = cos(tilt)
        val sn = sin(tilt)
        val rx = x1 * c + ax * dot * (1 - c)
        val ry = y1 * c + ay * dot * (1 - c)
        val rz = crossZ * sn
        val d = Focal * 5 * CellMm / size
        val tx = (cx - FrameW / 2.0) * d / Focal
        val ty = (cy - FrameH / 2.0) * d / Focal
        val z = rz + d
        return doubleArrayOf(Focal * (rx + tx) / z + FrameW / 2.0, Focal * (ry + ty) / z + FrameH / 2.0)
    }

    fun scene(grid: Grid, layer: InkLayer?, rnd: Random, minSize: Double = 80.0, maxSize: Double = 320.0): Scene {
        val tilt = rnd.nextDouble() * 45 * PI / 180
        val axis = rnd.nextDouble() * 2 * PI
        val roll = rnd.nextDouble() * 2 * PI
        // Log-uniform size: far-away markers are as common as close ones.
        var size = exp(ln(minSize) + rnd.nextDouble() * (ln(maxSize) - ln(minSize)))
        var center = doubleArrayOf(FrameW / 2.0, FrameH / 2.0)
        // Shrink until the marker and its quiet zone fit in the frame somewhere.
        for (attempt in 0 until 200) {
            center = doubleArrayOf(rnd.nextDouble() * FrameW, rnd.nextDouble() * FrameH)
            val fits = listOf(-0.8 to -0.8, 5.8 to -0.8, 5.8 to 5.8, -0.8 to 5.8).all { (u, v) ->
                val p = project(size, tilt, axis, roll, center[0], center[1], u, v)
                p[0] in 3.0..FrameW - 3.0 && p[1] in 3.0..FrameH - 3.0
            }
            if (fits) break
            if (attempt % 10 == 9) size = (size * 0.95).coerceAtLeast(minSize)
        }
        val dim = rnd.nextDouble() < 0.35
        val paperLevel = if (dim) 55.0 + rnd.nextDouble() * 50 else 105.0 + rnd.nextDouble() * 110
        val shadow = if (rnd.nextDouble() < 0.4) {
            // Phone or hand shadow (half plane): point, direction, penumbra, depth. The penumbra
            // is the light's angular size (room lamps, diffusers: 0.04-0.2 rad) times the focal
            // length, independent of distance; hard sunlight shadows are not modeled.
            val a = rnd.nextDouble() * 2 * PI
            doubleArrayOf(
                rnd.nextDouble() * FrameW, rnd.nextDouble() * FrameH, cos(a), sin(a),
                (0.04 + rnd.nextDouble() * 0.16) * Focal, 0.2 + rnd.nextDouble() * 0.3,
            )
        } else null
        val light = Light(
            paperLevel = paperLevel,
            gx = (rnd.nextDouble() - 0.5) * 0.6,
            gy = (rnd.nextDouble() - 0.5) * 0.6,
            vignette = rnd.nextDouble() * 0.35,
            torch = dim && rnd.nextBoolean(),
            shadow = shadow,
            noise = 2.0 + 8.0 * ((215 - paperLevel) / 160).coerceIn(0.0, 1.0),
        )
        val levels = layer?.levels(7)
        return Scene(grid, levels, layer?.x0 ?: 0, layer?.y0 ?: 0, size, tilt, axis, roll, center, 0.5 + rnd.nextDouble() * 1.0, light)
    }

    /** Output corners of the marker square for one frame, with hand shake (px and radians). */
    fun frameCorners(s: Scene, rnd: Random, jitter: Double = 1.5): DoubleArray {
        val out = DoubleArray(8)
        val cx = s.center[0] + (rnd.nextDouble() - 0.5) * 2 * jitter
        val cy = s.center[1] + (rnd.nextDouble() - 0.5) * 2 * jitter
        val roll = s.roll + (rnd.nextDouble() - 0.5) * 0.01
        val unit = listOf(0.0 to 0.0, 5.0 to 0.0, 5.0 to 5.0, 0.0 to 5.0)
        for (k in 0 until 4) {
            val p = project(s.size, s.tilt, s.axis, roll, cx, cy, unit[k].first, unit[k].second)
            out[2 * k] = p[0]
            out[2 * k + 1] = p[1]
        }
        return out
    }

    /** Renders one camera frame; [corners] are where the marker square lands. */
    fun render(s: Scene, corners: DoubleArray, rnd: Random): ByteArray {
        val src = DoubleArray(8)
        for (k in 0 until 4) {
            val p = s.grid.pts[if (k >= 2) 5 else 0][if (k == 1 || k == 2) 5 else 0]
            src[2 * k] = p[0]
            src[2 * k + 1] = p[1]
        }
        val toPhoto = multiply(squareToQuad(src), invert(squareToQuad(corners)))
        val img = Img(FrameW, FrameH)
        val levels = paperLevels
        for (y in 0 until FrameH) for (x in 0 until FrameW) {
            val X = x + 0.5
            val Y = y + 0.5
            val d = toPhoto[6] * X + toPhoto[7] * Y + toPhoto[8]
            val px = (toPhoto[0] * X + toPhoto[1] * Y + toPhoto[2]) / d
            val py = (toPhoto[3] * X + toPhoto[4] * Y + toPhoto[5]) / d
            // Footprint of one output pixel on the photo -> pyramid level.
            val dx1 = (toPhoto[0] * (X + 1) + toPhoto[1] * Y + toPhoto[2]) / (toPhoto[6] * (X + 1) + toPhoto[7] * Y + toPhoto[8]) - px
            val dy1 = (toPhoto[3] * (X + 1) + toPhoto[4] * Y + toPhoto[5]) / (toPhoto[6] * (X + 1) + toPhoto[7] * Y + toPhoto[8]) - py
            val dx2 = (toPhoto[0] * X + toPhoto[1] * (Y + 1) + toPhoto[2]) / (toPhoto[6] * X + toPhoto[7] * (Y + 1) + toPhoto[8]) - px
            val dy2 = (toPhoto[3] * X + toPhoto[4] * (Y + 1) + toPhoto[5]) / (toPhoto[6] * X + toPhoto[7] * (Y + 1) + toPhoto[8]) - py
            val foot = sqrt(abs(dx1 * dy2 - dx2 * dy1)).coerceAtLeast(1.0)
            val level = (ln(foot) / ln(2.0)).roundToInt().coerceIn(0, levels.size - 1)
            val scale = (1 shl level).toDouble()
            var v = levels[level].mirror(px / scale - 0.5, py / scale - 0.5)
            val ink = s.ink
            if (ink != null) {
                val lx = (px - s.inkX0) / scale
                val ly = (py - s.inkY0) / scale
                val li = ink[level.coerceAtMost(ink.size - 1)]
                if (lx >= 0 && ly >= 0 && lx < li.w - 1 && ly < li.h - 1) v *= li.mirror(lx - 0.5, ly - 0.5)
            }
            img.px[y * FrameW + x] = v
        }
        val blurred = gaussian(img, s.blur)
        val l = s.light
        val base = l.paperLevel / 150.0
        val out = ByteArray(FrameW * FrameH)
        for (y in 0 until FrameH) for (x in 0 until FrameW) {
            val nx = (x - FrameW / 2.0) / FrameW
            val ny = (y - FrameH / 2.0) / FrameH
            var illum = (1 + l.gx * nx * 2 + l.gy * ny * 2) * (1 - l.vignette * (nx * nx + ny * ny) * 2)
            if (l.torch) illum *= 0.55 + 0.45 * exp(-(nx * nx + ny * ny) / 0.12)
            val sh = l.shadow
            if (sh != null) {
                val dist = (x - sh[0]) * sh[2] + (y - sh[1]) * sh[3]
                val t = (0.5 + dist / sh[4]).coerceIn(0.0, 1.0)
                illum *= 1 - sh[5] * t * t * (3 - 2 * t)
            }
            val value = blurred.px[y * FrameW + x] * base * illum + rnd.nextGaussian() * l.noise
            out[y * FrameW + x] = value.roundToInt().coerceIn(0, 255).toByte()
        }
        return out
    }

    private fun Random.nextGaussian(): Double {
        val u1 = nextDouble().coerceAtLeast(1e-12)
        val u2 = nextDouble()
        return sqrt(-2 * ln(u1)) * cos(2 * PI * u2)
    }

    private fun gaussian(img: Img, sigma: Double): Img {
        val r = (sigma * 3).toInt().coerceAtLeast(1)
        val k = FloatArray(2 * r + 1) { exp(-((it - r) * (it - r)) / (2 * sigma * sigma)).toFloat() }
        val sum = k.sum()
        for (i in k.indices) k[i] /= sum
        val tmp = Img(img.w, img.h)
        val out = Img(img.w, img.h)
        for (y in 0 until img.h) for (x in 0 until img.w) {
            var s = 0f
            for (i in -r..r) s += k[i + r] * img.px[y * img.w + (x + i).coerceIn(0, img.w - 1)]
            tmp.px[y * img.w + x] = s
        }
        for (y in 0 until img.h) for (x in 0 until img.w) {
            var s = 0f
            for (i in -r..r) s += k[i + r] * tmp.px[(y + i).coerceIn(0, img.h - 1) * img.w + x]
            out.px[y * img.w + x] = s
        }
        return out
    }

    /** Unit square (0,0),(1,0),(1,1),(0,1) -> quad, row-major 3x3. */
    fun squareToQuad(q: DoubleArray): DoubleArray {
        val (x0, y0, x1, y1) = listOf(q[0], q[1], q[2], q[3])
        val x2 = q[4]
        val y2 = q[5]
        val x3 = q[6]
        val y3 = q[7]
        val sx = x0 - x1 + x2 - x3
        val sy = y0 - y1 + y2 - y3
        val dx1 = x1 - x2
        val dx2 = x3 - x2
        val dy1 = y1 - y2
        val dy2 = y3 - y2
        val det = dx1 * dy2 - dx2 * dy1
        val g = (sx * dy2 - dx2 * sy) / det
        val h = (dx1 * sy - sx * dy1) / det
        return doubleArrayOf(
            x1 - x0 + g * x1, x3 - x0 + h * x3, x0,
            y1 - y0 + g * y1, y3 - y0 + h * y3, y0,
            g, h, 1.0,
        )
    }

    fun invert(m: DoubleArray): DoubleArray {
        val a = m[0]; val b = m[1]; val c = m[2]
        val d = m[3]; val e = m[4]; val f = m[5]
        val g = m[6]; val h = m[7]; val i = m[8]
        return doubleArrayOf(
            e * i - f * h, c * h - b * i, b * f - c * e,
            f * g - d * i, a * i - c * g, c * d - a * f,
            d * h - e * g, b * g - a * h, a * e - b * d,
        )
    }

    fun multiply(a: DoubleArray, b: DoubleArray) = DoubleArray(9) { k ->
        val r = k / 3
        val c = k % 3
        a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c]
    }

    /** Debug PNG of a frame (build/ only, never committed). */
    fun save(frame: ByteArray, file: File) {
        val im = BufferedImage(FrameW, FrameH, BufferedImage.TYPE_BYTE_GRAY)
        im.raster.setDataElements(0, 0, FrameW, FrameH, frame)
        file.parentFile.mkdirs()
        ImageIO.write(im, "png", file)
    }
}
