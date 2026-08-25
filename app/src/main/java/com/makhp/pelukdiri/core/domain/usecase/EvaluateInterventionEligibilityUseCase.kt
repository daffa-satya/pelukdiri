package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.collector.UsageEventCollector
import com.makhp.pelukdiri.core.domain.engine.ControlEngine
import com.makhp.pelukdiri.core.domain.engine.DeviationEngine
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeSelector
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.core.domain.model.InterventionDecision
import com.makhp.pelukdiri.core.domain.model.InterventionDecisionAudit
import com.makhp.pelukdiri.core.domain.model.InterventionDecisionReason
import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.DeviationResult
import com.makhp.pelukdiri.core.domain.model.DifficultyHistoryEntry
import com.makhp.pelukdiri.core.domain.model.PerformanceMetrics
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.InterventionDecisionRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.SystemTimeProvider
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import android.util.Log
import java.time.LocalTime
import javax.inject.Inject

class EvaluateInterventionEligibilityUseCase @Inject constructor(
    private val usageEventCollector: UsageEventCollector,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getAdaptiveHistoryUseCase: GetAdaptiveHistoryUseCase,
    private val deviationEngine: DeviationEngine,
    private val controlEngine: ControlEngine,
    private val interventionLogRepository: InterventionLogRepository,
    private val interventionDecisionRepository: InterventionDecisionRepository,
    private val challengeSelector: InterventionChallengeSelector,
    private val appUsageCollector: AppUsageCollector,
    private val lockManager: com.makhp.pelukdiri.core.domain.InterventionLockManager,
    private val controlConfig: ControlConfig,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {
    suspend operator fun invoke(packageName: String): InterventionDecision {
        val currentTimeMs = timeProvider.nowMillis()
        return try {
            evaluate(packageName, currentTimeMs)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val currentDifficulty = try {
                userPreferencesRepository.currentDifficulty.first()
            } catch (_: Exception) {
                -1
            }
            val lux = try {
                appUsageCollector.getCurrentAmbientLightLux()
            } catch (_: Exception) {
                Float.NaN
            }
            audit(
                decision = InterventionDecision(false, null, 0.0, 0.0, lux),
                timestamp = currentTimeMs,
                packageName = packageName,
                currentDifficulty = currentDifficulty,
                reason = InterventionDecisionReason.EVALUATION_ERROR,
                errorType = error.javaClass.simpleName,
            )
            throw error
        }
    }

    private suspend fun evaluate(packageName: String, currentTimeMs: Long): InterventionDecision {
        val today = timeProvider.today()
        val currentDifficulty = userPreferencesRepository.currentDifficulty.first()

        // 0. Quick check for active intervention lock
        if (lockManager.isLocked.value) {
            val lux = appUsageCollector.getCurrentAmbientLightLux()
            return audit(
                decision = InterventionDecision(
                shouldTrigger = false,
                controlResult = null,
                monitoredUsageMinutes = 0.0,
                totalUsageMinutes = 0.0,
                ambientLux = lux,
                ),
                timestamp = currentTimeMs,
                packageName = packageName,
                currentDifficulty = currentDifficulty,
                reason = InterventionDecisionReason.ACTIVE_LOCK,
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
            return audit(
                decision = InterventionDecision(
                shouldTrigger = false,
                controlResult = null,
                monitoredUsageMinutes = currentMonitoredUsageMinutes,
                totalUsageMinutes = currentTotalUsageMinutes,
                ambientLux = appUsageCollector.getCurrentAmbientLightLux(),
                ),
                timestamp = currentTimeMs,
                packageName = packageName,
                currentDifficulty = currentDifficulty,
                reason = InterventionDecisionReason.PACKAGE_NOT_MONITORED,
            )
        }
        
        // 2. Cooldown & Quota check
        val nextEligible = userPreferencesRepository.nextEligibleInterventionAt.first()
        val bypassUntil = userPreferencesRepository.emergencyBypassUntil.first()
        
        if (currentTimeMs < nextEligible || currentTimeMs < bypassUntil) {
            val bypassActive = currentTimeMs < bypassUntil
            return audit(
                decision = InterventionDecision(
                    shouldTrigger = false,
                    controlResult = null,
                    monitoredUsageMinutes = currentMonitoredUsageMinutes,
                    totalUsageMinutes = currentTotalUsageMinutes,
                    ambientLux = appUsageCollector.getCurrentAmbientLightLux(),
                ),
                timestamp = currentTimeMs,
                packageName = packageName,
                currentDifficulty = currentDifficulty,
                reason = if (bypassActive) {
                    InterventionDecisionReason.BYPASS_ACTIVE
                } else {
                    InterventionDecisionReason.COOLDOWN_ACTIVE
                },
                nextEligibleAt = if (bypassActive) bypassUntil else nextEligible,
            )
        }
        
        // 3. Deviation
        val history = getAdaptiveHistoryUseCase()
        val devResult = deviationEngine.calculate(currentMonitoredUsageMinutes, history)
        
        // 4. Performance & Sensitivity
        val recentLogs = interventionLogRepository.getRecentLogs(PERFORMANCE_RUN_QUERY_LIMIT)
        val difficultyHistory = recentLogs.map {
            DifficultyHistoryEntry(
                difficulty = it.difficultyLevel,
                isValidResponse = !it.isBypassed && it.responseTimeMs > 0L,
            )
        }
        val challengeType = challengeSelector.select()
        val currentDifficultyRun = recentLogs
            .takeWhile { it.difficultyLevel == currentDifficulty }
            .filter { it.challengeType == challengeType }
            .filter { !it.isBypassed && it.responseTimeMs > 0L }
        val latestLog = currentDifficultyRun.firstOrNull()
        val lastPerformance = latestLog?.let { 
            PerformanceMetrics(
                responseTimeMs = it.responseTimeMs, 
                isSuccess = it.isSuccess, 
                difficulty = it.difficultyLevel
            ) 
        }
        val perfHistory = currentDifficultyRun
            .drop(1)
            .take(controlConfig.performanceEvidenceWindow)
            .takeWhile { it.isSuccess }
            .map { it.responseTimeMs }
        val consecutiveFailures = currentDifficultyRun
            .takeWhile { !it.isSuccess }
            .take(controlConfig.difficultyDecreaseEvidenceWindow)
            .count()
            
        val lux = appUsageCollector.getCurrentAmbientLightLux()
        val bedtime = userPreferencesRepository.bedtime.first()?.let { 
            try { LocalTime.parse(it) } catch (_: Exception) { null }
        }
        val wakeTime = userPreferencesRepository.wakeTime.first()?.let { 
            try { LocalTime.parse(it) } catch (_: Exception) { null }
        }
        
        // 5. Control Decision
        val controlResult = controlEngine.calculateNextIntervention(
            deviation = devResult.deviation,
            lastPerformance = lastPerformance,
            performanceHistory = perfHistory,
            lux = lux,
            bedtime = bedtime,
            wakeTime = wakeTime,
            currentLevel = currentDifficulty,
            timestampMs = currentTimeMs,
            difficultyHistory = difficultyHistory,
            consecutiveFailures = consecutiveFailures,
        )
        
        val shouldSchedule = nextEligible <= 0L
        val shouldTrigger = !shouldSchedule
        if (shouldSchedule) {
            userPreferencesRepository.setNextEligibleInterventionAt(controlResult.nextEligibleInterventionAt)
        }
        val decision = InterventionDecision(
            shouldTrigger = shouldTrigger,
            controlResult = controlResult,
            monitoredUsageMinutes = currentMonitoredUsageMinutes,
            totalUsageMinutes = currentTotalUsageMinutes,
            ambientLux = lux,
            challengeType = challengeType,
        )
        return audit(
            decision = decision,
            timestamp = currentTimeMs,
            packageName = packageName,
            currentDifficulty = currentDifficulty,
            reason = if (shouldSchedule) {
                InterventionDecisionReason.INTERVAL_SCHEDULED
            } else {
                InterventionDecisionReason.TRIGGERED
            },
            historyCount = history.size,
            deviationResult = devResult,
            challengeType = challengeType,
        )
    }

    private suspend fun audit(
        decision: InterventionDecision,
        timestamp: Long,
        packageName: String,
        currentDifficulty: Int,
        reason: InterventionDecisionReason,
        historyCount: Int = 0,
        deviationResult: DeviationResult? = null,
        challengeType: InterventionChallengeType? = null,
        nextEligibleAt: Long? = decision.controlResult?.nextEligibleInterventionAt,
        errorType: String? = null,
    ): InterventionDecision {
        val control = decision.controlResult
        try {
            interventionDecisionRepository.insert(
                InterventionDecisionAudit(
                    timestamp = timestamp,
                    packageName = packageName,
                    monitoredUsageMinutes = decision.monitoredUsageMinutes,
                    totalUsageMinutes = decision.totalUsageMinutes,
                    ambientLux = decision.ambientLux,
                    historyCount = historyCount,
                    baselineMedianMinutes = deviationResult?.baseline,
                    madMinutes = deviationResult?.mad,
                    deviationSignal = deviationResult?.signal,
                    relativeDeviation = deviationResult?.relativeDeviation,
                    relativeMagnitude = deviationResult?.relativeMagnitude,
                    deviation = deviationResult?.deviation,
                    performance = control?.performance,
                    qLux = control?.qLux,
                    qTime = control?.qTime,
                    sensitivity = control?.sensitivity,
                    difficultyControl = control?.difficultyControl,
                    difficultyControlSignal = control?.normalizedDifficultyControl,
                    difficultyTarget = control?.difficultyTarget,
                    currentDifficulty = currentDifficulty,
                    nextDifficulty = control?.nextDifficulty,
                    challengeType = challengeType,
                    frequencyControl = control?.frequencyControl,
                    normalizedFrequencyControl = control?.normalizedFrequencyControl,
                    proposedIntervalMinutes = control?.intervalMinutes,
                    nextEligibleAt = nextEligibleAt,
                    shouldTrigger = decision.shouldTrigger,
                    reason = reason,
                    controlMode = control?.mode,
                    errorType = errorType,
                )
            )
        } catch (_: Exception) {
            Log.e("EligibilityUseCase", "Unable to persist intervention decision audit")
        }
        return decision
    }

    private companion object {
        const val PERFORMANCE_RUN_QUERY_LIMIT = 32
    }
}
