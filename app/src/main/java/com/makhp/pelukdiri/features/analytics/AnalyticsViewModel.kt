package com.makhp.pelukdiri.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.features.dashboard.UiAppUsage
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UsageSensorRepository
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val usageSensorRepository: UsageSensorRepository,
    private val interventionLogRepository: InterventionLogRepository,
    private val adaptiveLimitRepository: AdaptiveLimitRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val socialMediaPackages = setOf(
        "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
        "com.google.android.youtube", "com.twitter.android", "com.facebook.katana",
        "com.whatsapp", "com.snapchat.android", "com.facebook.orca", "com.google.android.apps.messaging",
        "com.tencent.mm", "com.viber.voip", "jp.naver.line.android", "com.discord"
    )

    init { 
        load(LocalDate.now(), AnalyticsPeriod.DAILY) 
        startFunFactRotation()
    }

    private fun startFunFactRotation() = viewModelScope.launch {
        while (true) {
            val state = _uiState.value
            if (state is AnalyticsUiState.Success) {
                _uiState.update { 
                    (it as AnalyticsUiState.Success).copy(funFact = generateFunFact(it))
                }
            }
            delay(5 * 60 * 1000L) // 5 minutes
        }
    }

    private fun generateFunFact(state: AnalyticsUiState.Success): String {
        val socialTime = state.topApps
            .filter { socialMediaPackages.contains(it.packageName) }
            .sumOf { it.usageDurationMillis }
        
        val totalTime = state.summary?.totalScreenTimeMillis ?: 0L
        val activeTime = if (socialTime > 0) socialTime else totalTime
        
        if (activeTime == 0L) return "Mulai kelola waktu layarmu untuk melihat fakta menarik di sini!"

        val formattedTime = com.makhp.pelukdiri.ui.components.formatDuration(activeTime)
        val minutes = activeTime / 60_000L

        return when ((0..4).random()) {
            0 -> context.getString(R.string.analytics_fun_fact_guitar, formattedTime)
            1 -> context.getString(R.string.analytics_fun_fact_reading, formattedTime, (minutes / 2).toInt())
            2 -> context.getString(R.string.analytics_fun_fact_meditation, formattedTime, (minutes / 10).toInt())
            3 -> context.getString(R.string.analytics_fun_fact_workout, formattedTime)
            else -> context.getString(R.string.analytics_fun_fact_language, formattedTime)
        }
    }

    fun load(date: LocalDate, period: AnalyticsPeriod = AnalyticsPeriod.DAILY) = viewModelScope.launch(Dispatchers.IO) {
        val currentState = _uiState.value
        if (currentState !is AnalyticsUiState.Success) {
            _uiState.value = AnalyticsUiState.Loading
        }
        
        runCatching {
            val zone = ZoneId.systemDefault()
            val (startDate, endDate) = when (period) {
                AnalyticsPeriod.DAILY -> date to date
                AnalyticsPeriod.WEEKLY -> date.minusDays(6) to date
                AnalyticsPeriod.MONTHLY -> date.withDayOfMonth(1) to date.withDayOfMonth(date.lengthOfMonth())
            }

            val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            val history = usageRepository.getUsageHistory(startDate, endDate).first()
            
            val aggregatedSummary = if (period == AnalyticsPeriod.DAILY) {
                usageRepository.getDailySummary(date).first()
            } else {
                val totalDaysInRange = when(period) {
                    AnalyticsPeriod.WEEKLY -> 7L
                    AnalyticsPeriod.MONTHLY -> date.lengthOfMonth().toLong()
                    else -> 1L
                }
                DailySummary(
                    date = date,
                    totalScreenTimeMillis = history.sumOf { it.totalScreenTimeMillis } / totalDaysInRange,
                    totalScreenOnMillis = history.sumOf { it.totalScreenOnMillis } / totalDaysInRange,
                    unlockCount = (history.sumOf { it.unlockCount } / totalDaysInRange).toInt(),
                    mostUsedApp = history.groupBy { it.mostUsedApp }.filter { it.key != null }.maxByOrNull { it.value.size }?.key,
                    wellbeingScore = history.mapNotNull { it.wellbeingScore?.toDouble() }.let { if (it.isEmpty()) 0 else it.average().toInt() }
                )
            }

            val topApps = if (period == AnalyticsPeriod.DAILY) {
                val yesterdayDate = date.minusDays(1)
                val yesterdayUsage = usageRepository.getDailyUsage(yesterdayDate).first()
                val yesterdayMap = yesterdayUsage.associateBy { it.packageName }
                
                usageRepository.getDailyUsage(date).first().map { app ->
                    val yesterday = yesterdayMap[app.packageName]
                    UiAppUsage(
                        domain = app,
                        usageDurationYesterdayMillis = yesterday?.usageDurationMillis ?: 0L,
                        interventionsToday = null // Expose null/unavailable state
                    )
                }
            } else {
                val totalDaysInRange = when(period) {
                    AnalyticsPeriod.WEEKLY -> 7L
                    AnalyticsPeriod.MONTHLY -> date.lengthOfMonth().toLong()
                    else -> 1L
                }
                
                val allApps = mutableMapOf<String, AppUsage>()
                var current = startDate
                while (!current.isAfter(endDate)) {
                    usageRepository.getDailyUsage(current).first().forEach { app ->
                        val existing = allApps[app.packageName]
                        if (existing == null) {
                            allApps[app.packageName] = app
                        } else {
                            allApps[app.packageName] = existing.copy(
                                usageDurationMillis = existing.usageDurationMillis + app.usageDurationMillis
                            )
                        }
                    }
                    current = current.plusDays(1)
                }
                allApps.values.map { 
                    UiAppUsage(domain = it.copy(usageDurationMillis = it.usageDurationMillis / totalDaysInRange))
                }
            }.sortedByDescending { it.usageDurationMillis }.toImmutableList()

            val logs = usageSensorRepository.getLogsInRange(startMillis, endMillis)
            val hourlyUsage = logs
                .groupBy { Instant.ofEpochMilli(it.timestamp).atZone(zone).hour }
                .mapValues { (_, hourLogs) -> 
                    val total = hourLogs.sumOf { it.rawScreenTimeMs }
                    if (period == AnalyticsPeriod.DAILY) total else total / history.size.coerceAtLeast(1)
                }
            
            val interventions = interventionLogRepository.getAllLogsList().count { it.timestamp in startMillis..endMillis }
            
            val limit = if (period == AnalyticsPeriod.DAILY) {
                adaptiveLimitRepository.getLimitForDate(date.toString())?.calculatedLimitMinutes
            } else null

            val successState = AnalyticsUiState.Success(
                selectedDate = date,
                selectedPeriod = period,
                summary = aggregatedSummary,
                topApps = topApps,
                hourlyUsage = List(24) { hour -> hourlyUsage[hour] ?: 0L }.toImmutableList(),
                interventionCount = interventions,
                adaptiveLimitMinutes = limit
            )

            successState.copy(funFact = generateFunFact(successState))
        }.onSuccess { _uiState.value = it }
            .onFailure { _uiState.value = AnalyticsUiState.Error(it.message ?: "Tidak dapat memuat analytics") }
    }
}
