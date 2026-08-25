package com.makhp.pelukdiri.features.dashboard

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import com.makhp.pelukdiri.core.domain.model.DailySummary
import java.io.File

@Immutable
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    
    data class Success(
        val isPermissionGranted: Boolean,
        val isAccessibilityEnabled: Boolean = true,
        val isBatteryOptimizationIgnored: Boolean = true,
        val monitoredPackages: ImmutableSet<String> = persistentSetOf(),
        val todaySummary: DailySummary? = null,
        val todayAdaptiveLimit: Int? = null,
        val weeklySummaries: ImmutableList<DailySummary> = persistentListOf(),
        val topApps: ImmutableList<UiAppUsage> = persistentListOf(),
        val yesterdayTopApps: ImmutableList<UiAppUsage> = persistentListOf(),
        val isRefreshing: Boolean = false,
        val isRecalculatingAdaptiveLimit: Boolean = false,
        val adaptiveLimitError: String? = null,
        val isHistoryBackfilled: Boolean = false,
        val isExporting: Boolean = false,
        val exportedFile: File? = null,
        val exportError: String? = null,
        val isDndEnabled: Boolean = false,
        val socialMediaUsageMillis: Long = 0L,
        val yesterdaySocialMediaUsageMillis: Long = 0L
    ) : DashboardUiState
    
    data class Error(val message: String) : DashboardUiState
}
