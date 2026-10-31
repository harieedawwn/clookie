package io.github.harieedawwn.clookie.design

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every preset: body text >= 4.5:1, large numerals and icons >= 3:1 (WCAG 2.x). */
class PaletteContrastTest {

    private fun check(failures: MutableList<String>, preset: Preset, what: String, fg: Color, bg: Color, min: Double) {
        val ratio = contrast(fg, bg)
        if (ratio < min) failures += "${preset.id}: $what = ${"%.2f".format(ratio)} < $min"
    }

    @Test
    fun allPresetsMeetContrast() {
        val failures = mutableListOf<String>()
        for (p in Presets.all) {
            val c = p.colors
            // Body text.
            for ((name, bg) in listOf("background" to c.background, "surface" to c.surface, "slab" to c.slab)) {
                check(failures, p, "ink on $name", c.ink, bg, BodyContrast)
                check(failures, p, "inkMuted on $name", c.inkMuted, bg, BodyContrast)
            }
            check(failures, p, "onAccent on accent", c.onAccent, c.accent, BodyContrast)
            check(failures, p, "onAccentSoft on accentSoft", c.onAccentSoft, c.accentSoft, BodyContrast)
            check(failures, p, "onRinging on ringing", c.onRinging, c.ringing, BodyContrast)
            check(failures, p, "background on ink (nav bar)", c.background, c.ink, BodyContrast)
            // Large numerals, icons, strokes.
            check(failures, p, "accent on background", c.accent, c.background, LargeContrast)
            check(failures, p, "accent on surface", c.accent, c.surface, LargeContrast)
            check(failures, p, "outline on background", c.outline, c.background, LargeContrast)
            check(failures, p, "outline on surface", c.outline, c.surface, LargeContrast)
            check(failures, p, "chip on accentSoft (dial cookie)", c.chip, c.accentSoft, LargeContrast)
            check(failures, p, "onAccentSoft on accentSoft icon", c.onAccentSoft, c.accentSoft, LargeContrast)
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun ensureContrastRepairsWeakPairs() {
        val bg = Color(0xFFEDE7F6)
        val fixed = ensureContrast(Color(0xFFB39DDB), bg, BodyContrast)
        assertTrue(contrast(fixed, bg) >= BodyContrast)
    }

    @Test
    fun atLeastSevenPresetsWithUniqueIds() {
        assertTrue(Presets.all.size >= 7)
        assertTrue(Presets.all.map { it.id }.toSet().size == Presets.all.size)
    }
}
