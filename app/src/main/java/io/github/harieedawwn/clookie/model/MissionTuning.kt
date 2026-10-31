package io.github.harieedawwn.clookie.model

/**
 * Every tunable of the Stage 4 missions in one place: counts, sizes (dp), speeds and times (ms)
 * and sensor thresholds (g = 9.81 m/s²). Per-difficulty lists are indexed by level - 1.
 * Games are played half-asleep: targets stay big, timings forgiving, random tapping never wins.
 */
object MissionTuning {
    object Math {
        /** Level 1: a + b or a − b. */
        val addend = 11..89

        /** Levels 2-3: a × b (± c). */
        val factor = 12..49
        val multiplier = 3..9
        val offset = 11..99
        val levels = listOf(1, 2, 3)
        val counts = listOf(1, 3, 5, 10)
        const val DefaultLevel = 2
        const val DefaultCount = 3
        const val MaxDigits = 4
    }

    object Shake {
        val counts = listOf(20, 40, 80, 120)
        const val DefaultCount = 40

        /** Linear-acceleration peak per sensitivity 1 (low) .. 3 (high). Turning over in bed stays under 1 g. */
        val thresholdG = listOf(2.6f, 2.0f, 1.5f)
        const val DefaultSensitivity = 2

        /** Gravity estimate: low-pass time constant. */
        const val GravityTauMs = 500f

        /** A peak re-arms once the signal falls below this share of the threshold. */
        const val RearmFraction = 0.5f
        const val MinPeakGapMs = 70L

        /** A lone peak (phone dropped on the mattress) counts only if another follows this soon. */
        const val BurstWindowMs = 600L

        /** Cookie jolt per shake on screen. */
        const val JoltMs = 220L
        const val JoltDp = 14f
    }

    object Steps {
        val counts = listOf(20, 40, 80, 150)
        const val DefaultCount = 40

        /** Accelerometer fallback: smoothing of |a|, step peak and valley around 1 g. */
        const val SmoothTauMs = 60f
        const val PeakG = 0.12f
        const val ValleyG = 0.05f
        const val MinIntervalMs = 280L
        const val MaxIntervalMs = 1600L

        /** Consecutive intervals may differ by this share at most (walking is regular). */
        const val Regularity = 0.35f

        /** Steps count only after this many regular ones in a row (then all of them count). */
        const val MinRun = 5

        /** Harder than this (raw |a| − 1 g) is shaking, not walking: the run restarts. */
        const val MaxPeakG = 1.3f
    }

    object Typing {
        val phraseCounts = listOf(1, 2, 3)
        const val DefaultCount = 1

        /** 1 = short built-in phrases, 2 = long ones. */
        val lengths = listOf(1, 2)
        const val DefaultLength = 1
        const val MaxCustomLength = 80
    }

    object Memory {
        val grid = listOf(3, 3, 4)
        val sequence = listOf(4, 5, 6)
        val litMs = listOf(620L, 500L, 420L)
        val gapMs = listOf(220L, 180L, 150L)
        val rounds = listOf(1, 2, 3)
        const val DefaultLevel = 2
        const val DefaultRounds = 2
        const val StartDelayMs = 600L

        /** Flash of a tapped tile, and the pause before a failed round replays (with a new sequence). */
        const val TapFlashMs = 180L
        const val WrongPauseMs = 800L
        const val TileGapDp = 10f
        const val MaxSideDp = 340f
    }

    object Crumbs {
        val counts = listOf(10, 15, 25)
        const val DefaultCount = 15
        const val DefaultLevel = 2

        /** How long a crumb stays, its radius, and how many may be up at once, per level. */
        val lifeMs = listOf(1700L, 1300L, 1000L)
        val radiusDp = listOf(34f, 30f, 27f)
        val maxOnScreen = listOf(1, 2, 2)

        /** Extra touch radius around a crumb (target >= 48 dp across in every level). */
        const val HitSlopDp = 14f
        const val SpawnGapMinMs = 220L
        const val SpawnGapMaxMs = 520L
        const val MarginDp = 8f

        /** A crumb that vanishes untapped and a tap on empty plate each cost this much progress. */
        const val MissPenalty = 1
        const val PopMs = 140L
        const val BiteMs = 240L
        const val MissFlashMs = 260L
        const val MaxHeightDp = 380f
    }
}
