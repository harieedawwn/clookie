package io.github.harieedawwn.clookie.design

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.harieedawwn.clookie.R

/**
 * Color roles. Every preset fills every role with a solid color; no gradients, no alpha
 * tricks except [scrim]. Contrast rules are enforced by PaletteContrastTest.
 */
@Immutable
data class ClookieColors(
    val background: Color,
    val surface: Color,
    /** Fill of an enabled alarm slab and other "biscuit" blocks. */
    val slab: Color,
    val ink: Color,
    val inkMuted: Color,
    val accent: Color,
    val onAccent: Color,
    /** Secondary fill: chips, selected nav pill, cookie thumb. */
    val accentSoft: Color,
    val onAccentSoft: Color,
    /** 1.5-2 dp strokes. */
    val outline: Color,
    /** Hard offset shadow, zero blur. */
    val shadow: Color,
    /** Chocolate chips on cookie shapes. */
    val chip: Color,
    val ringing: Color,
    val onRinging: Color,
    /** Printed notebook grid (Giấy ô li); Unspecified when the preset has none. */
    val grid: Color = Color.Unspecified,
    val gridMajor: Color = Color.Unspecified,
    val margin: Color = Color.Unspecified,
    val isDark: Boolean = false,
) {
    val scrim: Color get() = if (isDark) Color.Black.copy(alpha = 0.6f) else ink.copy(alpha = 0.45f)
    val hasGrid: Boolean get() = grid != Color.Unspecified
}

enum class CornerStyle { Soft, Rounded, Crisp }

@Immutable
data class Preset(
    val id: String,
    @StringRes val nameRes: Int,
    val colors: ClookieColors,
    val corners: CornerStyle,
    val hardShadow: Boolean,
)

object Presets {
    val BoSua = Preset(
        id = "bo_sua", nameRes = R.string.preset_bo_sua, corners = CornerStyle.Rounded, hardShadow = true,
        colors = ClookieColors(
            background = Color(0xFFF6ECD9), surface = Color(0xFFFFF8EC), slab = Color(0xFFEDDCBB),
            ink = Color(0xFF2B1A10), inkMuted = Color(0xFF6A5140),
            accent = Color(0xFFC2352A), onAccent = Color(0xFFFFF6EA),
            accentSoft = Color(0xFFF2C46D), onAccentSoft = Color(0xFF2B1A10),
            outline = Color(0xFF2B1A10), shadow = Color(0xFF2B1A10), chip = Color(0xFF5A3420),
            ringing = Color(0xFFC2352A), onRinging = Color(0xFFFFF6EA),
        ),
    )
    val Socola = Preset(
        id = "socola", nameRes = R.string.preset_socola, corners = CornerStyle.Soft, hardShadow = true,
        colors = ClookieColors(
            background = Color(0xFF2A1912), surface = Color(0xFF37231A), slab = Color(0xFF4A2F22),
            ink = Color(0xFFF6E7D0), inkMuted = Color(0xFFCDB59A),
            accent = Color(0xFFE6A04E), onAccent = Color(0xFF2A1912),
            accentSoft = Color(0xFFC98A5A), onAccentSoft = Color(0xFF2A1912),
            outline = Color(0xFFA88568), shadow = Color(0xFF110905), chip = Color(0xFF2A1912),
            ringing = Color(0xFFE6A04E), onRinging = Color(0xFF2A1912), isDark = true,
        ),
    )
    val Matcha = Preset(
        id = "matcha", nameRes = R.string.preset_matcha, corners = CornerStyle.Soft, hardShadow = true,
        colors = ClookieColors(
            background = Color(0xFFE4EAD2), surface = Color(0xFFF4F6EA), slab = Color(0xFFCEDBB0),
            ink = Color(0xFF1D2A16), inkMuted = Color(0xFF4A5A3C),
            accent = Color(0xFF44702A), onAccent = Color(0xFFF7F8EF),
            accentSoft = Color(0xFFEBC4A8), onAccentSoft = Color(0xFF1D2A16),
            outline = Color(0xFF1D2A16), shadow = Color(0xFF1D2A16), chip = Color(0xFF2F4A1C),
            ringing = Color(0xFF44702A), onRinging = Color(0xFFF7F8EF),
        ),
    )
    val KhoaiMon = Preset(
        id = "khoai_mon", nameRes = R.string.preset_khoai_mon, corners = CornerStyle.Soft, hardShadow = false,
        colors = ClookieColors(
            background = Color(0xFFEAE0EF), surface = Color(0xFFF8F3FA), slab = Color(0xFFD9C7E5),
            ink = Color(0xFF2A1B33), inkMuted = Color(0xFF5C4868),
            accent = Color(0xFF71439A), onAccent = Color(0xFFF8F3FA),
            accentSoft = Color(0xFFF2D49E), onAccentSoft = Color(0xFF2A1B33),
            outline = Color(0xFF2A1B33), shadow = Color(0xFF2A1B33), chip = Color(0xFF4A2D63),
            ringing = Color(0xFF71439A), onRinging = Color(0xFFF8F3FA),
        ),
    )
    val GiayOLi = Preset(
        id = "giay_o_li", nameRes = R.string.preset_giay_o_li, corners = CornerStyle.Crisp, hardShadow = true,
        colors = ClookieColors(
            background = Color(0xFFFBFBF7), surface = Color(0xFFFFFFFF), slab = Color(0xFFE9EEFA),
            ink = Color(0xFF1E3A8A), inkMuted = Color(0xFF4A5D93),
            accent = Color(0xFFCC2F2A), onAccent = Color(0xFFFFFFFF),
            accentSoft = Color(0xFFD9E3F8), onAccentSoft = Color(0xFF1E3A8A),
            outline = Color(0xFF1E3A8A), shadow = Color(0xFF1E3A8A), chip = Color(0xFF1E3A8A),
            ringing = Color(0xFF1E3A8A), onRinging = Color(0xFFFFFFFF),
            grid = Color(0xFFE6EBF6), gridMajor = Color(0xFFCBD6EE), margin = Color(0xFFEBA3A0),
        ),
    )
    val Dem = Preset(
        id = "dem", nameRes = R.string.preset_dem, corners = CornerStyle.Crisp, hardShadow = false,
        colors = ClookieColors(
            background = Color(0xFF000000), surface = Color(0xFF101010), slab = Color(0xFF1B1B1B),
            ink = Color(0xFFF3EEE6), inkMuted = Color(0xFFA8A39B),
            accent = Color(0xFFEE8A4E), onAccent = Color(0xFF000000),
            accentSoft = Color(0xFF2E2A26), onAccentSoft = Color(0xFFF3EEE6),
            outline = Color(0xFF6E6961), shadow = Color(0xFF000000), chip = Color(0xFFEE8A4E),
            ringing = Color(0xFFEE8A4E), onRinging = Color(0xFF000000), isDark = true,
        ),
    )

    /** Fallback for "Màu hệ thống" before Android 12 and in JVM tests. */
    val SystemFallback = Preset(
        id = "he_thong", nameRes = R.string.preset_he_thong, corners = CornerStyle.Rounded, hardShadow = false,
        colors = ClookieColors(
            background = Color(0xFFF2F0EB), surface = Color(0xFFFFFFFF), slab = Color(0xFFE2DFD8),
            ink = Color(0xFF1C1B19), inkMuted = Color(0xFF57534C),
            accent = Color(0xFF3B57A3), onAccent = Color(0xFFFFFFFF),
            accentSoft = Color(0xFFDAE1F5), onAccentSoft = Color(0xFF1C1B19),
            outline = Color(0xFF1C1B19), shadow = Color(0xFF1C1B19), chip = Color(0xFF1C1B19),
            ringing = Color(0xFF3B57A3), onRinging = Color(0xFFFFFFFF),
        ),
    )

    val all: List<Preset> = listOf(BoSua, Socola, Matcha, KhoaiMon, GiayOLi, Dem, SystemFallback)

    fun byId(id: String?): Preset = all.firstOrNull { it.id == id } ?: BoSua
}
