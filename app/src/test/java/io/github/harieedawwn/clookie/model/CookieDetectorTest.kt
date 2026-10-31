package io.github.harieedawwn.clookie.model

import io.github.harieedawwn.clookie.model.SyntheticCamera.FrameH
import io.github.harieedawwn.clookie.model.SyntheticCamera.FrameW
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.random.Random

/**
 * Detection quality on ~300 synthetic scenes built from the notebook photo in ref/
 * (see [SyntheticCamera]): positives in blue and black ink, negatives (plain paper, scribbles,
 * handwriting, other patterns, the user's other codes). Three frames per scene, like the
 * scanner's 3-frame acceptance. Writes build/reports/cookie-detector.txt and sample frames.
 */
class CookieDetectorTest {
    private class Frames(val label: String, val target: Int, val drawn: Int?, val frames: List<ByteArray>)

    private val out = File("build/reports/cookie-detector").apply { deleteRecursively(); mkdirs() }

    private fun positives(): List<Frames> {
        val rnd = Random(2026_11_01)
        return List(PositiveScenes) { i ->
            val ink = if (i % 2 == 0) SyntheticCamera.Ink.Blue else SyntheticCamera.Ink.Black
            val pattern = CookiePattern.generate(emptyList(), rnd)!!
            val grid = SyntheticCamera.randomGrid(rnd)
            val layer = SyntheticCamera.inkLayer(grid, 1.0, ink, rnd)
            SyntheticCamera.drawMarker(layer, grid, pattern, rnd)
            val scene = SyntheticCamera.scene(grid, layer, rnd)
            Frames("pos-$i-${ink.name.lowercase()}", pattern, pattern, List(3) { SyntheticCamera.render(scene, SyntheticCamera.frameCorners(scene, rnd), rnd) })
        }
    }

    private fun negatives(): List<Frames> {
        val rnd = Random(1_000_003)
        return List(NegativeScenes) { i ->
            val target = CookiePattern.generate(emptyList(), rnd)!!
            val ink = if (rnd.nextBoolean()) SyntheticCamera.Ink.Blue else SyntheticCamera.Ink.Black
            val grid = SyntheticCamera.randomGrid(rnd)
            val layer = SyntheticCamera.inkLayer(grid, 1.0, ink, rnd)
            var drawn: Int? = null
            val kind = when (i % 5) {
                0 -> "paper"
                1 -> "scribble".also { SyntheticCamera.scribbles(layer, grid, rnd) }
                2 -> "handwriting".also { SyntheticCamera.handwriting(layer, grid, rnd) }
                3 -> "pattern".also {
                    when (rnd.nextInt(5)) {
                        0 -> SyntheticCamera.drawMarker(layer, grid, 0x1FF, rnd)
                        1 -> SyntheticCamera.drawMarker(layer, grid, 0, rnd)
                        2 -> SyntheticCamera.drawFramedGrid(layer, grid, listOf(4, 6, 7).random(rnd), rnd)
                        3 -> SyntheticCamera.checkerboard(layer, grid, listOf(4, 5).random(rnd), rnd)
                        else -> {
                            // A 3x3 pattern without the frame.
                            val p = CookiePattern.generate(emptyList(), rnd)!!
                            for (r in 0..2) for (c in 0..2) if (CookiePattern.isSet(p, r, c)) {
                                SyntheticCamera.fillCell(layer, grid, r + 1, c + 1, rnd, rnd.nextDouble() * 3, 0.85)
                            }
                        }
                    }
                }
                else -> "othercode".also {
                    // One of the user's other codes: valid and at least 2 cells away from the target.
                    val p = CookiePattern.generate(listOf(target), rnd)!!
                    drawn = p
                    SyntheticCamera.drawMarker(layer, grid, p, rnd)
                }
            }
            val scene = SyntheticCamera.scene(grid, if (kind == "paper") null else layer, rnd)
            Frames("neg-$i-$kind", target, drawn, List(3) { SyntheticCamera.render(scene, SyntheticCamera.frameCorners(scene, rnd), rnd) })
        }
    }

    private fun CookieDetector.matches(target: Int): Boolean =
        (0 until count).any { CookiePattern.matches(patterns[it], target) }

    @Test
    fun syntheticSetMeetsTargets() {
        val detector = CookieDetector()
        val pos = positives()
        val neg = negatives()

        var sceneHits = 0
        var frameHits = 0
        var accepted = 0
        val misses = mutableListOf<String>()
        val missWhy = mutableListOf<String>()
        val rejects = IntArray(CookieDetector.RejectKinds)
        val byInk = mutableMapOf<String, IntArray>()
        for (s in pos) {
            var hits = 0
            val gate = ScanGate(3)
            var ok = false
            for (f in s.frames) {
                detector.detect(f, FrameW, FrameH)
                for (k in rejects.indices) rejects[k] += detector.rejects[k]
                val hit = detector.matches(s.target)
                if (hit) hits++
                if (gate.feed(if (hit) 1L else ScanGate.NoMatch)) ok = true
            }
            frameHits += hits
            if (hits > 0) sceneHits++ else {
                misses += s.label
                missWhy += "${s.label}: ${detector.rejects.joinToString(",")}"
            }
            if (ok) accepted++
            val inkKey = s.label.substringAfterLast('-')
            byInk.getOrPut(inkKey) { IntArray(2) }.let { it[0]++; if (hits > 0) it[1]++ }
        }
        misses.take(30).forEach { m -> pos.first { it.label == m }.let { SyntheticCamera.save(it.frames[0], File(out, "miss-$m.png")) } }
        pos.take(6).forEach { SyntheticCamera.save(it.frames[0], File(out, "${it.label}.png")) }
        neg.take(10).forEach { SyntheticCamera.save(it.frames[0], File(out, "${it.label}.png")) }

        var falseFrames = 0
        var falseAccepts = 0
        val falseLabels = mutableListOf<String>()
        for (s in neg) {
            val gate = ScanGate(3)
            for (f in s.frames) {
                detector.detect(f, FrameW, FrameH)
                val hit = detector.matches(s.target)
                if (hit) {
                    falseFrames++
                    falseLabels += s.label
                }
                if (gate.feed(if (hit) 1L else ScanGate.NoMatch)) falseAccepts++
            }
        }

        // Per-frame time at analysis resolution (after warm-up).
        val all = pos.flatMap { it.frames } + neg.flatMap { it.frames }
        repeat(2) { all.take(60).forEach { detector.detect(it, FrameW, FrameH) } }
        val times = all.map { f ->
            val t0 = System.nanoTime()
            detector.detect(f, FrameW, FrameH)
            (System.nanoTime() - t0) / 1e6
        }.sorted()
        val mean = times.average()
        val p95 = times[(times.size * 0.95).toInt()]

        val report = buildString {
            appendLine("Cookie Code detector, synthetic set from ref/notebook-paper.jpg, ${FrameW}x$FrameH frames")
            appendLine("positives: ${pos.size} scenes x 3 frames; detected within 3 frames: $sceneHits/${pos.size} = ${"%.1f".format(100.0 * sceneHits / pos.size)}%")
            byInk.forEach { (k, v) -> appendLine("  $k ink: ${v[1]}/${v[0]}") }
            appendLine("  per frame: $frameHits/${pos.size * 3} = ${"%.1f".format(100.0 * frameHits / (pos.size * 3))}%; accepted by the 3-consecutive gate: $accepted/${pos.size}")
            appendLine("  rejects (contour, corners, lines, levels, contrast, quiet, frame, jagged, moved, outline): ${rejects.joinToString()}")
            missWhy.forEach { appendLine("  missed $it") }
            appendLine("negatives: ${neg.size} scenes x 3 frames; false single-frame matches: $falseFrames; false accepts: $falseAccepts ${falseLabels.distinct()}")
            appendLine("time per frame: mean ${"%.2f".format(mean)} ms, p95 ${"%.2f".format(p95)} ms (JVM, this PC)")
        }
        File(out.parentFile, "cookie-detector.txt").writeText(report)
        println(report)

        assertEquals("false matches on negatives", 0, falseFrames)
        assertEquals("false accepts on negatives", 0, falseAccepts)
        assertTrue("detected within 3 frames: $sceneHits/${pos.size}", sceneHits >= 0.95 * pos.size)
    }

    @Test
    fun realPhotoOfBlankPaperIsNegative() {
        val p = SyntheticCamera.photo
        val frame = ByteArray(FrameW * FrameH)
        // Downscale the whole photo (portrait) into the landscape frame like a rotated sensor.
        for (y in 0 until FrameH) for (x in 0 until FrameW) {
            val sx = (y + 0.5) / FrameH * p.w
            val sy = (1 - (x + 0.5) / FrameW) * p.h
            frame[y * FrameW + x] = p.mirror(sx, sy).toInt().coerceIn(0, 255).toByte()
        }
        assertEquals(0, CookieDetector().detect(frame, FrameW, FrameH))
    }

    @Test
    fun detectAllocatesNothingPerFrame() {
        val rnd = Random(7)
        val pattern = CookiePattern.generate(emptyList(), rnd)!!
        val grid = SyntheticCamera.randomGrid(rnd)
        val layer = SyntheticCamera.inkLayer(grid, 1.0, SyntheticCamera.Ink.Blue, rnd)
        SyntheticCamera.drawMarker(layer, grid, pattern, rnd)
        val scene = SyntheticCamera.scene(grid, layer, rnd, minSize = 160.0, maxSize = 200.0)
        val frame = SyntheticCamera.render(scene, SyntheticCamera.frameCorners(scene, rnd), rnd)
        val detector = CookieDetector()
        repeat(5) { detector.detect(frame, FrameW, FrameH) }
        val bean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        val id = Thread.currentThread().id
        val before = bean.getThreadAllocatedBytes(id)
        repeat(50) { detector.detect(frame, FrameW, FrameH) }
        val after = bean.getThreadAllocatedBytes(id)
        val perFrame = (after - before) / 50
        println("allocated per frame: $perFrame bytes")
        assertTrue("allocated $perFrame bytes per frame", perFrame < 64)
    }

    companion object {
        const val PositiveScenes = 200
        const val NegativeScenes = 100
    }
}
