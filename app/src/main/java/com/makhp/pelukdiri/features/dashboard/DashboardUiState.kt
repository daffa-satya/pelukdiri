package com.makhp.pelukdiri.features.dashboard

import androidx.compose.runtime.Immutable

import java.io.File

@Immutable
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    
    data class Success(
        val isPermissionGranted: Boolean,
        val isAccessibilityEnabled: Boolean = true,
        val isBatteryOptimizationIgnored: Boolean = true,
        val isRefreshing: Boolean = false,
        val isBackfilling: Boolean = false,
        val isHistoryBackfilled: Boolean = false,
        val isExporting: Boolean = false,
        val exportedFile: File? = null,
        val exportError: String? = null
    ) : DashboardUiState
    
    data class Error(val message: String) : DashboardUiState
}
