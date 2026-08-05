package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import com.makhp.pelukdiri.core.data.mapper.toDomainModel
import com.makhp.pelukdiri.core.data.mapper.toEntity
import com.makhp.pelukdiri.core.domain.model.AppUsage
import com.makhp.pelukdiri.core.domain.model.DailySummary
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject

class UsageRepositoryImpl @Inject constructor(
    private val dao: UsageDao,
    private val appUsageCollector: AppUsageCollector
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
        if (!appUsageCollector.isPermissionGranted()) return@withContext

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val todayStr = LocalDate.now().toString()

        val recentUsage = appUsageCollector.fetchRecentEvents(startTime, now)
        if (recentUsage.isNotEmpty()) {
            val entities = recentUsage.map { it.toEntity(todayStr) }
            dao.insertAppUsage(entities)

            // Calculate and update daily summary
            val totalScreenTime = recentUsage.sumOf { it.usageDurationMillis }
            val mostUsedApp = recentUsage.maxByOrNull { it.usageDurationMillis }?.appName

            val existingSummary = dao.getDailySummary(todayStr).firstOrNull()
            val newSummary = DailySummaryEntity(
                date = todayStr,
                totalScreenTimeMillis = totalScreenTime,
                unlockCount = existingSummary?.unlockCount ?: 0,
                mostUsedApp = mostUsedApp,
                wellbeingScore = existingSummary?.wellbeingScore
            )
            dao.insertDailySummary(newSummary)
        }
    }
}
