package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.engine.AdaptiveLimitGenerator
import com.makhp.pelukdiri.core.domain.engine.DeviationEngine
import com.makhp.pelukdiri.core.domain.model.AdaptiveLimitResult
import com.makhp.pelukdiri.core.domain.model.DailyAdaptiveLimit
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

class InitializeDailyAdaptiveLimitUseCase @Inject constructor(
    private val getAdaptiveHistoryUseCase: GetAdaptiveHistoryUseCase,
    private val adaptiveLimitRepository: AdaptiveLimitRepository,
    private val deviationEngine: DeviationEngine,
    private val adaptiveLimitGenerator: AdaptiveLimitGenerator
) {
    suspend operator fun invoke() {
        val today = LocalDate.now()
        val dateStr = today.toString()

        // 1. Idempotency Check
        val existingLimit = adaptiveLimitRepository.getLimitForDate(dateStr)
        if (existingLimit != null) return

        // 2. Fetch last 14 days of history (7 valid observations)
        val recent7 = getAdaptiveHistoryUseCase()

        // 3. 7-of-14 Selection check
        if (recent7.size < 7) {
            // Insufficient history - we do not persist a personalized limit
            return
        }

        // 4. Engine Execution
        val deviationResult = deviationEngine.calculate(0.0, recent7) // currentUsage is 0 for initial calc
        val result = adaptiveLimitGenerator.generateInitialLimit(deviationResult)

        // 6. Atomic Persistence
        if (result is AdaptiveLimitResult.Personalized) {
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
