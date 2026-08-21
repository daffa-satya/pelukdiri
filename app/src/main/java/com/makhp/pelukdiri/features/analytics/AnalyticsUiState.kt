package com.makhp.pelukdiri.features.analytics

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import com.makhp.pelukdiri.features.dashboard.UiAppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import java.time.LocalDate

@Immutable
enum class AnalyticsPeriod {
    DAILY, WEEKLY, MONTHLY
}

@Immutable
sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Success(
        val selectedDate: LocalDate,
        val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.DAILY,
        val summary: DailySummary?,
        val comparisonSummary: DailySummary? = null,
        val topApps: ImmutableList<UiAppUsage>,
        val hourlyUsage: ImmutableList<Long>,
        val interventionCount: Int,
        val socialMediaUsageMillis: Long = 0L,
        val adaptiveLimitMinutes: Int? = null,
        val funFact: String = ""
    ) : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}
