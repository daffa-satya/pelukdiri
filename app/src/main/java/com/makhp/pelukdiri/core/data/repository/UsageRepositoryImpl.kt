package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.core.data.database.UsageDao
import com.makhp.pelukdiri.core.data.mapper.toDomainModel
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val dao: UsageDao
) : UsageRepository {
    override fun getDailyUsage(date: LocalDate): Flow<List<AppUsage>> {
        return dao.getAppUsageByDate(date.toString()).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getDailySummary(date: LocalDate): Flow<DailySummary?> {
        return dao.getDailySummary(date.toString()).map { it?.toDomainModel() }
    }

    override fun getUsageHistory(startDate: LocalDate, endDate: LocalDate): Flow<List<DailySummary>> {
        return dao.getSummaryHistory(startDate.toString(), endDate.toString()).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun refreshUsageData() {
        // This will be called to trigger data collection via WorkManager or direct collection
        // For now it's a stub
    }
}
