package com.makhp.pelukdiri.core.domain.model

data class InterventionLog(
    val id: Long = 0,
    val timestamp: Long,
    val targetPackageName: String,
    val questionType: String,
    val difficultyLevel: String,
    val responseTimeMs: Long,
    val isCorrect: Boolean,
    val isBypassed: Boolean
)
