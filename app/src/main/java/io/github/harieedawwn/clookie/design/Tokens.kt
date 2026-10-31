package io.github.harieedawwn.clookie.design

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/** 4 dp spacing grid. Use these, never raw dp, for padding and gaps. */
object Space {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp
    val s10 = 40.dp
    val s12 = 48.dp
    val s16 = 64.dp

    /** Screen side gutter. */
    val gutter = 20.dp
}

object Stroke {
    val thin = 1.5.dp
    val regular = 2.dp
}

/** Minimum touch target. */
val TouchTarget = 48.dp

@Immutable
data class ClookieShapes(
    val small: RoundedCornerShape,
    val medium: RoundedCornerShape,
    val large: RoundedCornerShape,
    val smallRadius: Dp,
    val mediumRadius: Dp,
    val largeRadius: Dp,
) {
    companion object {
        fun of(style: CornerStyle): ClookieShapes {
            val (s, m, l) = when (style) {
                CornerStyle.Soft -> Triple(14.dp, 24.dp, 36.dp)
                CornerStyle.Rounded -> Triple(10.dp, 16.dp, 26.dp)
                CornerStyle.Crisp -> Triple(3.dp, 5.dp, 8.dp)
            }
            return ClookieShapes(RoundedCornerShape(s), RoundedCornerShape(m), RoundedCornerShape(l), s, m, l)
        }
    }
}

enum class UiDensity(val scale: Float) { Comfortable(1f), Compact(0.85f) }

enum class MotionLevel { Full, Reduced }

/**
 * Motion specs: springs, 150-350 ms feel. With [MotionLevel.Reduced] (user setting or system
 * animator scale 0) every spec snaps.
 */
@Immutable
data class ClookieMotion(val level: MotionLevel = MotionLevel.Full) {
    val reduced: Boolean get() = level == MotionLevel.Reduced

    /** Presses, toggles, small moves (~150 ms). */
    fun <T> snappy(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow * 2.5f)

    /** Sheets, screen elements (~250 ms). */
    fun <T> standard(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.86f, stiffness = Spring.StiffnessMediumLow)

    /** Playful settle with a little overshoot (~350 ms): thumbs, bite. */
    fun <T> playful(): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)

    fun offsetSpec(): FiniteAnimationSpec<IntOffset> = standard()
}

@Immutable
data class ClookieStyle(
    val hardShadow: Boolean,
    val shadowOffset: Dp = 4.dp,
    val density: UiDensity = UiDensity.Comfortable,
)
