package com.makhp.pelukdiri.core.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternQuestionGeneratorTest {
    private val generator = PatternQuestionGenerator()

    @Test fun `difficulty maps to sequence length three through seven`() {
        (1..5).forEach { level ->
            val question = generator.generateQuestion(level)
            assertEquals(level, question.level)
            assertEquals(level + 2, question.sequence.size)
            assertTrue(question.sequence.isNotEmpty())
        }
    }

    @Test fun `difficulty is clamped to supported range`() {
        assertEquals(3, generator.generateQuestion(-1).sequence.size)
        assertEquals(7, generator.generateQuestion(99).sequence.size)
    }
}
