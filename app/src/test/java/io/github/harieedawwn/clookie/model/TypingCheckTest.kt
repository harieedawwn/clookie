package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.Normalizer

class TypingCheckTest {
    private val target = "Chào buổi sáng"

    @Test
    fun composedAndDecomposedAccentsMatch() {
        // Telex/VNI keyboards and "Unicode tổ hợp" may send combining marks.
        val decomposed = Normalizer.normalize(target, Normalizer.Form.NFD)
        assertTrue(decomposed != target)
        assertTrue(TypingCheck.matches(decomposed, target))
        assertTrue(TypingCheck.matches("  $target ", target))
    }

    @Test
    fun wrongToneOrLetterDoesNotMatch() {
        assertFalse(TypingCheck.matches("Chao buổi sáng", target))
        assertFalse(TypingCheck.matches("Chào buổi sắng", target))
        assertFalse(TypingCheck.matches("chào buổi sáng", target))
        assertFalse(TypingCheck.matches("Đường", "Dường"))
    }

    @Test
    fun unaccentedWordOnItsWayIsOnTrack() {
        assertTrue(TypingCheck.onTrack("Chao", target))
        assertTrue(TypingCheck.onTrack("Chào buoi", target))
        assertTrue(TypingCheck.onTrack("D", "Đi thôi"))
        assertFalse(TypingCheck.onTrack("Chaof bx", target))
    }
}
