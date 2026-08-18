package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Retrieves the 7 most recent valid historical observations (screen time > 0)
 * within the last 14 days, excluding today.
 */
class GetAdaptiveHistoryUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    suspend operator fun invoke(): List<Double> {
        val today = LocalDate.now()
        val startDate = today.minusDays(14)
        val endDate = today.minusDays(1)
        
        val history = usageRepository.getUsageHistory(startDate, endDate).first()
        
        return history
            .filter { it.totalScreenTimeMillis > 0 }
            .sortedByDescending { it.date }
            .take(7)
            .map { it.totalScreenTimeMillis / 1000.0 / 60.0 }
    }
}
