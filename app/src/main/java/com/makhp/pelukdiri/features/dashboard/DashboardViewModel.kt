package com.makhp.pelukdiri.features.dashboard

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.collector.AppBlockerAccessibilityService
import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.collector.AppUsageInsight
import com.makhp.pelukdiri.collector.UsageEventCollector
import com.makhp.pelukdiri.core.database.export.CsvExporter
import com.makhp.pelukdiri.core.domain.model.HistoricalConfig
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionDecisionRepository
import com.makhp.pelukdiri.core.domain.model.InterventionDecisionReason
import com.makhp.pelukdiri.core.domain.usecase.InitializeDailyAdaptiveLimitUseCase
import com.makhp.pelukdiri.core.util.AccessibilityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val adaptiveLimitRepository: AdaptiveLimitRepository,
    private val appUsageCollector: AppUsageCollector,
    private val usageEventCollector: UsageEventCollector,
    private val interventionDecisionRepository: InterventionDecisionRepository,
    private val csvExporter: CsvExporter,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val initializeDailyAdaptiveLimitUseCase: InitializeDailyAdaptiveLimitUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            // Lightweight sync on startup instead of backfill
            usageRepository.syncRecentEventsOnly()

            if (!userPreferencesRepository.isHistoryBackfilled.first()) {
                try {
                    usageRepository.executeFullBackfill(HistoricalConfig.BACKFILL_DAYS, force = false)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    // Settings keeps a visible, retryable import action if the automatic attempt fails.
                }
            }

            val isGranted = appUsageCollector.isPermissionGranted()
            val isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context, AppBlockerAccessibilityService::class.java)
            val isOptimized = isBatteryOptimizationIgnored()
            val isBackfilled = userPreferencesRepository.isHistoryBackfilled.first()
            val monitored = userPreferencesRepository.monitoredPackages.first()
            val dnd = userPreferencesRepository.isDndEnabled.first()
            
            _uiState.value = dashboardState(
                isPermissionGranted = isGranted,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isBatteryOptimizationIgnored = isOptimized,
                isHistoryBackfilled = isBackfilled,
                monitoredPackages = monitored,
                isDndEnabled = dnd
            )
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun forceRefresh() {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.update { currentState.copy(isRefreshing = true) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                usageRepository.refreshUsageData()
                val isGranted = appUsageCollector.isPermissionGranted()
                val isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context, AppBlockerAccessibilityService::class.java)
                val isOptimized = isBatteryOptimizationIgnored()
                val isBackfilled = userPreferencesRepository.isHistoryBackfilled.first()

                val monitored = userPreferencesRepository.monitoredPackages.first()
                val dnd = userPreferencesRepository.isDndEnabled.first()
                _uiState.value = dashboardState(
                    isPermissionGranted = isGranted,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isBatteryOptimizationIgnored = isOptimized,
                    isHistoryBackfilled = isBackfilled,
                    monitoredPackages = monitored,
                    isRefreshing = false,
                    isDndEnabled = dnd
                )
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Failed to refresh data")
            }
        }
    }

    private suspend fun dashboardState(
        isPermissionGranted: Boolean,
        isAccessibilityEnabled: Boolean,
        isBatteryOptimizationIgnored: Boolean,
        isHistoryBackfilled: Boolean,
        monitoredPackages: Set<String>,
        isRefreshing: Boolean = false,
        isDndEnabled: Boolean = false
    ): DashboardUiState.Success {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        val todayApps = usageRepository.getDailyUsage(today).first()
            .sortedByDescending { it.usageDurationMillis }
        val yesterdayApps = usageRepository.getDailyUsage(yesterday).first()
            .associateBy { it.packageName }
        val todayInsights = usageEventCollector.getAppInsightsForDay(today)
        val yesterdayInsights = usageEventCollector.getAppInsightsForDay(yesterday)
        val zoneId = ZoneId.systemDefault()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val dayStart = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1
        val interventionCounts = interventionDecisionRepository.getAllList()
            .asSequence()
            .filter { it.reason == InterventionDecisionReason.TRIGGERED && it.timestamp in dayStart..dayEnd }
            .groupingBy { it.packageName }
            .eachCount()

        // Enrich today's apps with yesterday's comparison data for the UI
        val enrichedTodayApps = todayApps.map { app ->
            val yesterdayApp = yesterdayApps[app.packageName]
            UiAppUsage(
                domain = app,
                usageDurationYesterdayMillis = yesterdayApp?.usageDurationMillis,
                openingsToday = todayInsights[app.packageName]?.launchCount ?: 0,
                openingsYesterday = yesterdayInsights[app.packageName]?.launchCount ?: 0,
                peakTimeToday = todayInsights[app.packageName].toPeakTimeLabel(formatter, zoneId),
                peakTimeYesterday = yesterdayInsights[app.packageName].toPeakTimeLabel(formatter, zoneId),
                longestSessionTodayMillis = todayInsights[app.packageName]?.longestSessionDurationMillis,
                longestSessionYesterdayMillis = yesterdayInsights[app.packageName]?.longestSessionDurationMillis,
                interventionsToday = interventionCounts[app.packageName] ?: 0,
                interventionsLimit = 10
            )
        }

        return DashboardUiState.Success(
            isPermissionGranted = isPermissionGranted,
            isAccessibilityEnabled = isAccessibilityEnabled,
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
            isHistoryBackfilled = isHistoryBackfilled,
            monitoredPackages = monitoredPackages.toImmutableSet(),
            todaySummary = usageRepository.getDailySummary(today).first(),
            todayAdaptiveLimit = adaptiveLimitRepository.getLimitForDate(today.toString())?.calculatedLimitMinutes,
            weeklySummaries = usageRepository.getUsageHistory(today.minusDays(6), today).first().toImmutableList(),
            topApps = enrichedTodayApps.toImmutableList(),
            yesterdayTopApps = yesterdayApps.values.map { UiAppUsage(it) }.toImmutableList(),
            isRefreshing = isRefreshing,
            isDndEnabled = isDndEnabled,
            socialMediaUsageMillis = usageRepository.getDailySummary(today).first()?.monitoredUsageMillis ?: 0L,
            yesterdaySocialMediaUsageMillis = usageRepository.getDailySummary(yesterday).first()?.monitoredUsageMillis ?: 0L
        )
    }

    fun updatePermissionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val isGranted = appUsageCollector.isPermissionGranted()
            val isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context, AppBlockerAccessibilityService::class.java)
            val isOptimized = isBatteryOptimizationIgnored()
            val isBackfilled = userPreferencesRepository.isHistoryBackfilled.first()
            val monitored = userPreferencesRepository.monitoredPackages.first()
            val dnd = userPreferencesRepository.isDndEnabled.first()

            _uiState.update { state ->
                if (state is DashboardUiState.Success) {
                    state.copy(
                        isPermissionGranted = isGranted,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isBatteryOptimizationIgnored = isOptimized,
                        isHistoryBackfilled = isBackfilled,
                        monitoredPackages = monitored.toImmutableSet(),
                        isDndEnabled = dnd
                    )
                } else {
                    state
                }
            }
        }
    }

    fun toggleDnd() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = userPreferencesRepository.isDndEnabled.first()
            userPreferencesRepository.setDndEnabled(!current)
            updatePermissionStatus()
        }
    }

    fun toggleTargetApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.toggleMonitoredPackage(packageName)
            updatePermissionStatus()
        }
    }

    fun recalculateAdaptiveLimit() {
        val currentState = _uiState.value as? DashboardUiState.Success ?: return
        _uiState.value = currentState.copy(
            isRecalculatingAdaptiveLimit = true,
            adaptiveLimitError = null
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                initializeDailyAdaptiveLimitUseCase(force = true)
                val recalculated = adaptiveLimitRepository
                    .getLimitForDate(LocalDate.now().toString())
                    ?.calculatedLimitMinutes
                _uiState.update { state ->
                    if (state is DashboardUiState.Success) {
                        state.copy(
                            todayAdaptiveLimit = recalculated,
                            isRecalculatingAdaptiveLimit = false
                        )
                    } else state
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { state ->
                    if (state is DashboardUiState.Success) {
                        state.copy(
                            isRecalculatingAdaptiveLimit = false,
                            adaptiveLimitError = context.getString(R.string.dashboard_recalculate_limit_failed)
                        )
                    } else state
                }
            }
        }
    }

    fun exportDatabase() {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.update { currentState.copy(isExporting = true, exportedFile = null, exportError = null) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            csvExporter.exportFullDatabaseToZip().fold(
                onSuccess = { export ->
                    _uiState.update { state ->
                        if (state is DashboardUiState.Success) {
                            state.copy(isExporting = false, exportedFile = export.archiveFile)
                        } else {
                            state
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        if (state is DashboardUiState.Success) {
                            state.copy(isExporting = false, exportError = error.message ?: context.getString(R.string.export_failed))
                        } else {
                            state
                        }
                    }
                }
            )
        }
    }

    fun clearExportResult() {
        _uiState.update { state ->
            if (state is DashboardUiState.Success) {
                state.copy(exportedFile = null, exportError = null)
            } else {
                state
            }
        }
    }
}

private fun AppUsageInsight?.toPeakTimeLabel(
    formatter: DateTimeFormatter,
    zoneId: ZoneId,
): String? = this?.let { insight ->
    val start = insight.longestSessionStartMillis ?: return@let null
    val end = insight.longestSessionEndMillis ?: return@let null
    "${java.time.Instant.ofEpochMilli(start).atZone(zoneId).format(formatter)}–" +
        java.time.Instant.ofEpochMilli(end).atZone(zoneId).format(formatter)
}
