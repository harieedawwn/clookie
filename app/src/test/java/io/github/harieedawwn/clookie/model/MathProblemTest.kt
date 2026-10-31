package io.github.harieedawwn.clookie.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathProblemTest {
    @Test
    fun answersMatchTheTextAndStayPositive() {
        val rnd = Random(11)
        for (level in 1..3) repeat(500) {
            val p = MathProblem.generate(level, rnd)
            assertEquals(p.text, eval(p.text), p.answer)
            assertTrue(p.answer > 0)
        }
    }

    private fun eval(text: String): Int {
        val t = text.split(' ')
        var v = if (t.size >= 3 && t[1] == "×") t[0].toInt() * t[2].toInt() else t[0].toInt() + t[2].toInt()
        if (t.size == 5) v = if (t[3] == "+") v + t[4].toInt() else v - t[4].toInt()
        return v
    }
}
