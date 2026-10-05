package com.makhp.pelukdiri.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.database.export.CsvExporter
import com.makhp.pelukdiri.core.domain.model.AggressivenessLevel
import com.makhp.pelukdiri.core.domain.model.HistoricalConfig
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.R
import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val csvExporter: CsvExporter,
    private val usageRepository: UsageRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val localizedContext: Context
        get() = ContextCompat.getContextForLanguage(context)

    private val exportState = MutableStateFlow(ExportState())
    private val backfillState = MutableStateFlow(BackfillState())

    private val preferencesState = userPreferencesRepository.snapshot.map { preferences ->
        SettingsUiState(
            aggressivenessLevel = preferences.aggressivenessLevel,
            isFixedLimitEnabled = preferences.isFixedLimitEnabled,
            fixedDailyLimitMinutes = preferences.fixedDailyLimitMinutes,
            sleepTime = preferences.bedtime ?: "22:00",
            wakeTime = preferences.wakeTime ?: "06:00",
            appVersion = "1.0.0 (Beta)"
        )
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesState,
        exportState,
        backfillState,
    ) { settings, export, backfill ->
        settings.copy(
            isExporting = export.isExporting,
            exportedFilePath = export.exportedFilePath,
            exportError = export.error,
            isBackfilling = backfill.isRunning,
            backfillError = backfill.hasError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setAggressiveness(level: AggressivenessLevel) {
        viewModelScope.launch {
            userPreferencesRepository.setAggressivenessLevel(level)
        }
    }

    fun toggleFixedLimit(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFixedLimitEnabled(enabled)
        }
    }

    fun setFixedLimitMinutes(minutes: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setFixedDailyLimitMinutes(minutes)
        }
    }

    fun setSleepTime(time: String) {
        viewModelScope.launch {
            userPreferencesRepository.setBedtime(time)
        }
    }

    fun setWakeTime(time: String) {
        viewModelScope.launch {
            userPreferencesRepository.setWakeTime(time)
        }
    }

    fun exportDatabase() {
        if (exportState.value.isExporting) return
        exportState.value = ExportState(isExporting = true)
        viewModelScope.launch(Dispatchers.IO) {
            csvExporter.exportFullDatabaseToZip().fold(
                onSuccess = { export ->
                    exportState.value = ExportState(exportedFilePath = export.savedPath)
                },
                onFailure = { error ->
                    exportState.value = ExportState(error = error.message ?: localizedContext.getString(R.string.export_failed))
                },
            )
        }
    }

    fun clearExportResult() {
        exportState.value = ExportState()
    }

    fun reportExportPermissionDenied() {
        exportState.value = ExportState(error = localizedContext.getString(R.string.export_storage_permission_required))
    }

    fun backfillHistory() {
        if (backfillState.value.isRunning) return
        backfillState.value = BackfillState(isRunning = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                usageRepository.executeFullBackfill(HistoricalConfig.BACKFILL_DAYS, force = false)
                backfillState.value = BackfillState()
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (_: Exception) {
                backfillState.value = BackfillState(hasError = true)
            }
        }
    }

    fun clearBackfillError() {
        backfillState.value = BackfillState()
    }

    fun logout() {
        // Handle logout logic
    }

    private data class ExportState(
        val isExporting: Boolean = false,
        val exportedFilePath: String? = null,
        val error: String? = null,
    )

    private data class BackfillState(
        val isRunning: Boolean = false,
        val hasError: Boolean = false,
    )
}
