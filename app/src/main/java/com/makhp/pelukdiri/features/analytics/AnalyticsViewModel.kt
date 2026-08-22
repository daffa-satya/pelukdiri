package com.makhp.pelukdiri.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.collector.UsageEventCollector
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.features.dashboard.UiAppUsage
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
    private val interventionLogRepository: InterventionLogRepository,
    private val adaptiveLimitRepository: AdaptiveLimitRepository,
    private val usageEventCollector: UsageEventCollector,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var graphJob: Job? = null

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
        
        if (activeTime == 0L) return context.getString(R.string.analytics_fun_fact_empty)

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

    fun load(date: LocalDate, period: AnalyticsPeriod = AnalyticsPeriod.DAILY) {
        loadJob?.cancel()
        graphJob?.cancel()
        loadJob = viewModelScope.launch(Dispatchers.IO) {
        val today = LocalDate.now()
        val cappedDate = if (date.isAfter(today)) today else date

        val currentState = _uiState.value
        if (currentState !is AnalyticsUiState.Success) {
            _uiState.value = AnalyticsUiState.Loading
        }
        
        runCatching {
            val zone = ZoneId.systemDefault()
            val (startDate, endDate) = when (period) {
                AnalyticsPeriod.DAILY -> cappedDate to cappedDate
                AnalyticsPeriod.WEEKLY -> cappedDate.minusDays(6) to cappedDate
                AnalyticsPeriod.MONTHLY -> cappedDate.withDayOfMonth(1) to
                    minOf(cappedDate.withDayOfMonth(cappedDate.lengthOfMonth()), today)
            }
            val comparisonEndDate = startDate.minusDays(1)
            val comparisonStartDate = when (period) {
                AnalyticsPeriod.DAILY -> comparisonEndDate
                AnalyticsPeriod.WEEKLY -> comparisonEndDate.minusDays(6)
                AnalyticsPeriod.MONTHLY -> comparisonEndDate.withDayOfMonth(1)
            }

            val startMillis = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

            val history = usageRepository.getUsageHistory(startDate, endDate).first()
            val comparisonHistory = usageRepository
                .getUsageHistory(comparisonStartDate, comparisonEndDate)
                .first()
            
            val aggregatedSummary = if (period == AnalyticsPeriod.DAILY) {
                usageRepository.getDailySummary(cappedDate).first()
            } else {
                aggregateSummary(cappedDate, history)
            }

            val comparisonSummary = if (period == AnalyticsPeriod.DAILY) {
                usageRepository.getDailySummary(comparisonEndDate).first()
            } else {
                aggregateSummary(comparisonEndDate, comparisonHistory)
            }

            val activeDays = history.count { it.totalScreenTimeMillis > 0 }.coerceAtLeast(1)
            val comparisonActiveDays = comparisonHistory.count { it.totalScreenTimeMillis > 0 }.coerceAtLeast(1)
            val currentApps = loadAverageAppUsage(startDate, endDate, if (period == AnalyticsPeriod.DAILY) 1 else activeDays)
            val comparisonApps = loadAverageAppUsage(
                comparisonStartDate,
                comparisonEndDate,
                if (period == AnalyticsPeriod.DAILY) 1 else comparisonActiveDays
            )
            val comparisonAppsByPackage = comparisonApps.associateBy { it.packageName }
            val topApps = currentApps.map { app ->
                UiAppUsage(
                    domain = app,
                    usageDurationYesterdayMillis = if (period == AnalyticsPeriod.DAILY) {
                        comparisonAppsByPackage[app.packageName]?.usageDurationMillis ?: 0L
                    } else null,
                    interventionsToday = null
                )
            }.sortedByDescending { it.usageDurationMillis }.toImmutableList()

            val calculateGraphNow = shouldCalculateGraphAutomatically(period, cappedDate, today)
            val hourlyUsageList = if (calculateGraphNow) {
                loadAverageHourlyUsage(startDate, endDate)
            } else {
                List(24) { 0L }
            }
            
            val comparisonStartMillis = comparisonStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
            val comparisonEndMillis = comparisonEndDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
            val allInterventions = interventionLogRepository.getAllLogsList()
            val interventions = allInterventions.count { it.timestamp in startMillis..endMillis }
            val comparisonInterventions = allInterventions.count { it.timestamp in comparisonStartMillis..comparisonEndMillis }
            
            val socialMediaUsage = topApps
                .filter { socialMediaPackages.contains(it.packageName) }
                .sumOf { it.usageDurationMillis }
            val comparisonSocialMediaUsage = comparisonApps
                .filter { socialMediaPackages.contains(it.packageName) }
                .sumOf { it.usageDurationMillis }
            val longestSession = longestSessionInRange(startDate, endDate)
            val comparisonLongestSession = longestSessionInRange(comparisonStartDate, comparisonEndDate)

            val limit = if (period == AnalyticsPeriod.DAILY) {
                adaptiveLimitRepository.getLimitForDate(cappedDate.toString())?.calculatedLimitMinutes
            } else null

            val successState = AnalyticsUiState.Success(
                selectedDate = cappedDate,
                selectedPeriod = period,
                summary = aggregatedSummary,
                comparisonSummary = comparisonSummary,
                topApps = topApps,
                hourlyUsage = hourlyUsageList.toImmutableList(),
                isGraphCalculated = calculateGraphNow,
                interventionCount = interventions,
                comparisonInterventionCount = comparisonInterventions,
                socialMediaUsageMillis = socialMediaUsage,
                comparisonSocialMediaUsageMillis = comparisonSocialMediaUsage,
                longestSessionMillis = longestSession,
                comparisonLongestSessionMillis = comparisonLongestSession,
                adaptiveLimitMinutes = limit
            )

            successState.copy(funFact = generateFunFact(successState))
        }.onSuccess { _uiState.value = it }
            .onFailure {
                if (it is CancellationException) throw it
                _uiState.value = AnalyticsUiState.Error(it.message ?: "Tidak dapat memuat analytics")
            }
        }
    }

    fun calculateGraph() {
        val state = _uiState.value as? AnalyticsUiState.Success ?: return
        if (state.isCalculatingGraph) return
        val selectedDate = state.selectedDate
        val selectedPeriod = state.selectedPeriod
        _uiState.value = state.copy(isCalculatingGraph = true, graphError = null)

        graphJob?.cancel()
        graphJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val today = LocalDate.now()
                val (startDate, endDate) = when (selectedPeriod) {
                    AnalyticsPeriod.DAILY -> selectedDate to selectedDate
                    AnalyticsPeriod.WEEKLY -> selectedDate.minusDays(6) to selectedDate
                    AnalyticsPeriod.MONTHLY -> selectedDate.withDayOfMonth(1) to
                        minOf(selectedDate.withDayOfMonth(selectedDate.lengthOfMonth()), today)
                }
                val hourlyUsage = loadAverageHourlyUsage(startDate, endDate).toImmutableList()
                _uiState.update { current ->
                    if (current is AnalyticsUiState.Success &&
                        current.selectedDate == selectedDate &&
                        current.selectedPeriod == selectedPeriod
                    ) {
                        current.copy(
                            hourlyUsage = hourlyUsage,
                            isGraphCalculated = true,
                            isCalculatingGraph = false,
                            graphError = null,
                        )
                    } else current
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _uiState.update { current ->
                    if (current is AnalyticsUiState.Success &&
                        current.selectedDate == selectedDate &&
                        current.selectedPeriod == selectedPeriod
                    ) {
                        current.copy(
                            isCalculatingGraph = false,
                            graphError = context.getString(R.string.analytics_graph_failed),
                        )
                    } else current
                }
            }
        }
    }

    private fun aggregateSummary(date: LocalDate, history: List<DailySummary>): DailySummary {
        val activeDays = history.count { it.totalScreenTimeMillis > 0 }.toLong().coerceAtLeast(1L)
        return DailySummary(
            date = date,
            totalScreenTimeMillis = history.sumOf { it.totalScreenTimeMillis } / activeDays,
            totalScreenOnMillis = history.sumOf { it.totalScreenOnMillis } / activeDays,
            monitoredUsageMillis = history.sumOf { it.monitoredUsageMillis } / activeDays,
            unlockCount = (history.sumOf { it.unlockCount } / activeDays).toInt(),
            mostUsedApp = history.groupBy { it.mostUsedApp }.filter { it.key != null }
                .maxByOrNull { it.value.size }?.key,
            wellbeingScore = history.mapNotNull { it.wellbeingScore?.toDouble() }
                .filter { it > 0 }.let { if (it.isEmpty()) 0 else it.average().toInt() }
        )
    }

    private suspend fun loadAverageAppUsage(
        startDate: LocalDate,
        endDate: LocalDate,
        divisor: Int,
    ): List<AppUsage> {
        val apps = mutableMapOf<String, AppUsage>()
        var date = startDate
        while (!date.isAfter(endDate)) {
            usageRepository.getDailyUsage(date).first().forEach { app ->
                val existing = apps[app.packageName]
                apps[app.packageName] = existing?.copy(
                    usageDurationMillis = existing.usageDurationMillis + app.usageDurationMillis
                ) ?: app
            }
            date = date.plusDays(1)
        }
        return apps.values.map { app ->
            app.copy(usageDurationMillis = app.usageDurationMillis / divisor)
        }
    }

    private fun longestSessionInRange(startDate: LocalDate, endDate: LocalDate): Long {
        var date = startDate
        var longest = 0L
        while (!date.isAfter(endDate)) {
            longest = maxOf(longest, usageEventCollector.getLongestSessionForDay(date))
            date = date.plusDays(1)
        }
        return longest
    }

    private fun loadAverageHourlyUsage(startDate: LocalDate, endDate: LocalDate): List<Long> {
        val totals = LongArray(24)
        var daysWithUsage = 0
        var date = startDate
        while (!date.isAfter(endDate)) {
            val dailyUsage = usageEventCollector.getHourlyUsageForDay(date)
            if (dailyUsage.any { it > 0L }) {
                daysWithUsage++
                dailyUsage.forEachIndexed { hour, usage -> totals[hour] += usage }
            }
            date = date.plusDays(1)
        }
        val divisor = daysWithUsage.coerceAtLeast(1)
        return totals.map { it / divisor }
    }

    fun updateAppUsage(packageName: String, durationMillis: Long) {
        val state = _uiState.value as? AnalyticsUiState.Success ?: return
        if (state.selectedPeriod != AnalyticsPeriod.DAILY) return

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                usageRepository.updateAppScreenTime(packageName, state.selectedDate, durationMillis)
            }.onSuccess {
                load(state.selectedDate, AnalyticsPeriod.DAILY)
            }.onFailure {
                _uiState.value = AnalyticsUiState.Error(
                    context.getString(R.string.analytics_edit_usage_failed)
                )
            }
        }
    }
}
