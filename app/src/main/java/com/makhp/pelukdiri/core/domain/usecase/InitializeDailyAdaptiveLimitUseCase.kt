package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.engine.AdaptiveLimitGenerator
import com.makhp.pelukdiri.core.domain.engine.DeviationEngine
import com.makhp.pelukdiri.core.domain.model.AdaptiveLimitResult
import com.makhp.pelukdiri.core.domain.model.DailyAdaptiveLimit
import com.makhp.pelukdiri.core.domain.model.HistoricalConfig
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import java.time.LocalDate
import javax.inject.Inject

class InitializeDailyAdaptiveLimitUseCase @Inject constructor(
    private val getAdaptiveHistoryUseCase: GetAdaptiveHistoryUseCase,
    private val adaptiveLimitRepository: AdaptiveLimitRepository,
    private val deviationEngine: DeviationEngine,
    private val adaptiveLimitGenerator: AdaptiveLimitGenerator
) {
    suspend operator fun invoke(force: Boolean = false) {
        val today = LocalDate.now()
        val dateStr = today.toString()

        // 1. Idempotency Check
        val existingLimit = adaptiveLimitRepository.getLimitForDate(dateStr)
        if (existingLimit != null && !force) return

        // 2. Fetch the globally configured valid historical sample.
        val recent = getAdaptiveHistoryUseCase().take(HistoricalConfig.HISTORY_SAMPLE_DAYS)

        // 3. Selection check
        if (recent.size < HistoricalConfig.MINIMUM_HISTORY_DAYS) {
            // Insufficient history - we do not persist a personalized limit
            return
        }

        // 4. Engine Execution
        val deviationResult = deviationEngine.calculate(0.0, recent) // currentUsage is 0 for initial calc
        val result = adaptiveLimitGenerator.generateInitialLimit(deviationResult)

        // 6. Atomic Persistence
        if (result is AdaptiveLimitResult.Personalized) {
            if (force && existingLimit != null) {
                adaptiveLimitRepository.updateCalculatedLimit(dateStr, result.limitMinutes)
            } else {
                adaptiveLimitRepository.insertInitialLimit(
                    DailyAdaptiveLimit(
                        dateString = dateStr,
                        calculatedLimitMinutes = result.limitMinutes,
                        actualScreenTimeMinutes = 0,
                        reclaimedTimeMinutes = 0
                    )
                )
            }
        }
    }
}
