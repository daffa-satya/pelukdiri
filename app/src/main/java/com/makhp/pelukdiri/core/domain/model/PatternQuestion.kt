package com.makhp.pelukdiri.core.domain.model

enum class PatternShape {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    PENTAGON,
}

data class PatternQuestion(
    val sequence: List<PatternShape>,
    val level: Int,
)
