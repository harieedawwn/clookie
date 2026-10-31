package io.github.harieedawwn.clookie.model

import kotlin.random.Random

/** One arithmetic problem with a positive integer answer. */
data class MathProblem(val text: String, val answer: Int) {
    companion object {
        private val t = MissionTuning.Math

        /**
         * Level 1: two-digit addition or subtraction. Level 2: two-digit by one-digit
         * multiplication. Level 3: "a × b + c" or "a × b − c" with two-digit a and c (answers up to ~550).
         */
        fun generate(level: Int, random: Random = Random.Default): MathProblem = when (level) {
            1 -> {
                val a = random.pick(t.addend)
                val b = random.pick(t.addend)
                if (random.nextBoolean() && a != b) MathProblem("${maxOf(a, b)} − ${minOf(a, b)}", maxOf(a, b) - minOf(a, b))
                else MathProblem("$a + $b", a + b)
            }
            2 -> {
                val a = random.pick(t.factor)
                val b = random.pick(t.multiplier)
                MathProblem("$a × $b", a * b)
            }
            else -> {
                val a = random.pick(t.factor)
                val b = random.pick(t.multiplier)
                val c = random.pick(t.offset)
                if (random.nextBoolean() && a * b > c) MathProblem("$a × $b − $c", a * b - c)
                else MathProblem("$a × $b + $c", a * b + c)
            }
        }

        private fun Random.pick(range: IntRange) = nextInt(range.first, range.last + 1)
    }
}
