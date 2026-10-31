package io.github.harieedawwn.clookie.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.harieedawwn.clookie.R

val DisplayFamily = FontFamily(Font(R.font.fraunces_display, FontWeight.ExtraBold))

val UiFamily = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
)

/** Never trim: stacked Vietnamese diacritics (Ỗ Ữ Ẫ) sit above the cap height. */
private val NoTrim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun ui(size: TextUnit, line: TextUnit, weight: FontWeight, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = UiFamily, fontSize = size, lineHeight = line, fontWeight = weight,
    letterSpacing = tracking, lineHeightStyle = NoTrim, fontFeatureSettings = "tnum",
)

private fun display(size: TextUnit, line: TextUnit, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = DisplayFamily, fontWeight = FontWeight.ExtraBold, fontSize = size, lineHeight = line,
    letterSpacing = tracking, lineHeightStyle = NoTrim, fontFeatureSettings = "tnum, lnum",
)

/**
 * Type scale. Time numerals are sized in dp by [TimeDisplay] (digits only, so no diacritic
 * clearance needed); every text style keeps line height >= 1.4x for Vietnamese.
 */
@Immutable
data class ClookieType(
    val displayTitle: TextStyle = display(34.sp, 48.sp, (-0.01).em),
    val displaySmall: TextStyle = display(24.sp, 34.sp),
    val title: TextStyle = ui(22.sp, 32.sp, FontWeight.Bold),
    val heading: TextStyle = ui(17.sp, 26.sp, FontWeight.SemiBold),
    val body: TextStyle = ui(15.sp, 23.sp, FontWeight.Normal),
    val bodyStrong: TextStyle = ui(15.sp, 23.sp, FontWeight.SemiBold),
    val label: TextStyle = ui(13.sp, 20.sp, FontWeight.Medium),
    /** Uppercase eyebrow labels. */
    val eyebrow: TextStyle = ui(11.sp, 18.sp, FontWeight.Bold, 0.12.em),
    val caption: TextStyle = ui(11.sp, 17.sp, FontWeight.Medium),
)

/** Numeral sizes in dp: they ignore font scale because they are already huge. */
object TimeSize {
    const val Hero = 124f
    const val Ringing = 136f
    const val Dial = 64f
    const val Slab = 56f
    const val Compact = 34f
}
