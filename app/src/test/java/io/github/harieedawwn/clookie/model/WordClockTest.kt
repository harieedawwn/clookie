package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordClockTest {

    /** Independent reader: Vietnamese number words back to an int (0-99). */
    private fun readNumber(words: List<String>): Int {
        val unit = mapOf(
            "không" to 0, "một" to 1, "mốt" to 1, "hai" to 2, "ba" to 3, "bốn" to 4, "tư" to 4,
            "năm" to 5, "lăm" to 5, "sáu" to 6, "bảy" to 7, "tám" to 8, "chín" to 9,
        )
        return when {
            words.size == 1 && words[0] == "mười" -> 10
            words.size == 1 -> unit.getValue(words[0])
            words[0] == "mười" -> 10 + unit.getValue(words[1])
            words[1] == "mươi" -> unit.getValue(words[0]) * 10 + (if (words.size == 3) unit.getValue(words[2]) else 0)
            else -> error("bad number $words")
        }
    }

    /** Parses a word-clock phrase back to minutes of the day. */
    private fun read(text: String): Int {
        val w = text.split(' ')
        val gio = w.indexOf("giờ")
        val hour = readNumber(w.subList(0, gio))
        val rest = w.subList(gio + 1, w.size).filter { it != "phút" }
        return when {
            rest == listOf("đúng") -> hour * 60
            rest == listOf("rưỡi") -> hour * 60 + 30
            rest.first() == "kém" -> ((hour * 60 - readNumber(rest.drop(1))) + 1440) % 1440
            else -> hour * 60 + readNumber(rest)
        }
    }

    @Test
    fun everyMinuteOfTheDayFollowsTheGrammar() {
        val seen = HashSet<String>()
        for (minuteOfDay in 0 until 1440) {
            val h = minuteOfDay / 60
            val m = minuteOfDay % 60
            val text = WordClock.vi(h, m)
            assertTrue("unique: $text", seen.add(text))
            assertEquals("round trip $h:$m '$text'", minuteOfDay, read(text))
            assertEquals("đúng only at :00 ($text)", m == 0, text.endsWith(" giờ đúng"))
            assertEquals("rưỡi only at :30 ($text)", m == 30, text.endsWith(" giờ rưỡi"))
            assertEquals("kém only at :45-:59 ($text)", m >= 45, " kém " in text)
            if (m >= 45) {
                // Against the next hour, 23:xx counts down to "không giờ".
                assertTrue(text, text.startsWith(WordClock.number((h + 1) % 24) + " giờ kém "))
            } else {
                assertTrue(text, text.startsWith(WordClock.number(h) + " giờ"))
            }
            // Natural number words: never "mươi một", "mười mốt", "mươi năm", "mười năm".
            for (bad in listOf("mươi một", "mười mốt", "mươi năm", "mười năm", "mười tư", "mươi bốn")) {
                assertFalse("'$bad' in '$text'", bad in text)
            }
            assertEquals("phút only after a single-digit minute ($text)", text.endsWith(" phút"), m in 1..9 || m in 51..59)
        }
        assertEquals(1440, seen.size)
    }

    @Test
    fun examples() {
        assertEquals("bảy giờ kém mười lăm", WordClock.vi(6, 45))
        assertEquals("bảy giờ đúng", WordClock.vi(7, 0))
        assertEquals("bảy giờ rưỡi", WordClock.vi(7, 30))
        assertEquals("chín giờ năm phút", WordClock.vi(9, 5))
        assertEquals("mười giờ kém năm phút", WordClock.vi(9, 55))
        assertEquals("hai mươi mốt giờ hai mươi mốt", WordClock.vi(21, 21))
        assertEquals("mười bốn giờ hai mươi tư", WordClock.vi(14, 24))
        assertEquals("mười một giờ ba mươi lăm", WordClock.vi(11, 35))
        assertEquals("không giờ kém mười", WordClock.vi(23, 50))
        assertEquals("không giờ đúng", WordClock.vi(0, 0))
        assertEquals("hai mươi ba giờ mười tám", WordClock.vi(23, 18))
        assertEquals("mười hai giờ bốn mươi tư", WordClock.vi(12, 44))
        assertEquals("mười lăm giờ mười", WordClock.vi(15, 10))
    }
}
