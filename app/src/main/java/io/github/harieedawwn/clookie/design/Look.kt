package io.github.harieedawwn.clookie.design

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

/** Numerals of [io.github.harieedawwn.clookie.ui.components.TimeDisplay]. */
enum class ClockFont { Display, Ui }

/** Home clock faces. */
enum class ClockStyle { Digital, Cookie, Words, Minimal }

/**
 * The studio's choices on top of a built-in preset. Null fields keep the preset's own value.
 * Stored as a short text code (see [encode]).
 */
@Immutable
data class Look(
    val base: String = Presets.BoSua.id,
    val accent: Color? = null,
    val clockFont: ClockFont = ClockFont.Display,
    val corners: CornerStyle? = null,
    val hardShadow: Boolean? = null,
    val density: UiDensity = UiDensity.Comfortable,
) {
    val isPlain: Boolean get() = this == Look(base)

    fun encode(): String = listOf(
        base,
        accent?.let { "%08X".format(it.toArgb()) } ?: "",
        clockFont.name,
        corners?.name ?: "",
        hardShadow?.toString() ?: "",
        density.name,
    ).joinToString("|")

    companion object {
        fun decode(s: String?, fallbackBase: String? = null): Look {
            if (s.isNullOrEmpty()) return Look(Presets.byId(fallbackBase).id)
            val p = s.split('|')
            fun at(i: Int) = p.getOrElse(i) { "" }
            return Look(
                base = Presets.byId(at(0)).id,
                accent = at(1).takeIf { it.length == 8 }?.toLongOrNull(16)?.let { Color(it.toInt()) },
                clockFont = ClockFont.entries.firstOrNull { it.name == at(2) } ?: ClockFont.Display,
                corners = CornerStyle.entries.firstOrNull { it.name == at(3) },
                hardShadow = at(4).toBooleanStrictOrNull(),
                density = UiDensity.entries.firstOrNull { it.name == at(5) } ?: UiDensity.Comfortable,
            )
        }
    }
}

/** A saved studio look. */
@Immutable
data class CustomPreset(val id: String, val name: String, val look: Look) {
    fun encode(): String = "$id\t${name.replace('\t', ' ').replace('\n', ' ')}\t${look.encode()}"

    companion object {
        fun decodeAll(s: String?): List<CustomPreset> = s.orEmpty().split('\n').mapNotNull { line ->
            val p = line.split('\t')
            if (p.size != 3 || p[0].isEmpty()) null else CustomPreset(p[0], p[1], Look.decode(p[2]))
        }

        fun encodeAll(list: List<CustomPreset>): String = list.joinToString("\n") { it.encode() }
    }
}

/** Accent swatches in the studio. Some fail on some presets on purpose: they get auto-adjusted. */
val AccentSwatches: List<Color> = listOf(
    0xFFC2352A, 0xFFE6A04E, 0xFF44702A, 0xFF71439A, 0xFF1E3A8A, 0xFFEE8A4E,
    0xFFE85D75, 0xFF2A9D8F, 0xFF3B82F6, 0xFFF4D35E, 0xFF8D6E63, 0xFF111111,
).map { Color(it.toInt()) }

/**
 * Makes a chosen accent pass the preset contrast rules: >= 3:1 against background and
 * surface (large numerals, icons) and a text color with >= 4.5:1 on it. The accent is pushed
 * away from the background's lightness until all hold.
 */
fun adjustAccent(accent: Color, colors: ClookieColors): Color {
    val target = if (colors.background.luminance() > 0.4f) Color.Black else Color.White
    fun ok(c: Color) = contrast(c, colors.background) >= LargeContrast && contrast(c, colors.surface) >= LargeContrast &&
        bestOn(c, colors).let { contrast(it, c) >= BodyContrast }
    var t = 0f
    var c = accent
    while (!ok(c) && t < 1f) {
        t = (t + 0.025f).coerceAtMost(1f)
        c = lerp(accent, target, t)
    }
    return c
}

/** Text color on an accent: the preset's own onAccent if it reads, else its ink/background, else black or white. */
fun bestOn(accent: Color, colors: ClookieColors): Color =
    listOf(colors.onAccent, colors.background, colors.ink, Color.White, Color.Black)
        .firstOrNull { contrast(it, accent) >= BodyContrast }
        ?: listOf(Color.White, Color.Black).maxBy { contrast(it, accent) }

/** Applies a [Look] to a (resolved) base preset. */
fun Preset.with(look: Look): Preset {
    val accent = look.accent?.let { adjustAccent(it, colors) }
    val c = if (accent == null) colors else {
        val on = bestOn(accent, colors)
        // Presets whose ringing color is their accent follow the new accent; Giấy ô li keeps its ink.
        val ringFollows = colors.ringing == colors.accent
        colors.copy(
            accent = accent, onAccent = on,
            ringing = if (ringFollows) accent else colors.ringing,
            onRinging = if (ringFollows) on else colors.onRinging,
        )
    }
    return copy(colors = c, corners = look.corners ?: corners, hardShadow = look.hardShadow ?: hardShadow)
}
