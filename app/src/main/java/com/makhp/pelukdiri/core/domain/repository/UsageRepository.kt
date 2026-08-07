package com.makhp.pelukdiri.core.domain.repository

import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface UsageRepository {
    fun getDailyUsage(date: LocalDate): Flow<List<AppUsage>>
    fun getDailySummary(date: LocalDate): Flow<DailySummary?>
    fun getUsageHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<DailySummary>>
    suspend fun refreshUsageData()
    suspend fun syncRecentEventsOnly()
    suspend fun executeFullBackfill(daysHistory: Int = 7, force: Boolean = false)
}
