package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.SystemTimeProvider
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
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
        responseTimeMs: Long,
        challengeType: InterventionChallengeType = InterventionChallengeType.MATH,
    ): BypassResult {
        val today = timeProvider.today()
        val startOfDay = today.atStartOfDay(timeProvider.zoneId()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(timeProvider.zoneId()).toInstant().toEpochMilli()

        val currentTimeMs = timeProvider.nowMillis()
        val bypassUntil = currentTimeMs + 180_000L // Exactly 3 minutes

        // Quota enforcement and the append-only audit row share one Room transaction.
        val remaining = interventionLogRepository.insertBypassIfQuotaAvailable(
            InterventionLog(
                timestamp = currentTimeMs,
                deviation = deviation,
                difficultyControlSignal = difficultyControlSignal,
                difficultyLevel = difficulty,
                responseTimeMs = responseTimeMs,
                isSuccess = false,
                isBypassed = true,
                penaltyAppliedMinutes = penaltyMinutes,
                challengeType = challengeType,
            ),
            startOfDay = startOfDay,
            endOfDay = endOfDay,
            limit = DAILY_BYPASS_LIMIT,
        ) ?: return BypassResult.Exhausted

        // Set bypass window
        userPreferencesRepository.setEmergencyBypassUntil(bypassUntil)
        
        // Restore eligibility immediately after bypass expires
        userPreferencesRepository.setNextEligibleInterventionAt(bypassUntil)

        return BypassResult.Success(remaining = remaining)
    }

    private companion object {
        const val DAILY_BYPASS_LIMIT = 5
    }
}
