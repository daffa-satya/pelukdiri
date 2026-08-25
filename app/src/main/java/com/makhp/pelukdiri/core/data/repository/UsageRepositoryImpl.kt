package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.collector.UsageEventCollector
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
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val dao: UsageDao,
    private val appUsageCollector: AppUsageCollector,
    private val usageEventCollector: UsageEventCollector,
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

        val today = LocalDate.now()
        reconstructAndSave(today)
        
        userPreferencesRepository.setLastSyncedTimestamp(System.currentTimeMillis())
    }

    override suspend fun executeFullBackfill(daysHistory: Int, force: Boolean) = withContext(Dispatchers.IO) {
        if (!appUsageCollector.isPermissionGranted()) return@withContext

        // For validation phase, we force backfill
        val today = LocalDate.now()
        for (i in 1..daysHistory) {
            val targetDate = today.minusDays(i.toLong())
            reconstructAndSave(targetDate)
        }

        userPreferencesRepository.setHistoryBackfilled(true)
    }

    private suspend fun reconstructAndSave(date: LocalDate) {
        val usageList = usageEventCollector.getUsageForDay(date)
        saveUsageData(usageList, date.toString())
    }

    private suspend fun saveUsageData(usageList: List<AppUsage>, dateStr: String) {
        val entities = usageList.map { it.toEntity(dateStr) }

        // Calculate daily summary
        val totalScreenTime = usageList.sumOf { it.usageDurationMillis }
        val mostUsedApp = usageList.maxByOrNull { it.usageDurationMillis }?.appName

        val monitoredPackages = userPreferencesRepository.monitoredPackages.first()
        val monitoredUsage = usageList
            .filter { it.packageName in monitoredPackages }
            .sumOf { it.usageDurationMillis }

        val existingSummary = dao.getDailySummary(dateStr).firstOrNull()
        val totalScreenOnMillis = usageEventCollector.getScreenOnMillisForDay(LocalDate.parse(dateStr))
        val newSummary = DailySummaryEntity(
            date = dateStr,
            totalScreenTimeMillis = totalScreenTime,
            totalScreenOnMillis = totalScreenOnMillis,
            monitoredUsageMillis = monitoredUsage,
            unlockCount = existingSummary?.unlockCount ?: 0,
            mostUsedApp = mostUsedApp,
            wellbeingScore = existingSummary?.wellbeingScore
        )
        
        // Bulk insert with transaction
        dao.saveUsageDataWithSummary(entities, newSummary)
    }
    override suspend fun updateAppScreenTime(packageName: String, date: LocalDate, newScreenTimeMillis: Long) {
        require(!date.isAfter(LocalDate.now())) { "Future usage cannot be edited" }
        require(newScreenTimeMillis in 0L..24L * 60L * 60L * 1000L) {
            "Usage must be between 0 and 24 hours"
        }
        dao.updateAppUsageAndSummary(
            date = date.toString(),
            packageName = packageName,
            newDuration = newScreenTimeMillis,
            monitoredPackages = userPreferencesRepository.monitoredPackages.first(),
        )
    }
}
