package com.makhp.pelukdiri.features.settings

import androidx.compose.runtime.Immutable

@Immutable
data class AdaptiveModeUiState(
    val isEnabled: Boolean = true,
    val predictedLimit: String = "3j 20m",
    val interventionIntensity: String = "Seimbang",
    val difficultyLevel: String = "Sedang",
    val monitoredAppsCount: Int = 6,
    val totalAppsCount: Int = 12
)
