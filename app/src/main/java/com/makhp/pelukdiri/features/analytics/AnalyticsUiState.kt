package com.makhp.pelukdiri.features.analytics

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
        val canEditUsage: Boolean = false,
        val summary: DailySummary?,
        val comparisonSummary: DailySummary? = null,
        val topApps: ImmutableList<UiAppUsage>,
        val hourlyUsage: ImmutableList<Long>,
        val isGraphCalculated: Boolean = false,
        val isCalculatingGraph: Boolean = false,
        val graphError: String? = null,
        val interventionCount: Int,
        val comparisonInterventionCount: Int = 0,
        val socialMediaUsageMillis: Long = 0L,
        val comparisonSocialMediaUsageMillis: Long = 0L,
        val longestSessionMillis: Long = 0L,
        val comparisonLongestSessionMillis: Long = 0L,
        val adaptiveLimitMinutes: Int? = null,
        val funFact: String = "",
        val allInstalledApps: ImmutableList<UiAppUsage> = persistentListOf(),
        val isInstalledAppsLoading: Boolean = false,
        val installedAppsError: String? = null,
        val editUsageError: String? = null,
    ) : AnalyticsUiState
    data class Error(val message: String) : AnalyticsUiState
}

internal fun percentageChange(current: Long, previous: Long): Int? =
    if (previous == 0L) null else (((current - previous) * 100) / previous).toInt()

internal fun shouldCalculateGraphAutomatically(
    period: AnalyticsPeriod,
    selectedDate: LocalDate,
    today: LocalDate,
): Boolean = period == AnalyticsPeriod.DAILY && selectedDate == today
