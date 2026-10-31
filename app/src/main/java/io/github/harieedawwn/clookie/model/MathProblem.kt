package io.github.harieedawwn.clookie.model

import kotlin.random.Random

/** One arithmetic problem with a non-negative integer answer. */
data class MathProblem(val text: String, val answer: Int) {
    companion object {
        /**
         * Level 1: two-digit addition. Level 2: two-digit by one-digit multiplication.
         * Level 3: "a × b + c" or "a × b − c" with two-digit a and c (answers up to ~550).
         */
        fun generate(level: Int, random: Random = Random.Default): MathProblem = when (level) {
            1 -> {
                val a = random.nextInt(12, 90)
                val b = random.nextInt(12, 90)
                MathProblem("$a + $b", a + b)
            }
            2 -> {
                val a = random.nextInt(12, 50)
                val b = random.nextInt(3, 10)
                MathProblem("$a × $b", a * b)
            }
            else -> {
                val a = random.nextInt(12, 50)
                val b = random.nextInt(3, 10)
                val c = random.nextInt(11, 100)
                if (random.nextBoolean() && a * b > c) MathProblem("$a × $b − $c", a * b - c)
                else MathProblem("$a × $b + $c", a * b + c)
            }
        }
    }
}
