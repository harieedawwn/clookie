package io.github.harieedawwn.clookie.model

/** Repeating waveform: alternating off/on durations (ms) and amplitudes 0-255; repeats from index 0. */
class Waveform(val timings: LongArray, val amplitudes: IntArray)

val VibrationPattern.waveform: Waveform?
    get() = when (this) {
        VibrationPattern.Off -> null
        // lub-dub, rest
        VibrationPattern.Heartbeat -> Waveform(longArrayOf(0, 120, 90, 160, 700), intArrayOf(0, 255, 0, 200, 0))
        VibrationPattern.Steady -> Waveform(longArrayOf(0, 600, 400), intArrayOf(0, 255, 0))
        // three pulses that grow, then a long one
        // Full strength nearly without pause (Báo động đỏ).
        VibrationPattern.Max -> Waveform(longArrayOf(0, 1400, 120), intArrayOf(0, 255, 0))
        VibrationPattern.Crescendo -> Waveform(
            longArrayOf(0, 150, 150, 200, 150, 280, 150, 800, 500),
            intArrayOf(0, 90, 0, 150, 0, 210, 0, 255, 0),
        )
    }
