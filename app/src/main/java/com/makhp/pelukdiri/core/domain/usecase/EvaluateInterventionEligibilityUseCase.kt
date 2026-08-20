package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.collector.UsageEventCollector
import com.makhp.pelukdiri.core.domain.engine.ControlEngine
import com.makhp.pelukdiri.core.domain.engine.DeviationEngine
import com.makhp.pelukdiri.core.domain.model.InterventionDecision
import com.makhp.pelukdiri.core.domain.model.PerformanceMetrics
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.SystemTimeProvider
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class EvaluateInterventionEligibilityUseCase @Inject constructor(
    private val usageEventCollector: UsageEventCollector,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getAdaptiveHistoryUseCase: GetAdaptiveHistoryUseCase,
    private val deviationEngine: DeviationEngine,
    private val controlEngine: ControlEngine,
    private val interventionLogRepository: InterventionLogRepository,
    private val appUsageCollector: AppUsageCollector,
    private val lockManager: com.makhp.pelukdiri.core.domain.InterventionLockManager,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {
    suspend operator fun invoke(packageName: String): InterventionDecision {
        val currentTimeMs = timeProvider.nowMillis()
        val today = timeProvider.today()

        // 0. Quick check for active intervention lock
        if (lockManager.isLocked.value) {
            return InterventionDecision(
                shouldTrigger = false,
                controlResult = null,
                monitoredUsageMinutes = 0.0,
                totalUsageMinutes = 0.0,
                ambientLux = appUsageCollector.getCurrentAmbientLightLux()
            )
        }
        
        // 1. Authoritative measurement
        val usageList = usageEventCollector.getUsageForDay(today)
        val monitoredPackages = userPreferencesRepository.monitoredPackages.first()
        
        val totalUsageMillis = usageList.sumOf { it.usageDurationMillis }
        val monitoredUsageMillis = usageList
            .filter { it.packageName in monitoredPackages }
            .sumOf { it.usageDurationMillis }
            
        val currentMonitoredUsageMinutes = monitoredUsageMillis / 1000.0 / 60.0
        val currentTotalUsageMinutes = totalUsageMillis / 1000.0 / 60.0

        if (packageName !in monitoredPackages) {
            return InterventionDecision(
                shouldTrigger = false,
                controlResult = null,
                monitoredUsageMinutes = currentMonitoredUsageMinutes,
                totalUsageMinutes = currentTotalUsageMinutes,
                ambientLux = appUsageCollector.getCurrentAmbientLightLux()
            )
        }
        
        // 2. Cooldown & Quota check
        val nextEligible = userPreferencesRepository.nextEligibleInterventionAt.first()
        val bypassUntil = userPreferencesRepository.emergencyBypassUntil.first()
        
        if (currentTimeMs < nextEligible || currentTimeMs < bypassUntil) {
             return InterventionDecision(
                 shouldTrigger = false, 
                 controlResult = null, 
                 monitoredUsageMinutes = currentMonitoredUsageMinutes, 
                 totalUsageMinutes = currentTotalUsageMinutes, 
                 ambientLux = appUsageCollector.getCurrentAmbientLightLux()
             )
        }
        
        // 3. Deviation
        val history = getAdaptiveHistoryUseCase()
        val devResult = deviationEngine.calculate(currentMonitoredUsageMinutes, history)
        
        // 4. Performance & Sensitivity
        val currentDiff = userPreferencesRepository.currentDifficulty.first()
        val latestLog = interventionLogRepository.getLatestValidPerformanceLogByDifficulty(currentDiff)
        val lastPerformance = latestLog?.let { 
            PerformanceMetrics(
                responseTimeMs = it.responseTimeMs, 
                isSuccess = it.isSuccess, 
                difficulty = it.difficultyLevel
            ) 
        }
        val perfHistory = interventionLogRepository.getRecentValidSuccessfulLogsByDifficulty(currentDiff, 6)
            .filterNot { it.id == latestLog?.id }
            .take(5)
            .map { it.responseTimeMs }
            
        val lux = appUsageCollector.getCurrentAmbientLightLux()
        val bedtime = userPreferencesRepository.bedtime.first()?.let { 
            try { LocalTime.parse(it) } catch (e: Exception) { null } 
        }
        val wakeTime = userPreferencesRepository.wakeTime.first()?.let { 
            try { LocalTime.parse(it) } catch (e: Exception) { null } 
        }
        
        // 5. Control Decision
        val controlResult = controlEngine.calculateNextIntervention(
            deviation = devResult.deviation,
            lastPerformance = lastPerformance,
            performanceHistory = perfHistory,
            lux = lux,
            bedtime = bedtime,
            wakeTime = wakeTime,
            currentLevel = currentDiff,
            timestampMs = currentTimeMs
        )
        
        val shouldTrigger = devResult.deviation != null && devResult.deviation > 0.05
        
        return InterventionDecision(
            shouldTrigger = shouldTrigger,
            controlResult = controlResult,
            monitoredUsageMinutes = currentMonitoredUsageMinutes,
            totalUsageMinutes = currentTotalUsageMinutes,
            ambientLux = lux
        )
    }
}
