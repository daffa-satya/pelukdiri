package com.makhp.pelukdiri.features.settings

import androidx.compose.runtime.Immutable
import com.makhp.pelukdiri.core.domain.model.AggressivenessLevel

@Immutable
data class SettingsUiState(
    val isAdaptiveModeEnabled: Boolean = true,
    val notificationStatus: String = "Aktif",
    val appVersion: String = "1.0.0 (Beta)",
    val aggressivenessLevel: AggressivenessLevel = AggressivenessLevel.BALANCED,
    val isFixedLimitEnabled: Boolean = false,
    val fixedDailyLimitMinutes: Int = 60,
    val sleepTime: String = "22:00",
    val wakeTime: String = "06:00",
    val isExporting: Boolean = false,
    val exportedFilePath: String? = null,
    val exportError: String? = null,
)
