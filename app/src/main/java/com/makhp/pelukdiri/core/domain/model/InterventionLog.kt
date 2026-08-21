package com.makhp.pelukdiri.core.domain.model

import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType

data class InterventionLog(
    val id: Long = 0,
    val timestamp: Long,
    val deviation: Double,
    val difficultyControlSignal: Double,
    val difficultyLevel: Int,
    val responseTimeMs: Long,
    val isSuccess: Boolean,
    val isBypassed: Boolean = false,
    val penaltyAppliedMinutes: Int,
    val challengeType: InterventionChallengeType = InterventionChallengeType.MATH,
)
