package com.makhp.pelukdiri.features.dashboard

import androidx.compose.runtime.Immutable
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import java.io.File

@Immutable
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    
    data class Success(
        val isPermissionGranted: Boolean,
        val isAccessibilityEnabled: Boolean = true,
        val isBatteryOptimizationIgnored: Boolean = true,
        val monitoredPackages: Set<String> = emptySet(),
        val todaySummary: DailySummary? = null,
        val weeklySummaries: List<DailySummary> = emptyList(),
        val topApps: List<AppUsage> = emptyList(),
        val isRefreshing: Boolean = false,
        val isBackfilling: Boolean = false,
        val isHistoryBackfilled: Boolean = false,
        val isExporting: Boolean = false,
        val exportedFile: File? = null,
        val exportError: String? = null
    ) : DashboardUiState
    
    data class Error(val message: String) : DashboardUiState
}
