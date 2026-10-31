package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Shake and step counting on short synthetic accelerometer traces (50 Hz, m/s², phone flat). */
class MotionCountersTest {
    private class Sample(val tNs: Long, val x: Float, val y: Float, val z: Float)

    private val hz = 50
    private val dtNs = 1_000_000_000L / hz

    /** [accel] gives the acceleration in g at time t (s), gravity included. */
    private fun trace(seconds: Double, seed: Long, noiseG: Double = 0.02, accel: (Double) -> Triple<Double, Double, Double>): List<Sample> {
        val rnd = java.util.Random(seed)
        return (0 until (seconds * hz).toInt()).map { i ->
            val t = i.toDouble() / hz
            val (x, y, z) = accel(t)
            fun n() = rnd.nextGaussian() * noiseG
            Sample(i * dtNs, ((x + n()) * Gravity).toFloat(), ((y + n()) * Gravity).toFloat(), ((z + n()) * Gravity).toFloat())
        }
    }

    private fun shakes(samples: List<Sample>, sensitivity: Int) = ShakeCounter(sensitivity).also { c -> samples.forEach { c.onSample(it.tNs, it.x, it.y, it.z) } }.count
    private fun steps(samples: List<Sample>) = StepCounter().also { c -> samples.forEach { c.onSample(it.tNs, it.x, it.y, it.z) } }.count

    /** Handling in bed: lying still, picked up and turned over, put down hard, a few bumps. */
    private fun bed(seed: Long) = trace(40.0, seed) { t ->
        // Turned over between 8 s and 8.5 s (gravity moves from z to x), back at 25 s.
        val flip = (((t - 8.0) / 0.5).coerceIn(0.0, 1.0) - ((t - 25.0) / 0.5).coerceIn(0.0, 1.0)) * PI / 2
        // Holding it up to read: irregular hand motion (no steady rhythm, unlike walking).
        val hand = if (t in 8.0..14.0) 0.12 * sin(2 * PI * 0.7 * t) + 0.08 * sin(2 * PI * 2.3 * t + 1.0) else 0.0
        // Dropped onto the mattress at 30 s: one hard jolt and a rebound.
        val drop = when {
            t in 30.0..30.04 -> 2.5
            t in 30.1..30.14 -> -1.1
            else -> 0.0
        }
        val bump = if (t in 4.0..4.1 || t in 18.0..18.1 || t in 35.5..35.6) 0.45 else 0.0
        Triple(sin(flip) + hand, 0.0, cos(flip) + drop + bump)
    }

    @Test
    fun shakingCountsEveryJolt() {
        // 4 Hz back and forth, 2.5 g: two jolts per cycle, 5 s = 40.
        val shake = trace(5.0, 1) { t -> Triple(2.5 * sin(2 * PI * 4 * t), 0.0, 1.0) }
        val n = shakes(shake, 2)
        assertTrue("counted $n", n in 36..40)
        // Low sensitivity needs harder shaking.
        assertEquals(0, shakes(trace(5.0, 2) { t -> Triple(2.2 * sin(2 * PI * 4 * t), 0.0, 1.0) }, 1))
        assertTrue(shakes(trace(5.0, 3) { t -> Triple(3.4 * sin(2 * PI * 4 * t), 0.0, 1.0) }, 1) >= 36)
    }

    @Test
    fun handlingInBedIsNotAShake() {
        for (sensitivity in 1..3) for (seed in 10L..14L) assertEquals("sensitivity $sensitivity", 0, shakes(bed(seed), sensitivity))
    }

    @Test
    fun walkingCountsSteps() {
        // 1.8 steps/s, ±0.25 g along gravity, 30 s = 54 steps.
        val walk = trace(30.0, 4, noiseG = 0.03) { t -> Triple(0.0, 0.0, 1.0 + 0.25 * sin(2 * PI * 1.8 * t)) }
        val n = steps(walk)
        assertTrue("counted $n", n in 50..56)
        // Phone in a pocket at an angle: |a| is the same, so are the steps.
        val tilted = trace(30.0, 5, noiseG = 0.03) { t ->
            val m = 1.0 + 0.25 * sin(2 * PI * 1.8 * t)
            Triple(m * 0.6, 0.0, m * 0.8)
        }
        assertTrue(steps(tilted) in 50..56)
    }

    @Test
    fun handlingInBedAndShakingAreNotSteps() {
        for (seed in 20L..24L) assertEquals(0, steps(bed(seed)))
        // Fast shaking: intervals too short.
        assertEquals(0, steps(trace(10.0, 6) { t -> Triple(2.0 * sin(2 * PI * 5 * t), 0.0, 1.0) }))
        // Shaking at walking pace: far too hard for a step.
        assertEquals(0, steps(trace(10.0, 7) { t -> Triple(0.0, 0.0, 1.0 + 2.5 * sin(2 * PI * 1.8 * t)) }))
    }
}
