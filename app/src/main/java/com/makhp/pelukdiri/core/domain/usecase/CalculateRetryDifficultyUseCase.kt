package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.engine.DifficultyController
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.DifficultyHistoryEntry
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import javax.inject.Inject

class CalculateRetryDifficultyUseCase @Inject constructor(
    private val interventionLogRepository: InterventionLogRepository,
    private val difficultyController: DifficultyController,
    private val controlConfig: ControlConfig,
) {
    suspend operator fun invoke(
        currentLevel: Int,
        challengeType: InterventionChallengeType,
        deviation: Double,
    ): Int {
        val recentLogs = interventionLogRepository.getRecentLogs(PERFORMANCE_RUN_QUERY_LIMIT)
        val currentDifficultyRun = recentLogs
            .takeWhile { it.difficultyLevel == currentLevel }
            .filter { it.challengeType == challengeType }
            .filter { !it.isBypassed && it.responseTimeMs > 0L }
        val consecutiveFailures = currentDifficultyRun
            .takeWhile { !it.isSuccess }
            .take(controlConfig.difficultyDecreaseEvidenceWindow)
            .count()

        return difficultyController.calculate(
            deviation = deviation,
            performance = 0.0,
            sensitivity = 0.0,
            currentLevel = currentLevel,
            insufficientEvidence = true,
            difficultyHistory = recentLogs.map {
                DifficultyHistoryEntry(
                    difficulty = it.difficultyLevel,
                    isValidResponse = !it.isBypassed && it.responseTimeMs > 0L,
                )
            },
            consecutiveFailures = consecutiveFailures,
            latestResponseFailed = currentDifficultyRun.firstOrNull()?.isSuccess == false,
        ).nextLevel
    }

    private companion object {
        const val PERFORMANCE_RUN_QUERY_LIMIT = 32
    }
}
