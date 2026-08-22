package com.makhp.pelukdiri.core.domain.repository

import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.core.domain.model.HistoricalConfig
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface UsageRepository {
    suspend fun updateAppScreenTime(packageName: String, date: LocalDate, newScreenTimeMillis: Long)
    fun getDailyUsage(date: LocalDate): Flow<List<AppUsage>>
    fun getDailySummary(date: LocalDate): Flow<DailySummary?>
    fun getUsageHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<DailySummary>>
    suspend fun refreshUsageData()
    suspend fun syncRecentEventsOnly()
    suspend fun executeFullBackfill(daysHistory: Int = HistoricalConfig.BACKFILL_DAYS, force: Boolean = false)
}
