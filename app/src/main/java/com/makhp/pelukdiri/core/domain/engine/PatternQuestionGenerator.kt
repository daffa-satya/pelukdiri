package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.PatternQuestion
import com.makhp.pelukdiri.core.domain.model.PatternShape
import javax.inject.Inject
import kotlin.random.Random

class PatternQuestionGenerator @Inject constructor() {
    fun generateQuestion(level: Int): PatternQuestion {
        val normalizedLevel = level.coerceIn(1, 5)
        val shapes = PatternShape.entries
        return PatternQuestion(
            sequence = List(normalizedLevel + 2) { shapes[Random.nextInt(shapes.size)] },
            level = normalizedLevel,
        )
    }
}
