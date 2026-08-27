package com.makhp.pelukdiri.core.database.dao

import androidx.room.*
import com.makhp.pelukdiri.core.database.entity.AppUsageEntity
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

private const val MAX_DAILY_USAGE_MILLIS = 24L * 60L * 60L * 1000L

@Dao
interface UsageDao {
    @Query("SELECT * FROM app_usage WHERE date = :date")
    fun getAppUsageByDate(date: String): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date = :date")
    suspend fun getAppUsageByDateList(date: String): List<AppUsageEntity>

    @Query("SELECT * FROM daily_summary WHERE date = :date")
    suspend fun getDailySummaryOnce(date: String): DailySummaryEntity?

    @Query("SELECT * FROM daily_summary WHERE date = :date")
    fun getDailySummary(date: String): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :startDate AND :endDate")
    fun getSummaryHistory(startDate: String, endDate: String): Flow<List<DailySummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailySummaryEntity)

    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun deleteAppUsageByDate(date: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(usage: List<AppUsageEntity>)

    @Transaction
    suspend fun saveUsageDataWithSummary(usage: List<AppUsageEntity>, summary: DailySummaryEntity) {
        deleteAppUsageByDate(summary.date)
        insertAppUsage(usage)
        insertDailySummary(summary)
    }

    @Transaction
    suspend fun updateAppUsageAndSummary(
        date: String,
        packageName: String,
        appName: String,
        newDuration: Long,
        monitoredPackages: Set<String>,
        screenOnMillisForNewSummary: Long,
    ) {
        val currentUsage = getAppUsageByDateList(date).toMutableList()
        val existingIndex = currentUsage.indexOfFirst { it.packageName == packageName }

        if (existingIndex != -1) {
            currentUsage[existingIndex] = currentUsage[existingIndex].copy(usageDurationMillis = newDuration)
        } else {
            currentUsage.add(
                AppUsageEntity(
                    packageName = packageName,
                    appName = appName,
                    usageDurationMillis = newDuration,
                    // A manual duration does not provide an authoritative last-used instant.
                    lastUsedTimestamp = 0L,
                    date = date
                )
            )
        }

        val totalScreenTime = currentUsage.sumOf { it.usageDurationMillis }
        require(totalScreenTime <= MAX_DAILY_USAGE_MILLIS) {
            "Daily usage cannot exceed 24 hours"
        }
        val monitoredUsage = currentUsage
            .filter { it.packageName in monitoredPackages }
            .sumOf { it.usageDurationMillis }
        val mostUsed = currentUsage.maxByOrNull { it.usageDurationMillis }?.appName

        val existingSummary = getDailySummaryOnce(date)
        val newSummary = if (existingSummary != null) {
            existingSummary.copy(
                totalScreenTimeMillis = totalScreenTime,
                monitoredUsageMillis = monitoredUsage,
                mostUsedApp = mostUsed,
            )
        } else {
            DailySummaryEntity(
                date = date,
                totalScreenTimeMillis = totalScreenTime,
                totalScreenOnMillis = screenOnMillisForNewSummary,
                monitoredUsageMillis = monitoredUsage,
                unlockCount = 0,
                mostUsedApp = mostUsed,
                wellbeingScore = null
            )
        }

        insertAppUsage(currentUsage)
        insertDailySummary(newSummary)
    }

    @Query("SELECT * FROM app_usage ORDER BY date DESC")
    suspend fun getAllAppUsageList(): List<AppUsageEntity>

    @Query("SELECT * FROM daily_summary ORDER BY date DESC")
    suspend fun getAllDailySummariesList(): List<DailySummaryEntity>
}
