package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import com.makhp.pelukdiri.core.data.mapper.toDomainModel
import com.makhp.pelukdiri.core.data.mapper.toEntity
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val dao: UsageDao,
    private val appUsageCollector: AppUsageCollector,
    private val userPreferencesRepository: UserPreferencesRepository
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

    override suspend fun refreshUsageData() = withContext(Dispatchers.IO) {
        syncRecentEventsOnly()
    }

    override suspend fun syncRecentEventsOnly() = withContext(Dispatchers.IO) {
        if (!appUsageCollector.isPermissionGranted()) return@withContext

        val lastSynced = userPreferencesRepository.lastSyncedTimestamp.first()
        val recentUsage = appUsageCollector.syncRecentEventsOnly(lastSynced)

        if (recentUsage.isNotEmpty()) {
            val todayStr = LocalDate.now().toString()
            saveUsageData(recentUsage, todayStr)
            userPreferencesRepository.setLastSyncedTimestamp(System.currentTimeMillis())
        }
    }

    override suspend fun executeFullBackfill(daysHistory: Int, force: Boolean) = withContext(Dispatchers.IO) {
        if (!appUsageCollector.isPermissionGranted()) return@withContext

        val isBackfilled = userPreferencesRepository.isHistoryBackfilled.first()
        if (isBackfilled && !force) return@withContext

        val backfillData = appUsageCollector.executeFullBackfill(daysHistory)
        backfillData.forEach { (dateStr, usageList) ->
            saveUsageData(usageList, dateStr)
        }

        userPreferencesRepository.setHistoryBackfilled(true)
    }

    private suspend fun saveUsageData(usageList: List<AppUsage>, dateStr: String) {
        val entities = usageList.map { it.toEntity(dateStr) }

        // Calculate daily summary
        val totalScreenTime = usageList.sumOf { it.usageDurationMillis }
        val mostUsedApp = usageList.maxByOrNull { it.usageDurationMillis }?.appName

        val existingSummary = dao.getDailySummary(dateStr).firstOrNull()
        val newSummary = DailySummaryEntity(
            date = dateStr,
            totalScreenTimeMillis = totalScreenTime,
            unlockCount = existingSummary?.unlockCount ?: 0,
            mostUsedApp = mostUsedApp,
            wellbeingScore = existingSummary?.wellbeingScore
        )
        
        // Bulk insert with transaction
        dao.saveUsageDataWithSummary(entities, newSummary)
    }
}
