package io.github.harieedawwn.clookie.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Font
import java.io.File

/** Bundled fonts must cover all of Vietnamese (precomposed letters with stacked marks). */
class FontCoverageTest {
    private val vietnamese = buildString {
        append("ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚÝàáâãèéêìíòóôõùúýĂăĐđĨĩŨũƠơƯư")
        for (c in 0x1EA0..0x1EF9) append(c.toChar())
        append("Ỗ Ữ Ẫ ặ ỹ Ở 0123456789:")
    }

    @Test
    fun everyBundledFontCoversVietnamese() {
        val dir = File("src/main/res/font")
        val fonts = dir.listFiles { f -> f.extension == "ttf" }.orEmpty()
        assertTrue("no fonts in ${dir.absolutePath}", fonts.size >= 5)
        for (file in fonts) {
            val font = Font.createFont(Font.TRUETYPE_FONT, file)
            assertEquals("${file.name} misses a glyph", -1, font.canDisplayUpTo(vietnamese))
        }
    }
}
