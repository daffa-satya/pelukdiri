package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.SystemTimeProvider
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import javax.inject.Inject

sealed interface BypassResult {
    data class Success(val remaining: Int) : BypassResult
    data object Exhausted : BypassResult
}

class PerformEmergencyBypassUseCase @Inject constructor(
    private val interventionLogRepository: InterventionLogRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {
    suspend operator fun invoke(
        deviation: Double,
        difficultyControlSignal: Double,
        difficulty: Int,
        penaltyMinutes: Int,
        responseTimeMs: Long
    ): BypassResult {
        val today = timeProvider.today()
        val startOfDay = today.atStartOfDay(timeProvider.zoneId()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(timeProvider.zoneId()).toInstant().toEpochMilli()

        // 1. Backend Enforcement (Check daily limit)
        val currentCount = interventionLogRepository.getBypassCountForDay(startOfDay, endOfDay)

        if (currentCount >= 5) {
            return BypassResult.Exhausted
        }

        val currentTimeMs = timeProvider.nowMillis()
        val bypassUntil = currentTimeMs + 180_000L // Exactly 3 minutes

        // 2. Persistent State Updates

        // Log as bypassed
        interventionLogRepository.insertLog(
            InterventionLog(
                timestamp = currentTimeMs,
                deviation = deviation,
                difficultyControlSignal = difficultyControlSignal,
                difficultyLevel = difficulty,
                responseTimeMs = responseTimeMs,
                isSuccess = false,
                isBypassed = true,
                penaltyAppliedMinutes = penaltyMinutes
            )
        )

        // Set bypass window
        userPreferencesRepository.setEmergencyBypassUntil(bypassUntil)
        
        // Restore eligibility immediately after bypass expires
        userPreferencesRepository.setNextEligibleInterventionAt(bypassUntil)

        return BypassResult.Success(remaining = 5 - (currentCount + 1))
    }
}
