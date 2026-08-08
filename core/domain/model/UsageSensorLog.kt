package com.makhp.pelukdiri.core.domain.model

data class UsageSensorLog(
    val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val rawScreenTimeMs: Long,
    val appOpeningFrequency: Int,
    val ambientLightLux: Float
)
