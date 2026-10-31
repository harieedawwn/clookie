package io.github.harieedawwn.clookie.model

import kotlin.math.abs
import kotlin.math.sqrt

/** Standard gravity in m/s² (accelerometer units). */
const val Gravity = 9.80665f

/**
 * Counts shakes from raw accelerometer samples (m/s², sensor timestamps in ns). A shake is one
 * peak of linear acceleration (gravity removed by a low-pass estimate) above the sensitivity's
 * threshold. Peaks count only in bursts: a lone jolt, like the phone landing on the mattress,
 * never counts; the first peak of a burst counts once a second one follows within the window.
 */
class ShakeCounter(sensitivity: Int) {
    private val t = MissionTuning.Shake
    private val threshold = t.thresholdG[(sensitivity - 1).coerceIn(0, t.thresholdG.lastIndex)]
    var count = 0
        private set
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var lastNs = -1L
    private var armed = true
    private var lastPeakNs = Long.MIN_VALUE / 2
    private var lonePeak = false

    /** Feeds one sample; returns the shakes it added. */
    fun onSample(timeNs: Long, x: Float, y: Float, z: Float): Int {
        if (lastNs < 0) {
            gx = x; gy = y; gz = z
            lastNs = timeNs
            return 0
        }
        val dt = ((timeNs - lastNs) / 1e6f).coerceIn(0f, 200f)
        lastNs = timeNs
        val k = dt / (t.GravityTauMs + dt)
        gx += (x - gx) * k
        gy += (y - gy) * k
        gz += (z - gz) * k
        val lx = x - gx
        val ly = y - gy
        val lz = z - gz
        val lin = sqrt(lx * lx + ly * ly + lz * lz) / Gravity
        if (!armed) {
            if (lin < threshold * t.RearmFraction) armed = true
            return 0
        }
        if (lin < threshold) return 0
        armed = false
        val gapMs = (timeNs - lastPeakNs) / 1_000_000
        if (gapMs < t.MinPeakGapMs) return 0
        lastPeakNs = timeNs
        val added = when {
            gapMs > t.BurstWindowMs -> {
                lonePeak = true
                0
            }
            lonePeak -> {
                lonePeak = false
                2
            }
            else -> 1
        }
        count += added
        return added
    }
}

/**
 * Accelerometer step counter (fallback when there is no step sensor or no activity permission).
 * Works on |a|, so turning the phone over changes nothing. A step is a rise of the smoothed
 * magnitude above 1 g + peak after a dip below 1 g − valley. Steps count only within a run of
 * regular ones (interval 0.28-1.6 s, each within 35 % of the previous): after [MissionTuning.Steps.MinRun]
 * in a row they all count at once. Bumps in bed are irregular, shaking is too fast and too hard.
 */
class StepCounter {
    private val t = MissionTuning.Steps
    var count = 0
        private set
    private var smooth = 1f
    private var lastNs = -1L
    private var dipped = false
    private var strideMax = 0f
    private var lastStepNs = -1L
    private var lastInterval = 0L
    private var run = 0

    /** Feeds one sample; returns the steps it added. */
    fun onSample(timeNs: Long, x: Float, y: Float, z: Float): Int {
        val m = sqrt(x * x + y * y + z * z) / Gravity
        if (lastNs < 0) {
            smooth = m
            lastNs = timeNs
            return 0
        }
        val dt = ((timeNs - lastNs) / 1e6f).coerceIn(0f, 200f)
        lastNs = timeNs
        smooth += (m - smooth) * (dt / (t.SmoothTauMs + dt))
        strideMax = maxOf(strideMax, m)
        if (smooth < 1f - t.ValleyG) dipped = true
        if (!dipped || smooth < 1f + t.PeakG) return 0
        dipped = false
        val tooHard = strideMax - 1f > t.MaxPeakG
        strideMax = 0f
        val interval = if (lastStepNs < 0) -1L else (timeNs - lastStepNs) / 1_000_000
        lastStepNs = timeNs
        if (tooHard) {
            run = 0
            return 0
        }
        val inRange = interval in t.MinIntervalMs..t.MaxIntervalMs
        val steady = run < 2 || abs(interval - lastInterval) <= t.Regularity * lastInterval
        run = if (run > 0 && inRange && steady) run + 1 else 1
        lastInterval = if (inRange) interval else 0L
        val added = when {
            run < t.MinRun -> 0
            run == t.MinRun -> t.MinRun
            else -> 1
        }
        count += added
        return added
    }
}
