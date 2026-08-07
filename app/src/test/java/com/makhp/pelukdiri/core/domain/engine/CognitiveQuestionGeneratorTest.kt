package com.makhp.pelukdiri.core.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CognitiveQuestionGeneratorTest {
    private val generator = CognitiveQuestionGenerator()

    @Test
    fun `level 1 stays within range and result is non negative for subtraction`() {
        repeat(300) {
            val question = generator.generateQuestion(level = 1)
            val match = Regex("""(\d+) ([+-]) (\d+)""").matchEntire(question.expression)
            assertNotNull(match)
            val (firstRaw, operator, secondRaw) = match!!.destructured
            val first = firstRaw.toInt()
            val second = secondRaw.toInt()

            assertTrue(first in 10..50)
            assertTrue(second in 10..50)
            val computed = if (operator == "+") first + second else first - second
            if (operator == "-") {
                assertTrue(computed >= 0)
            }
            assertEquals(computed, question.correctAnswer)
        }
    }

    @Test
    fun `level 2 respects add or multiply bounds`() {
        repeat(300) {
            val question = generator.generateQuestion(level = 2)
            val addMatch = Regex("""(\d+) \+ (\d+)""").matchEntire(question.expression)
            val multiplyMatch = Regex("""(\d+) \* (\d+)""").matchEntire(question.expression)
            assertTrue(addMatch != null || multiplyMatch != null)

            if (addMatch != null) {
                val (firstRaw, secondRaw) = addMatch.destructured
                val first = firstRaw.toInt()
                val second = secondRaw.toInt()
                assertTrue(first in 100..500)
                assertTrue(second in 100..500)
                assertEquals(first + second, question.correctAnswer)
            }

            if (multiplyMatch != null) {
                val (firstRaw, secondRaw) = multiplyMatch.destructured
                val first = firstRaw.toInt()
                val second = secondRaw.toInt()
                assertTrue(first in 4..9)
                assertTrue(second in 12..25)
                assertEquals(first * second, question.correctAnswer)
            }
        }
    }

    @Test
    fun `level 3 follows two step formula bounds`() {
        repeat(300) {
            val question = generator.generateQuestion(level = 3)
            val match = Regex("""\((\d+) \* (\d+)\) \+ (\d+)""").matchEntire(question.expression)
            assertNotNull(match)
            val (firstRaw, secondRaw, thirdRaw) = match!!.destructured
            val first = firstRaw.toInt()
            val second = secondRaw.toInt()
            val third = thirdRaw.toInt()

            assertTrue(first in 11..20)
            assertTrue(second in 3..8)
            assertTrue(third in 15..50)
            assertEquals((first * second) + third, question.correctAnswer)
        }
    }

    @Test
    fun `level 4 follows multiplication or reverse division constraints`() {
        repeat(300) {
            val question = generator.generateQuestion(level = 4)
            val multiplyMatch = Regex("""(\d+) \* (\d+)""").matchEntire(question.expression)
            val divideMatch = Regex("""(\d+) / (\d+)""").matchEntire(question.expression)
            assertTrue(multiplyMatch != null || divideMatch != null)

            if (multiplyMatch != null) {
                val (firstRaw, secondRaw) = multiplyMatch.destructured
                val first = firstRaw.toInt()
                val second = secondRaw.toInt()
                assertTrue(first in 11..25)
                assertTrue(second in 11..20)
                assertEquals(first * second, question.correctAnswer)
            }

            if (divideMatch != null) {
                val (dividendRaw, divisorRaw) = divideMatch.destructured
                val dividend = dividendRaw.toInt()
                val divisor = divisorRaw.toInt()
                assertTrue(divisor in 6..12)
                assertEquals(0, dividend % divisor)
                val quotient = dividend / divisor
                assertTrue(quotient in 12..30)
                assertEquals(quotient, question.correctAnswer)
            }
        }
    }

    @Test
    fun `level 5 stays positive and matches expression math`() {
        repeat(300) {
            val question = generator.generateQuestion(level = 5)
            val match = Regex("""\((\d+) \* (\d+)\) - \((\d+) \+ (\d+)\)""")
                .matchEntire(question.expression)
            assertNotNull(match)
            val (firstRaw, secondRaw, thirdRaw, fourthRaw) = match!!.destructured
            val first = firstRaw.toInt()
            val second = secondRaw.toInt()
            val third = thirdRaw.toInt()
            val fourth = fourthRaw.toInt()

            val computed = (first * second) - (third + fourth)
            assertTrue(computed > 0)
            assertEquals(computed, question.correctAnswer)
        }
    }
}
