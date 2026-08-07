package com.makhp.pelukdiri.core.domain.model

data class DailyAdaptiveLimit(
    val dateString: String,
    val calculatedLimitMinutes: Int,
    val actualScreenTimeMinutes: Int,
    val reclaimedTimeMinutes: Int
)
