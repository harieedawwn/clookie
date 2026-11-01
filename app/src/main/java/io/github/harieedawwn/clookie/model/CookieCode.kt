package io.github.harieedawwn.clookie.model

import kotlin.random.Random

/**
 * Cookie Code patterns: the inner 3x3 of a 5x5 marker whose outer ring is solid ink.
 * A pattern is a 9-bit mask, bit `row * 3 + col` (row 0 = top, as drawn on paper).
 */
object CookiePattern {
    const val Size = 5
    const val MinCells = 3
    const val MaxCells = 6

    /** Codes must differ in at least this many cells under every rotation. */
    const val MinDistance = 2

    fun isSet(pattern: Int, row: Int, col: Int) = pattern and (1 shl (row * 3 + col)) != 0

    /** Rotate 90° clockwise: the new cell (r, c) is the old cell (2 - c, r). */
    fun rotate(pattern: Int): Int {
        var out = 0
        for (r in 0..2) for (c in 0..2) if (isSet(pattern, 2 - c, r)) out = out or (1 shl (r * 3 + c))
        return out
    }

    /** True when a half turn (and so any turn) maps the pattern onto itself. */
    fun isSymmetric(pattern: Int) = rotate(rotate(pattern)) == pattern

    /** Smallest number of differing cells between [a] and [b] over the four rotations of [a]. */
    fun distance(a: Int, b: Int): Int {
        var p = a
        var best = 9
        repeat(4) {
            best = minOf(best, Integer.bitCount(p xor b))
            p = rotate(p)
        }
        return best
    }

    /** [raw] read in any orientation is [target]. */
    fun matches(raw: Int, target: Int): Boolean {
        var p = raw
        repeat(4) {
            if (p == target) return true
            p = rotate(p)
        }
        return false
    }

    fun isValid(pattern: Int, others: Collection<Int> = emptyList()): Boolean =
        pattern in 0..0x1FF &&
            Integer.bitCount(pattern) in MinCells..MaxCells &&
            !isSymmetric(pattern) &&
            others.all { distance(pattern, it) >= MinDistance }

    /** All valid patterns given the user's [others]; empty when the code space is used up. */
    fun candidates(others: Collection<Int>): List<Int> = (0..0x1FF).filter { isValid(it, others) }

    /** A random valid pattern with as few cells as the user's [others] allow (less to draw). */
    fun generate(others: Collection<Int>, random: Random = Random.Default): Int? {
        val all = candidates(others)
        val fewest = all.minOfOrNull { Integer.bitCount(it) } ?: return null
        return all.filter { Integer.bitCount(it) == fewest }.random(random)
    }

    /** Marker cell (row, col) in 0..4 is ink: the ring, or a set pattern cell. */
    fun markerCell(pattern: Int, row: Int, col: Int): Boolean =
        row == 0 || col == 0 || row == Size - 1 || col == Size - 1 || isSet(pattern, row - 1, col - 1)

    /**
     * Drawing order for the guide: the ring clockwise from the top-left corner, then the
     * pattern cells in reading order. Each entry is `row * 5 + col`.
     */
    fun frameOrder(): List<Int> = buildList {
        for (c in 0 until Size) add(c)
        for (r in 1 until Size) add(r * Size + Size - 1)
        for (c in Size - 2 downTo 0) add((Size - 1) * Size + c)
        for (r in Size - 2 downTo 1) add(r * Size)
    }

    fun patternOrder(pattern: Int): List<Int> = buildList {
        for (r in 0..2) for (c in 0..2) if (isSet(pattern, r, c)) add((r + 1) * Size + c + 1)
    }
}

/** A saved Cookie Code. */
data class CookieCode(val id: Long, val name: String, val pattern: Int)

/** A registered QR code or barcode (ZXing format name + decoded text). */
data class ScanCode(val id: Long, val name: String, val format: String, val value: String)

/**
 * Accepts a code once [required] frames agree on it with at most [maxGap] blank frames in a
 * row between them (a shaky hand or a blurred frame does not restart). Another code restarts
 * the count. Allocation-free.
 */
class ScanGate(private val required: Int = 3, private val maxGap: Int = 2) {
    var streak = 0
        private set
    private var current = NoMatch
    private var gap = 0

    /** Feed one frame's matched code id (or [NoMatch]); returns true once accepted. */
    fun feed(id: Long): Boolean {
        if (id == NoMatch) {
            if (++gap > maxGap) reset()
            return false
        }
        gap = 0
        streak = if (id == current) streak + 1 else 1
        current = id
        return streak >= required
    }

    fun reset() {
        streak = 0
        current = NoMatch
        gap = 0
    }

    companion object {
        const val NoMatch = -1L
    }
}
