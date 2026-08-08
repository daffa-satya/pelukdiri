package com.makhp.pelukdiri.features.settings

import androidx.compose.runtime.Immutable

@Immutable
data class SettingsUiState(
    val isAdaptiveModeEnabled: Boolean = true,
    val notificationStatus: String = "Aktif",
    val appVersion: String = "1.0.0 (Beta)"
)
