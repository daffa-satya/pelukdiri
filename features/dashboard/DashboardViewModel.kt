package com.makhp.pelukdiri.features.dashboard

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.collector.AppBlockerAccessibilityService
import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.core.database.export.CsvExporter
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.util.AccessibilityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val appUsageCollector: AppUsageCollector,
    private val csvExporter: CsvExporter,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
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

            val isGranted = appUsageCollector.isPermissionGranted()
            val isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context, AppBlockerAccessibilityService::class.java)
            val isOptimized = isBatteryOptimizationIgnored()
            val isBackfilled = userPreferencesRepository.isHistoryBackfilled.first()
            val monitored = userPreferencesRepository.monitoredPackages.first()
            
            _uiState.value = dashboardState(
                isPermissionGranted = isGranted,
                isAccessibilityEnabled = isAccessibilityEnabled,
                isBatteryOptimizationIgnored = isOptimized,
                isHistoryBackfilled = isBackfilled,
                monitoredPackages = monitored
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
                _uiState.value = dashboardState(
                    isPermissionGranted = isGranted,
                    isAccessibilityEnabled = isAccessibilityEnabled,
                    isBatteryOptimizationIgnored = isOptimized,
                    isHistoryBackfilled = isBackfilled,
                    monitoredPackages = monitored,
                    isRefreshing = false
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
        isRefreshing: Boolean = false
    ): DashboardUiState.Success {
        val today = LocalDate.now()
        return DashboardUiState.Success(
            isPermissionGranted = isPermissionGranted,
            isAccessibilityEnabled = isAccessibilityEnabled,
            isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
            isHistoryBackfilled = isHistoryBackfilled,
            monitoredPackages = monitoredPackages,
            todaySummary = usageRepository.getDailySummary(today).first(),
            weeklySummaries = usageRepository.getUsageHistory(today.minusDays(6), today).first(),
            topApps = usageRepository.getDailyUsage(today).first()
                .sortedByDescending { it.usageDurationMillis },
            isRefreshing = isRefreshing
        )
    }

    fun updatePermissionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val isGranted = appUsageCollector.isPermissionGranted()
            val isAccessibilityEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context, AppBlockerAccessibilityService::class.java)
            val isOptimized = isBatteryOptimizationIgnored()
            val isBackfilled = userPreferencesRepository.isHistoryBackfilled.first()
            val monitored = userPreferencesRepository.monitoredPackages.first()

            _uiState.update { state ->
                if (state is DashboardUiState.Success) {
                    state.copy(
                        isPermissionGranted = isGranted,
                        isAccessibilityEnabled = isAccessibilityEnabled,
                        isBatteryOptimizationIgnored = isOptimized,
                        isHistoryBackfilled = isBackfilled,
                        monitoredPackages = monitored
                    )
                } else {
                    state
                }
            }
        }
    }

    fun toggleTargetApp(packageName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferencesRepository.toggleMonitoredPackage(packageName)
            updatePermissionStatus()
        }
    }

    fun backfillHistory() {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.update { currentState.copy(isBackfilling = true) }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // strictly manual, on-demand operation
                usageRepository.executeFullBackfill(daysHistory = 7, force = false)
                val isBackfilled = userPreferencesRepository.isHistoryBackfilled.first()

                _uiState.update { state ->
                    if (state is DashboardUiState.Success) {
                        state.copy(isBackfilling = false, isHistoryBackfilled = isBackfilled)
                    } else {
                        state
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DashboardUiState.Error(e.message ?: "Failed to backfill history")
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
                onSuccess = { file ->
                    _uiState.update { state ->
                        if (state is DashboardUiState.Success) {
                            state.copy(isExporting = false, exportedFile = file)
                        } else {
                            state
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        if (state is DashboardUiState.Success) {
                            state.copy(isExporting = false, exportError = error.message ?: "Export failed")
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
