package io.github.harieedawwn.clookie.design

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

/** WCAG 2.x contrast ratio between two opaque colors. */
fun contrast(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

const val BodyContrast = 4.5
const val LargeContrast = 3.0

/** Pushes [fg] toward black or white (whichever [bg] contrasts with more) until it reaches [min]. */
fun ensureContrast(fg: Color, bg: Color, min: Double): Color {
    if (contrast(fg, bg) >= min) return fg
    val target = if (contrast(Color.Black, bg) >= contrast(Color.White, bg)) Color.Black else Color.White
    var t = 0f
    var c = fg
    while (contrast(c, bg) < min && t < 1f) {
        t += 0.05f
        c = lerp(fg, target, t)
    }
    return c
}

/**
 * "Màu hệ thống": Material You tones flattened to solid Clookie roles, then corrected so the
 * same contrast rules as the hand-made presets always hold.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun systemPreset(context: Context): Preset {
    fun c(id: Int) = Color(context.getColor(id))
    val bg = c(android.R.color.system_neutral1_50)
    val surface = c(android.R.color.system_neutral1_10)
    val slab = c(android.R.color.system_accent2_100)
    val accentRaw = c(android.R.color.system_accent1_600)
    val accent = ensureContrast(ensureContrast(accentRaw, bg, LargeContrast), surface, LargeContrast)
    val onAccent = if (contrast(Color.White, accent) >= contrast(Color.Black, accent)) Color.White else Color.Black
    val ink = listOf(bg, surface, slab).fold(c(android.R.color.system_neutral1_900)) { fg, b -> ensureContrast(fg, b, BodyContrast) }
    val muted = listOf(bg, surface, slab).fold(c(android.R.color.system_neutral2_700)) { fg, b -> ensureContrast(fg, b, BodyContrast) }
    val soft = c(android.R.color.system_accent3_100)
    val base = Presets.SystemFallback
    return base.copy(
        colors = base.colors.copy(
            background = bg, surface = surface, slab = slab, ink = ink, inkMuted = muted,
            accent = accent, onAccent = onAccent,
            accentSoft = soft, onAccentSoft = ensureContrast(ink, soft, BodyContrast),
            outline = ink, shadow = ink, chip = c(android.R.color.system_accent1_800),
            ringing = accent, onRinging = onAccent,
        ),
    )
}
