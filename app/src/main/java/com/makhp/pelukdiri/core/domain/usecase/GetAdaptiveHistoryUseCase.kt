package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.model.HistoricalConfig
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.time.SystemTimeProvider
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Retrieves the 14 most recent valid historical observations (screen time > 0)
 * within the last 21 days, excluding today.
 */
class GetAdaptiveHistoryUseCase @Inject constructor(
    private val usageRepository: UsageRepository,
    private val timeProvider: TimeProvider = SystemTimeProvider()
) {
    suspend operator fun invoke(): List<Double> {
        val today = timeProvider.today()
        val startDate = today.minusDays(HistoricalConfig.CALENDAR_LOOKBACK_DAYS)
        val endDate = today.minusDays(1)
        
        val history = usageRepository.getUsageHistory(startDate, endDate).first()
        
        return history
            .filter { it.monitoredUsageMillis > 0 }
            .sortedByDescending { it.date }
            .take(HistoricalConfig.HISTORY_SAMPLE_DAYS)
            .map { it.monitoredUsageMillis / 1000.0 / 60.0 }
    }
}
