package com.makhp.pelukdiri.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UsageSensorRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val usageSensorRepository: UsageSensorRepository,
    private val interventionLogRepository: InterventionLogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init { load(LocalDate.now()) }

    fun load(date: LocalDate) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val zone = ZoneId.systemDefault()
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val hourlyUsage = usageSensorRepository.getLogsInRange(start, end)
                .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).hour }
                .mapValues { (_, logs) -> logs.sumOf { it.rawScreenTimeMs } }
            val interventions = interventionLogRepository.getAllLogsList().count { it.timestamp in start..end }
            AnalyticsUiState.Success(
                selectedDate = date,
                summary = usageRepository.getDailySummary(date).first(),
                topApps = usageRepository.getDailyUsage(date).first().sortedByDescending { it.usageDurationMillis },
                hourlyUsage = List(24) { hour -> hourlyUsage[hour] ?: 0L },
                interventionCount = interventions
            )
        }.onSuccess { _uiState.value = it }
            .onFailure { _uiState.value = AnalyticsUiState.Error(it.message ?: "Tidak dapat memuat analytics") }
    }
}
