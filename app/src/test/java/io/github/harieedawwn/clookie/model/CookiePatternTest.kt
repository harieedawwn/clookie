package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CookiePatternTest {
    @Test
    fun fourQuarterTurnsAreIdentity() {
        for (p in 0..0x1FF) {
            var q = p
            repeat(4) { q = CookiePattern.rotate(q) }
            assertEquals(p, q)
        }
    }

    @Test
    fun rotateMovesTopLeftToTopRight() {
        // (0,0) -> (0,2) when turned clockwise.
        assertEquals(1 shl 2, CookiePattern.rotate(1 shl 0))
    }

    @Test
    fun generatedCodesFollowTheRules() {
        val rnd = Random(3)
        val codes = mutableListOf<Int>()
        while (true) {
            val p = CookiePattern.generate(codes, rnd) ?: break
            assertTrue(Integer.bitCount(p) in 3..6)
            assertFalse("symmetric $p", CookiePattern.isSymmetric(p))
            codes.forEach { other -> assertTrue("$p vs $other", CookiePattern.distance(p, other) >= 2) }
            codes += p
        }
        // Plenty of codes before the space runs out, and then generation says so.
        assertTrue("only ${codes.size} codes", codes.size >= 20)
        assertNull(CookiePattern.generate(codes, rnd))
    }

    @Test
    fun matchesInAnyRotationButNotMirrored() {
        val p = 0b000_011_110 // an S shape, chiral
        var q = p
        repeat(4) {
            assertTrue(CookiePattern.matches(q, p))
            q = CookiePattern.rotate(q)
        }
        val mirrored = (0..8).fold(0) { acc, i ->
            val r = i / 3
            val c = i % 3
            if (CookiePattern.isSet(p, r, c)) acc or (1 shl (r * 3 + 2 - c)) else acc
        }
        assertFalse(CookiePattern.matches(mirrored, p))
    }

    @Test
    fun guideOrderDrawsFrameClockwiseThenPattern() {
        val frame = CookiePattern.frameOrder()
        assertEquals(16, frame.size)
        assertEquals(listOf(0, 1, 2, 3, 4, 9, 14, 19, 24), frame.take(9))
        assertEquals(listOf(7, 11, 13), CookiePattern.patternOrder(0b000_101_010))
    }

    @Test
    fun gateNeedsThreeConsecutiveFramesOfTheSameCode() {
        val g = ScanGate(3)
        assertFalse(g.feed(5))
        assertFalse(g.feed(5))
        assertFalse(g.feed(ScanGate.NoMatch))
        assertFalse(g.feed(5))
        assertFalse(g.feed(6))
        assertFalse(g.feed(6))
        assertTrue(g.feed(6))
    }
}
