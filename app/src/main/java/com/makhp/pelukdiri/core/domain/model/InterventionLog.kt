package com.makhp.pelukdiri.core.domain.model

data class InterventionLog(
    val id: Long = 0,
    val timestamp: Long,
    val riskScore: Double,
    val difficultyLevel: Int,
    val responseTimeMs: Long,
    val isSuccess: Boolean,
    val penaltyAppliedMinutes: Int
)
