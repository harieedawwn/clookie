package io.github.harieedawwn.clookie.model

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Finds hand-drawn Cookie Code markers in a camera luminance (Y) frame and reads their
 * 3x3 pattern (in the frame's orientation; compare with [CookiePattern.matches]).
 *
 * 1. 5x5 box blur, then a paper-white map (local max over about 12-16 px).
 * 2. Ink = darker than [DarkRatio] of the local paper white; a 3x3 opening removes the thin
 *    printed grid lines and pen texture specks.
 * 3. Each 8-connected blob of plausible size: outer contour, four corners, one line fitted
 *    per side (robust to rounded corners and overflow), corners from line intersections.
 * 4. Homography onto the 5x5 marker; paper (outside ring) and ink (frame cells) levels are
 *    fitted as planes so light gradients cancel; every cell is decided by the share of
 *    its central samples darker than the paper/ink midpoint. The frame must be mostly inked.
 *
 * One instance per analysis thread. Buffers are allocated on the first frame of a given size;
 * after that [detect] allocates nothing.
 */
class CookieDetector(val maxMarkers: Int = 4) {
    /** Markers found by the last [detect]. */
    var count = 0
        private set

    /** Pattern of marker i as read with its first corner as the top-left. */
    val patterns = IntArray(maxMarkers)

    /** Corners of marker i: x0, y0 .. x3, y3, clockwise on screen. */
    val corners = FloatArray(maxMarkers * 8)

    /** Mean luminance of the last frame (0..255), for auto-torch. */
    var meanLuma = 0
        private set

    /** Why candidates were rejected in the last frame (index = Reject* constant); for tests. */
    val rejects = IntArray(RejectKinds)

    private var w = 0
    private var h = 0
    private var integral = IntArray(0)
    private var smooth = IntArray(0)
    private var mask = ByteArray(0)
    private var tmp = ByteArray(0)
    private var stack = IntArray(0)
    private var nbx = 0
    private var nby = 0
    private var blockMax = IntArray(0)
    private var blockTmp = IntArray(0)
    private var rowBg = FloatArray(0)
    private var xBlock = IntArray(0)
    private var xWeight = FloatArray(0)

    private val cx = IntArray(MaxContour)
    private val cy = IntArray(MaxContour)
    private var contourLen = 0

    private val cornerIdx = IntArray(4)
    private val qx = FloatArray(4)
    private val qy = FloatArray(4)
    private val lineNx = FloatArray(4)
    private val lineNy = FloatArray(4)
    private val lineC = FloatArray(4)
    private var lastDist = 0f

    // Homography from the unit square to the image.
    private var ha = 0f
    private var hb = 0f
    private var hc = 0f
    private var hd = 0f
    private var he = 0f
    private var hf = 0f
    private var hg = 0f
    private var hh = 0f
    private var mapX = 0f
    private var mapY = 0f

    private val su = FloatArray(MaxSamples)
    private val sv = FloatArray(MaxSamples)
    private val sval = FloatArray(MaxSamples)
    private val paper = FloatArray(3)
    private val ink = FloatArray(3)
    private val plane = FloatArray(3)

    /**
     * Detects markers in [y] ([width] x [height], rows [rowStride] bytes apart).
     * Returns the number found; results are in [patterns] and [corners].
     */
    fun detect(y: ByteArray, width: Int, height: Int, rowStride: Int = width): Int {
        prepare(width, height)
        count = 0
        rejects.fill(0)
        buildIntegral(y, rowStride)
        boxBlur()
        paperWhite()
        threshold()
        open3x3()
        findBlobs()
        return count
    }

    private fun prepare(width: Int, height: Int) {
        if (width == w && height == h) return
        require(width >= 32 && height >= 32) { "frame too small" }
        w = width
        h = height
        integral = IntArray((w + 1) * (h + 1))
        smooth = IntArray(w * h)
        mask = ByteArray(w * h)
        tmp = ByteArray(w * h)
        stack = IntArray(w * h)
        nbx = (w + Block - 1) / Block
        nby = (h + Block - 1) / Block
        blockMax = IntArray(nbx * nby)
        blockTmp = IntArray(nbx * nby)
        rowBg = FloatArray(nbx)
        xBlock = IntArray(w)
        xWeight = FloatArray(w)
        for (x in 0 until w) {
            val f = ((x + 0.5f) / Block - 0.5f).coerceIn(0f, (nbx - 1).toFloat())
            val b = f.toInt().coerceAtMost(maxOf(nbx - 2, 0))
            xBlock[x] = b
            xWeight[x] = if (nbx > 1) f - b else 0f
        }
    }

    private fun buildIntegral(y: ByteArray, stride: Int) {
        val iw = w + 1
        var total = 0L
        for (row in 0 until h) {
            var rowSum = 0
            val src = row * stride
            val dst = (row + 1) * iw
            for (x in 0 until w) {
                rowSum += y[src + x].toInt() and 0xFF
                integral[dst + x + 1] = integral[dst - iw + x + 1] + rowSum
            }
            total += rowSum
        }
        meanLuma = (total / (w.toLong() * h)).toInt()
    }

    /** 5x5 mean: averages noise and pen texture for both the ink mask and the cell samples. */
    private fun boxBlur() {
        val iw = w + 1
        for (y in 0 until h) {
            val y0 = maxOf(y - 2, 0)
            val y1 = minOf(y + 2, h - 1) + 1
            for (x in 0 until w) {
                val x0 = maxOf(x - 2, 0)
                val x1 = minOf(x + 2, w - 1) + 1
                val s = integral[y1 * iw + x1] - integral[y0 * iw + x1] - integral[y1 * iw + x0] + integral[y0 * iw + x0]
                smooth[y * w + x] = s / ((x1 - x0) * (y1 - y0))
            }
        }
    }

    /**
     * Local paper white: per-block max of [smooth], then a small max filter over blocks
     * ([BgRadius] blocks, about 12-16 px). Only ink within that distance of paper is flagged,
     * which is the band the outer contour needs; thick ink interiors stay hollow. A small
     * window keeps soft shadows and illumination valleys (wider than the window) as paper.
     */
    private fun paperWhite() {
        for (by in 0 until nby) for (bx in 0 until nbx) {
            var m = 0
            val yEnd = minOf((by + 1) * Block, h)
            val xEnd = minOf((bx + 1) * Block, w)
            for (y in by * Block until yEnd) {
                val row = y * w
                for (x in bx * Block until xEnd) {
                    val v = smooth[row + x]
                    if (v > m) m = v
                }
            }
            blockMax[by * nbx + bx] = m
        }
        blockFilter(blockMax, blockTmp, horizontal = true, max = true)
        blockFilter(blockTmp, blockMax, horizontal = false, max = true)
    }

    private fun blockFilter(src: IntArray, dst: IntArray, horizontal: Boolean, max: Boolean) {
        for (by in 0 until nby) for (bx in 0 until nbx) {
            var m = if (max) 0 else 255
            if (horizontal) {
                val row = by * nbx
                for (k in maxOf(bx - BgRadius, 0)..minOf(bx + BgRadius, nbx - 1)) {
                    m = if (max) maxOf(m, src[row + k]) else minOf(m, src[row + k])
                }
            } else {
                for (k in maxOf(by - BgRadius, 0)..minOf(by + BgRadius, nby - 1)) {
                    m = if (max) maxOf(m, src[k * nbx + bx]) else minOf(m, src[k * nbx + bx])
                }
            }
            dst[by * nbx + bx] = m
        }
    }

    private fun threshold() {
        for (y in 0 until h) {
            val f = ((y + 0.5f) / Block - 0.5f).coerceIn(0f, (nby - 1).toFloat())
            val b0 = f.toInt().coerceAtMost(maxOf(nby - 2, 0))
            val b1 = minOf(b0 + 1, nby - 1)
            val wy = if (nby > 1) f - b0 else 0f
            for (bx in 0 until nbx) rowBg[bx] = blockMax[b0 * nbx + bx] * (1 - wy) + blockMax[b1 * nbx + bx] * wy
            val row = y * w
            for (x in 0 until w) {
                val b = xBlock[x]
                val wx = xWeight[x]
                val bg = rowBg[b] * (1 - wx) + rowBg[minOf(b + 1, nbx - 1)] * wx
                val v = smooth[row + x]
                mask[row + x] = if (v < bg * DarkRatio && bg - v >= MinDelta) 1 else 0
            }
        }
    }

    /**
     * Opening with a 3x3 square: removes printed lines and pen specks up to 2 px (a wider
     * opening loses small, blurred blue markers). Clears the 1-pixel border afterwards so
     * contour tracing never leaves the image.
     */
    private fun open3x3() {
        erode3x3()
        dilate3x3()
        for (x in 0 until w) {
            mask[x] = 0
            mask[(h - 1) * w + x] = 0
        }
        for (y in 0 until h) {
            mask[y * w] = 0
            mask[y * w + w - 1] = 0
        }
    }

    private fun erode3x3() {
        for (y in 0 until h) {
            val r = y * w
            tmp[r] = 0
            tmp[r + w - 1] = 0
            for (x in 1 until w - 1) {
                tmp[r + x] = if (mask[r + x - 1].toInt() and mask[r + x].toInt() and mask[r + x + 1].toInt() != 0) 1 else 0
            }
        }
        for (x in 0 until w) {
            mask[x] = 0
            mask[(h - 1) * w + x] = 0
        }
        for (y in 1 until h - 1) {
            val r = y * w
            for (x in 0 until w) {
                mask[r + x] = if (tmp[r - w + x].toInt() and tmp[r + x].toInt() and tmp[r + w + x].toInt() != 0) 1 else 0
            }
        }
    }

    private fun dilate3x3() {
        for (y in 0 until h) {
            val r = y * w
            for (x in 0 until w) {
                val l = if (x > 0) mask[r + x - 1].toInt() else 0
                val rr = if (x < w - 1) mask[r + x + 1].toInt() else 0
                tmp[r + x] = if (l or mask[r + x].toInt() or rr != 0) 1 else 0
            }
        }
        for (y in 0 until h) {
            val r = y * w
            for (x in 0 until w) {
                val up = if (y > 0) tmp[r - w + x].toInt() else 0
                val dn = if (y < h - 1) tmp[r + w + x].toInt() else 0
                mask[r + x] = if (up or tmp[r + x].toInt() or dn != 0) 1 else 0
            }
        }
    }

    private fun findBlobs() {
        val minSide = MinSide
        val maxSide = minOf(w, h) - 4
        var candidates = 0
        for (start in 0 until w * h) {
            if (mask[start].toInt() != 1) continue
            // Flood fill (8-connected), marking visited pixels with 2.
            var sp = 0
            stack[sp++] = start
            mask[start] = 2
            var minX = w
            var maxX = -1
            var minY = h
            var maxY = -1
            var area = 0
            while (sp > 0) {
                val p = stack[--sp]
                val px = p % w
                val py = p / w
                area++
                if (px < minX) minX = px
                if (px > maxX) maxX = px
                if (py < minY) minY = py
                if (py > maxY) maxY = py
                for (dy in -1..1) {
                    val row = (py + dy) * w
                    for (dx in -1..1) {
                        val q = row + px + dx
                        if (mask[q].toInt() == 1) {
                            mask[q] = 2
                            stack[sp++] = q
                        }
                    }
                }
            }
            val bw = maxX - minX + 1
            val bh = maxY - minY + 1
            if (bw < minSide || bh < minSide || bw > maxSide || bh > maxSide) continue
            if (minX <= 1 || minY <= 1 || maxX >= w - 2 || maxY >= h - 2) continue
            if (maxOf(bw, bh) > 4 * minOf(bw, bh)) continue
            if (area < 0.06f * bw * bh) continue
            if (count >= maxMarkers || candidates >= MaxCandidates) return
            candidates++
            decodeBlob(start)
        }
    }

    private fun decodeBlob(start: Int) {
        if (!traceContour(start)) return reject(RejectContour)
        if (!roughCorners()) return reject(RejectCorners)
        val why = refineCorners()
        if (why >= 0) return reject(why)
        orderClockwise()
        computeHomography()
        readCells()
    }

    private fun reject(kind: Int) {
        rejects[kind]++
    }

    /** Moore-neighbor tracing of the outer boundary from the blob's top-left pixel. */
    private fun traceContour(start: Int): Boolean {
        val sx = start % w
        val sy = start / w
        var x = sx
        var y = sy
        var back = 4 // came from the west, which is background
        var first = -1
        var n = 0
        while (true) {
            var d = -1
            for (k in 1..8) {
                val dir = (back + k) and 7
                if (mask[(y + DY[dir]) * w + x + DX[dir]].toInt() != 0) {
                    d = dir
                    break
                }
            }
            if (d < 0) return false
            if (first < 0) {
                first = d
            } else if (x == sx && y == sy && d == first) {
                break
            }
            if (n >= MaxContour) return false
            cx[n] = x
            cy[n] = y
            n++
            x += DX[d]
            y += DY[d]
            back = if (d and 1 == 0) (d + 6) and 7 else (d + 5) and 7
        }
        contourLen = n
        return n >= 4 * MinSide / 2
    }

    private fun farthestFromLine(from: Int, to: Int, ax: Float, ay: Float, bx: Float, by: Float): Int {
        val n = contourLen
        val dx = bx - ax
        val dy = by - ay
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-3f)
        var best = -1
        var bestD = -1f
        var i = (from + 1) % n
        while (i != to) {
            val d = abs((cx[i] - ax) * dy - (cy[i] - ay) * dx) / len
            if (d > bestD) {
                bestD = d
                best = i
            }
            i = (i + 1) % n
        }
        lastDist = bestD
        return best
    }

    /** Four contour indices at the corners, in contour order. */
    private fun roughCorners(): Boolean {
        val n = contourLen
        var mx = 0f
        var my = 0f
        for (i in 0 until n) {
            mx += cx[i]
            my += cy[i]
        }
        mx /= n
        my /= n
        var i0 = 0
        var best = -1f
        for (i in 0 until n) {
            val d = (cx[i] - mx) * (cx[i] - mx) + (cy[i] - my) * (cy[i] - my)
            if (d > best) {
                best = d
                i0 = i
            }
        }
        var i2 = 0
        best = -1f
        for (i in 0 until n) {
            val d = (cx[i] - cx[i0]).toFloat().let { it * it } + (cy[i] - cy[i0]).toFloat().let { it * it }
            if (d > best) {
                best = d
                i2 = i
            }
        }
        if (i0 == i2) return false
        val diag = sqrt(best)
        val x0 = cx[i0].toFloat()
        val y0 = cy[i0].toFloat()
        val x2 = cx[i2].toFloat()
        val y2 = cy[i2].toFloat()
        val fa = farthestFromLine(i0, i2, x0, y0, x2, y2)
        val da = lastDist
        val fb = farthestFromLine(i2, i0, x2, y2, x0, y0)
        val db = lastDist
        if (fa < 0 || fb < 0) return false
        if (minOf(da, db) >= 0.2f * diag) {
            setCorners(i0, fa, i2, fb)
            return true
        }
        // i0-i2 is a side: the far chain holds two corners.
        val from = if (da > db) i0 else i2
        val to = if (da > db) i2 else i0
        val v = if (da > db) fa else fb
        val u1 = farthestFromLine(from, v, cx[from].toFloat(), cy[from].toFloat(), cx[v].toFloat(), cy[v].toFloat())
        val d1 = lastDist
        val u2 = farthestFromLine(v, to, cx[v].toFloat(), cy[v].toFloat(), cx[to].toFloat(), cy[to].toFloat())
        val d2 = lastDist
        if (u1 < 0 || u2 < 0) return false
        if (d1 > d2) setCorners(from, u1, v, to) else setCorners(from, v, u2, to)
        return true
    }

    private fun setCorners(a: Int, b: Int, c: Int, d: Int) {
        cornerIdx[0] = a
        cornerIdx[1] = b
        cornerIdx[2] = c
        cornerIdx[3] = d
    }

    /**
     * Fit a line to the middle of each side and intersect neighbors; returns -1 or a Reject* kind.
     * Side points are picked by position (near the rough side), not by contour order, so a gap
     * in the drawn frame that lets the contour wander inside does not matter.
     */
    private fun refineCorners(): Int {
        val n = contourLen
        var perimeter = 0f
        for (s in 0 until 4) {
            val a = cornerIdx[s]
            val b = cornerIdx[(s + 1) % 4]
            val ax = cx[a].toFloat()
            val ay = cy[a].toFloat()
            val bx = cx[b].toFloat()
            val by = cy[b].toFloat()
            val chord = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
            if (chord < MinSide * 0.6f) return RejectLines
            perimeter += chord
            lineNx[s] = -(by - ay) / chord
            lineNy[s] = (bx - ax) / chord
            lineC[s] = lineNx[s] * ax + lineNy[s] * ay
            if (!fitSide(s, ax, ay, bx, by, 2f + 0.04f * chord, 2f + 0.08f * chord)) return RejectLines
            if (!fitSide(s, ax, ay, bx, by, 1.5f + 0.015f * chord, 1.5f + 0.025f * chord)) return RejectLines
        }
        if (n > 4f * perimeter) return RejectJagged
        for (s in 0 until 4) {
            val p = (s + 3) % 4
            val det = lineNx[p] * lineNy[s] - lineNy[p] * lineNx[s]
            if (abs(det) < 0.05f) return RejectLines
            val x = (lineC[p] * lineNy[s] - lineNy[p] * lineC[s]) / det
            val y = (lineNx[p] * lineC[s] - lineC[p] * lineNx[s]) / det
            val k = cornerIdx[s]
            val dx = x - cx[k]
            val dy = y - cy[k]
            if (dx * dx + dy * dy > (0.08f * perimeter) * (0.08f * perimeter)) return RejectMoved
            qx[s] = x
            qy[s] = y
        }
        // Every side must be traced along most of its length (10 bins, at most 3 empty; hand-drawn
        // edges wobble).
        val tol = 2f + 0.02f * perimeter
        for (s in 0 until 4) {
            val e = (s + 1) % 4
            val dx = qx[e] - qx[s]
            val dy = qy[e] - qy[s]
            val len2 = dx * dx + dy * dy
            var bins = 0
            for (i in 0 until n) {
                if (abs(lineNx[s] * cx[i] + lineNy[s] * cy[i] - lineC[s]) > tol) continue
                val t = ((cx[i] - qx[s]) * dx + (cy[i] - qy[s]) * dy) / len2
                if (t < 0f || t >= 1f) continue
                bins = bins or (1 shl (t * 10).toInt())
            }
            if (Integer.bitCount(bins) < 7) return RejectOutline
        }
        return -1
    }

    /**
     * Total least squares line through the contour points that project onto the middle 76% of
     * the rough side A-B and lie at most [inside] px inside / [outside] px outside the current
     * line. One-sided because a contour that slips in through a gap in the drawing also traces
     * the inner edge of the ink band, parallel to the real edge.
     */
    private fun fitSide(side: Int, ax: Float, ay: Float, bx: Float, by: Float, inside: Float, outside: Float): Boolean {
        val n = contourLen
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        val nx0 = lineNx[side]
        val ny0 = lineNy[side]
        val c0 = lineC[side]
        // Outward = away from the middle of the rough quad.
        val mx0 = 0.25f * (cx[cornerIdx[0]] + cx[cornerIdx[1]] + cx[cornerIdx[2]] + cx[cornerIdx[3]])
        val my0 = 0.25f * (cy[cornerIdx[0]] + cy[cornerIdx[1]] + cy[cornerIdx[2]] + cy[cornerIdx[3]])
        val out = if (nx0 * mx0 + ny0 * my0 - c0 > 0) -1f else 1f
        var m = 0
        var sx = 0.0
        var sy = 0.0
        var sxx = 0.0
        var syy = 0.0
        var sxy = 0.0
        for (i in 0 until n) {
            val x = cx[i].toFloat()
            val y = cy[i].toFloat()
            val t = ((x - ax) * dx + (y - ay) * dy) / len2
            if (t < 0.12f || t > 0.88f) continue
            val d = out * (nx0 * x + ny0 * y - c0)
            if (d < -inside || d > outside) continue
            m++
            sx += x
            sy += y
            sxx += x * x
            syy += y * y
            sxy += x * y
        }
        if (m < 5) return false
        val mx = sx / m
        val my = sy / m
        val cxx = sxx / m - mx * mx
        val cyy = syy / m - my * my
        val cxy = sxy / m - mx * my
        val theta = 0.5 * atan2(2 * cxy, cxx - cyy)
        val nx = -sin(theta)
        val ny = cos(theta)
        lineNx[side] = nx.toFloat()
        lineNy[side] = ny.toFloat()
        lineC[side] = (nx * mx + ny * my).toFloat()
        return true
    }

    /** Clockwise on screen (y down) so the marker is read unmirrored. */
    private fun orderClockwise() {
        var area = 0f
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            area += qx[i] * qy[j] - qx[j] * qy[i]
        }
        if (area < 0) {
            var t = qx[1]
            qx[1] = qx[3]
            qx[3] = t
            t = qy[1]
            qy[1] = qy[3]
            qy[3] = t
        }
    }

    /** Unit square (0,0),(1,0),(1,1),(0,1) -> corners 0..3 (Heckbert). */
    private fun computeHomography() {
        val sx = qx[0] - qx[1] + qx[2] - qx[3]
        val sy = qy[0] - qy[1] + qy[2] - qy[3]
        val dx1 = qx[1] - qx[2]
        val dx2 = qx[3] - qx[2]
        val dy1 = qy[1] - qy[2]
        val dy2 = qy[3] - qy[2]
        val det = dx1 * dy2 - dx2 * dy1
        if (abs(det) < 1e-6f) {
            hg = 0f
            hh = 0f
        } else {
            hg = (sx * dy2 - dx2 * sy) / det
            hh = (dx1 * sy - sx * dy1) / det
        }
        ha = qx[1] - qx[0] + hg * qx[1]
        hb = qx[3] - qx[0] + hh * qx[3]
        hc = qx[0]
        hd = qy[1] - qy[0] + hg * qy[1]
        he = qy[3] - qy[0] + hh * qy[3]
        hf = qy[0]
    }

    /** Marker coordinates (cells, 0..5) to image; result in [mapX], [mapY]. */
    private fun map(u: Float, v: Float) {
        val uu = u / CookiePattern.Size
        val vv = v / CookiePattern.Size
        val den = hg * uu + hh * vv + 1f
        mapX = (ha * uu + hb * vv + hc) / den
        mapY = (hd * uu + he * vv + hf) / den
    }

    /** Bilinear smoothed luminance at marker coordinates, or -1 outside the frame. */
    private fun lumAt(u: Float, v: Float): Float {
        map(u, v)
        val x = mapX
        val y = mapY
        if (x < 0f || y < 0f || x >= w - 1 || y >= h - 1) return -1f
        val x0 = x.toInt()
        val y0 = y.toInt()
        val fx = x - x0
        val fy = y - y0
        val i = y0 * w + x0
        val top = smooth[i] + (smooth[i + 1] - smooth[i]) * fx
        val bottom = smooth[i + w] + (smooth[i + w + 1] - smooth[i + w]) * fx
        return top + (bottom - top) * fy
    }

    private fun readCells() {
        val size = CookiePattern.Size
        // Ink: central samples of the 16 frame cells.
        var n = 0
        for (r in 0 until size) for (c in 0 until size) {
            if (r != 0 && c != 0 && r != size - 1 && c != size - 1) continue
            for (j in 0..2) for (i in 0..2) {
                val u = c + 0.3f + 0.2f * i
                val v = r + 0.3f + 0.2f * j
                val l = lumAt(u, v)
                if (l >= 0) {
                    su[n] = u
                    sv[n] = v
                    sval[n] = l
                    n++
                }
            }
        }
        if (n < 100 || !fitPlane(n, ink, -1)) return reject(RejectLevels)
        // Paper: a ring just outside the frame.
        n = 0
        for (k in 0 until 12) {
            val t = 0.25f + k * (size - 0.5f) / 11f
            for (depth in 0..1) {
                val o = 0.45f + 0.3f * depth
                n = addPaper(n, t, -o)
                n = addPaper(n, t, size + o)
                n = addPaper(n, -o, t)
                n = addPaper(n, size + o, t)
            }
        }
        if (n < 40) return reject(RejectLevels)
        val paperCount = n
        if (!fitPlane(n, paper, 1)) return reject(RejectLevels)
        val pc = paper[0] + paper[1] * 2.5f + paper[2] * 2.5f
        val ic = ink[0] + ink[1] * 2.5f + ink[2] * 2.5f
        if (pc - ic < maxOf(MinContrast, 0.2f * pc)) return reject(RejectContrast)
        // Quiet zone: the paper ring must be mostly light.
        var light = 0
        for (k in 0 until paperCount) if (sval[k] > mid(su[k], sv[k])) light++
        if (light < 0.7f * paperCount) return reject(RejectQuiet)

        var pattern = 0
        for (r in 0 until size) for (c in 0 until size) {
            var dark = 0
            var valid = 0
            for (j in 0..4) for (i in 0..4) {
                val u = c + 0.2f + 0.15f * i
                val v = r + 0.2f + 0.15f * j
                val l = lumAt(u, v)
                if (l < 0) continue
                valid++
                if (l < mid(u, v)) dark++
            }
            if (valid < 15) return reject(RejectLevels)
            val fill = dark.toFloat() / valid
            val frame = r == 0 || c == 0 || r == size - 1 || c == size - 1
            if (frame) {
                if (fill < FrameFill) return reject(RejectFrame)
            } else if (fill >= CellFill) {
                pattern = pattern or (1 shl ((r - 1) * 3 + c - 1))
            }
        }
        val k = count
        patterns[k] = pattern
        for (i in 0 until 4) {
            corners[k * 8 + i * 2] = qx[i]
            corners[k * 8 + i * 2 + 1] = qy[i]
        }
        count++
    }

    private fun addPaper(n: Int, u: Float, v: Float): Int {
        if (n >= MaxSamples) return n
        val l = lumAt(u, v)
        if (l < 0) return n
        su[n] = u
        sv[n] = v
        sval[n] = l
        return n + 1
    }

    private fun mid(u: Float, v: Float): Float =
        0.5f * (paper[0] + paper[1] * u + paper[2] * v + ink[0] + ink[1] * u + ink[2] * v)

    /**
     * Least-squares plane a + b*u + c*v over the samples, refitted on one side of the first fit:
     * the darker half for ink ([side] -1: pen texture lets paper show through), the lighter half
     * for paper ([side] +1: grid lines and stray ink).
     */
    private fun fitPlane(n: Int, out: FloatArray, side: Int): Boolean {
        if (!solvePlane(n, 0)) return false
        if (!solvePlane(n, side)) return false
        out[0] = plane[0]
        out[1] = plane[1]
        out[2] = plane[2]
        return true
    }

    /** Plane through all samples ([side] 0) or those on one side of the current [plane]. */
    private fun solvePlane(n: Int, side: Int): Boolean {
        var s0 = 0.0
        var s1 = 0.0
        var s2 = 0.0
        var s11 = 0.0
        var s12 = 0.0
        var s22 = 0.0
        var b0 = 0.0
        var b1 = 0.0
        var b2 = 0.0
        val a0 = plane[0]
        val a1 = plane[1]
        val a2 = plane[2]
        for (i in 0 until n) {
            val u = su[i].toDouble()
            val v = sv[i].toDouble()
            val z = sval[i].toDouble()
            if (side != 0 && side * (z - (a0 + a1 * u + a2 * v)) < 0) continue
            s0 += 1.0
            s1 += u
            s2 += v
            s11 += u * u
            s12 += u * v
            s22 += v * v
            b0 += z
            b1 += u * z
            b2 += v * z
        }
        if (s0 < 12) return false
        // Cramer's rule on [[s0 s1 s2][s1 s11 s12][s2 s12 s22]] x = b.
        val det = s0 * (s11 * s22 - s12 * s12) - s1 * (s1 * s22 - s12 * s2) + s2 * (s1 * s12 - s11 * s2)
        if (abs(det) < 1e-9) return false
        val d0 = b0 * (s11 * s22 - s12 * s12) - s1 * (b1 * s22 - s12 * b2) + s2 * (b1 * s12 - s11 * b2)
        val d1 = s0 * (b1 * s22 - b2 * s12) - b0 * (s1 * s22 - s12 * s2) + s2 * (s1 * b2 - b1 * s2)
        val d2 = s0 * (s11 * b2 - s12 * b1) - s1 * (s1 * b2 - b1 * s2) + b0 * (s1 * s12 - s11 * s2)
        plane[0] = (d0 / det).toFloat()
        plane[1] = (d1 / det).toFloat()
        plane[2] = (d2 / det).toFloat()
        return true
    }

    companion object {
        private const val Block = 4
        private const val BgRadius = 3
        const val DarkRatio = 0.7f
        private const val MinDelta = 12
        private const val MinSide = 20
        private const val MaxCandidates = 24
        private const val MaxContour = 8192
        private const val MaxSamples = 256
        private const val MinContrast = 14f
        // Loose hatching is enough: a frame cell needs a third of its centre inked.
        private const val FrameFill = 0.35f
        // One split for pattern cells: codes differ in >= 2 cells, so a misread cell gives no
        // code at all rather than another one; the 3-frame gate absorbs it.
        private const val CellFill = 0.33f

        const val RejectContour = 0
        const val RejectCorners = 1
        const val RejectLines = 2
        const val RejectLevels = 3
        const val RejectContrast = 4
        const val RejectQuiet = 5
        const val RejectFrame = 6
        const val RejectJagged = 7
        const val RejectMoved = 8
        const val RejectOutline = 9
        const val RejectKinds = 10

        private val DX = intArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
        private val DY = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
    }
}
