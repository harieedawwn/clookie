package io.github.harieedawwn.clookie.model

import java.text.Normalizer
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Typing check. Vietnamese keyboards (Telex, VNI) and pasted text may produce composed or
 * decomposed accents, so both sides are NFC-normalized and trimmed before an exact compare.
 */
object TypingCheck {
    fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFC).trim()

    fun matches(typed: String, target: String): Boolean = normalize(typed) == normalize(target)

    /**
     * True while [typed] can still become [target]: compared on base letters, so a word that the
     * keyboard has not accented yet ("chao" on the way to "chào", "d" before "đ") is on track.
     */
    fun onTrack(typed: String, target: String): Boolean = base(target).startsWith(base(typed).trimStart())

    private fun base(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
            .replace('đ', 'd').replace('Đ', 'D')
}

/** Memory: a sequence of lit tiles to repeat. No tile repeats back to back, so every step is visible. */
object MemorySequence {
    fun generate(cells: Int, length: Int, random: Random): List<Int> {
        val out = ArrayList<Int>(length)
        while (out.size < length) {
            val next = random.nextInt(cells)
            if (out.lastOrNull() != next) out += next
        }
        return out
    }
}

/**
 * Crumbs (pure, no Android): crumbs pop up at random spots of a [width] x [height] plate (px)
 * and vanish after the level's life time. A tap on a crumb scores; a crumb that vanishes and a
 * tap on empty plate each cost progress, so random tapping loses. Times are game milliseconds
 * taken from frame time.
 */
class CrumbsGame(
    level: Int,
    val target: Int,
    private val width: Float,
    private val height: Float,
    density: Float,
    seed: Long,
) {
    class Crumb(val x: Float, val y: Float, val bornMs: Long, val shape: Int) {
        var bittenAt = -1L
            internal set
    }

    enum class Tap { Hit, Miss }

    private val t = MissionTuning.Crumbs
    private val i = (level - 1).coerceIn(0, t.lifeMs.lastIndex)
    val lifeMs = t.lifeMs[i]
    val radius = t.radiusDp[i] * density
    private val hit = radius + t.HitSlopDp * density
    private val margin = radius + t.MarginDp * density
    private val maxOnScreen = t.maxOnScreen[i]
    private val random = Random(seed)
    private var nextSpawn = MissionTuning.Crumbs.SpawnGapMinMs

    val crumbs = mutableListOf<Crumb>()
    var score = 0
        private set

    /** Game time of the last progress loss (for a short flash), or -1. */
    var lostAt = -1L
        private set
    val done: Boolean get() = score >= target

    /** Advances to [now]: drops eaten and expired crumbs (expired ones cost progress), spawns new ones. */
    fun update(now: Long) {
        if (done) return
        val iter = crumbs.iterator()
        while (iter.hasNext()) {
            val c = iter.next()
            if (c.bittenAt >= 0) {
                if (now - c.bittenAt > t.BiteMs) iter.remove()
            } else if (now - c.bornMs > lifeMs) {
                iter.remove()
                lose(now)
            }
        }
        val alive = crumbs.count { c -> c.bittenAt < 0 }
        if (alive < maxOnScreen && now >= nextSpawn) {
            spawn(now)
            nextSpawn = now + random.nextLong(t.SpawnGapMinMs, t.SpawnGapMaxMs + 1)
        }
    }

    fun tap(x: Float, y: Float, now: Long): Tap {
        val c = crumbs.filter { it.bittenAt < 0 }.minByOrNull { hypot(it.x - x, it.y - y) }
        if (c != null && hypot(c.x - x, c.y - y) <= hit) {
            c.bittenAt = now
            score++
            return Tap.Hit
        }
        lose(now)
        return Tap.Miss
    }

    private fun lose(now: Long) {
        score = (score - t.MissPenalty).coerceAtLeast(0)
        lostAt = now
    }

    private fun spawn(now: Long) {
        if (width <= 2 * margin || height <= 2 * margin) return
        var best: Pair<Float, Float>? = null
        var bestGap = -1f
        // Eight tries: keep the spot farthest from the crumbs on the plate.
        repeat(8) {
            val x = margin + random.nextFloat() * (width - 2 * margin)
            val y = margin + random.nextFloat() * (height - 2 * margin)
            val gap = crumbs.minOfOrNull { c -> hypot(c.x - x, c.y - y) } ?: Float.MAX_VALUE
            if (gap > bestGap) {
                bestGap = gap
                best = x to y
            }
        }
        best?.let { (x, y) -> crumbs += Crumb(x, y, now, random.nextInt(4)) }
    }
}
