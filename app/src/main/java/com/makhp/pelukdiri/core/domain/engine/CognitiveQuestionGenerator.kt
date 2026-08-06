package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.MathQuestion
import kotlin.random.Random
import javax.inject.Inject

class CognitiveQuestionGenerator @Inject constructor() {
    private val random = Random.Default

    fun generateQuestion(level: Int): MathQuestion {
        val normalizedLevel = level.coerceIn(1, 5)
        return when (normalizedLevel) {
            1 -> generateLevelOne()
            2 -> generateLevelTwo()
            3 -> generateLevelThree()
            4 -> generateLevelFour()
            else -> generateLevelFive()
        }
    }

    private fun generateLevelOne(): MathQuestion {
        val first = randomInRange(10, 50)
        val second = randomInRange(10, 50)
        val isAddition = random.nextBoolean()
        return if (isAddition) {
            MathQuestion(
                expression = "$first + $second",
                correctAnswer = first + second,
                level = 1
            )
        } else {
            val larger = maxOf(first, second)
            val smaller = minOf(first, second)
            MathQuestion(
                expression = "$larger - $smaller",
                correctAnswer = larger - smaller,
                level = 1
            )
        }
    }

    private fun generateLevelTwo(): MathQuestion {
        return if (random.nextBoolean()) {
            val first = randomInRange(100, 500)
            val second = randomInRange(100, 500)
            MathQuestion(
                expression = "$first + $second",
                correctAnswer = first + second,
                level = 2
            )
        } else {
            val first = randomInRange(4, 9)
            val second = randomInRange(12, 25)
            MathQuestion(
                expression = "$first * $second",
                correctAnswer = first * second,
                level = 2
            )
        }
    }

    private fun generateLevelThree(): MathQuestion {
        val first = randomInRange(11, 20)
        val second = randomInRange(3, 8)
        val third = randomInRange(15, 50)
        return MathQuestion(
            expression = "($first * $second) + $third",
            correctAnswer = (first * second) + third,
            level = 3
        )
    }

    private fun generateLevelFour(): MathQuestion {
        return if (random.nextBoolean()) {
            val first = randomInRange(11, 25)
            val second = randomInRange(11, 20)
            MathQuestion(
                expression = "$first * $second",
                correctAnswer = first * second,
                level = 4
            )
        } else {
            val divisor = randomInRange(6, 12)
            val quotient = randomInRange(12, 30)
            val dividend = divisor * quotient
            MathQuestion(
                expression = "$dividend / $divisor",
                correctAnswer = quotient,
                level = 4
            )
        }
    }

    private fun generateLevelFive(): MathQuestion {
        while (true) {
            val first = randomInRange(15, 30)
            val second = randomInRange(6, 12)
            val third = randomInRange(20, 80)
            val fourth = randomInRange(20, 80)

            val result = (first * second) - (third + fourth)
            if (result > 0) {
                return MathQuestion(
                    expression = "($first * $second) - ($third + $fourth)",
                    correctAnswer = result,
                    level = 5
                )
            }
        }
    }

    private fun randomInRange(min: Int, max: Int): Int = random.nextInt(from = min, until = max + 1)
}
