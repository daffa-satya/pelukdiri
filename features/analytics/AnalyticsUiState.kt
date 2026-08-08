package com.makhp.pelukdiri.features.analytics

import androidx.compose.runtime.Immutable
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import java.time.LocalDate

@Immutable
sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Success(
        val selectedDate: LocalDate,
        val summary: DailySummary?,
        val topApps: List<AppUsage>,
        val hourlyUsage: List<Long>,
        val interventionCount: Int
    ) : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}
