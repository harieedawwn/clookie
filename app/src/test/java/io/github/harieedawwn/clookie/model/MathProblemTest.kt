package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathProblemTest {
    private val t = MissionTuning.Math

    @Test
    fun answersMatchTheTextAndStayPositive() {
        val rnd = Random(11)
        for (level in 1..3) repeat(500) {
            val p = MathProblem.generate(level, rnd)
            assertEquals(p.text, eval(p.text), p.answer)
            assertTrue(p.answer > 0)
            assertTrue(p.text, p.answer.toString().length <= t.MaxDigits)
        }
    }

    @Test
    fun operandsStayWithinEachLevel() {
        val rnd = Random(5)
        val ops = mutableMapOf<Int, MutableSet<String>>()
        for (level in 1..3) repeat(2000) {
            val p = MathProblem.generate(level, rnd)
            val n = p.text.split(' ').filterIndexed { i, _ -> i % 2 == 0 }.map(String::toInt)
            ops.getOrPut(level) { mutableSetOf() } += p.text.split(' ').filterIndexed { i, _ -> i % 2 == 1 }
            when (level) {
                1 -> {
                    assertEquals(2, n.size)
                    assertTrue(p.text, n.all { it in t.addend })
                    assertTrue(p.text, p.answer in 1..2 * t.addend.last)
                }
                2 -> {
                    assertEquals(2, n.size)
                    assertTrue(p.text, n[0] in t.factor && n[1] in t.multiplier)
                    assertTrue(p.text, p.answer in t.factor.first * t.multiplier.first..t.factor.last * t.multiplier.last)
                }
                else -> {
                    assertEquals(3, n.size)
                    assertTrue(p.text, n[0] in t.factor && n[1] in t.multiplier && n[2] in t.offset)
                    assertTrue(p.text, p.answer in 1..t.factor.last * t.multiplier.last + t.offset.last)
                }
            }
        }
        // Each level uses its own operations, and the easy level mixes + and −.
        assertEquals(setOf("+", "−"), ops[1])
        assertEquals(setOf("×"), ops[2])
        assertEquals(setOf("×", "+", "−"), ops[3])
    }

    private fun eval(text: String): Int {
        val t = text.split(' ')
        var v = when (t[1]) {
            "×" -> t[0].toInt() * t[2].toInt()
            "−" -> t[0].toInt() - t[2].toInt()
            else -> t[0].toInt() + t[2].toInt()
        }
        if (t.size == 5) v = if (t[3] == "+") v + t[4].toInt() else v - t[4].toInt()
        return v
    }
}
