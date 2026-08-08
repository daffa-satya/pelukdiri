package com.makhp.pelukdiri.core.domain.model

data class RiskAssessmentResult(
    val riskScore: Double,
    val level: Int,
    val penaltyMinutes: Int,
    val calculatedLimitMinutes: Int
)
