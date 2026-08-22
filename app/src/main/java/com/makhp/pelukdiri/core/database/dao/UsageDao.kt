package com.makhp.pelukdiri.core.database.dao

import androidx.room.*
import com.makhp.pelukdiri.core.database.entity.AppUsageEntity
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM app_usage WHERE date = :date")
    fun getAppUsageByDate(date: String): Flow<List<AppUsageEntity>>

    @Query("UPDATE app_usage SET usageDurationMillis = :newDuration WHERE date = :date AND packageName = :packageName")
    suspend fun updateAppUsageDuration(date: String, packageName: String, newDuration: Long)

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
        newDuration: Long,
        monitoredPackages: Set<String>,
    ) {
        val currentUsage = getAppUsageByDateList(date)
        require(currentUsage.any { it.packageName == packageName }) { "Usage row not found" }
        val updatedUsage = currentUsage.map {
            if (it.packageName == packageName) it.copy(usageDurationMillis = newDuration) else it
        }
        require(updatedUsage.sumOf { it.usageDurationMillis } <= 24L * 60L * 60L * 1000L) {
            "Daily usage cannot exceed 24 hours"
        }

        updateAppUsageDuration(date, packageName, newDuration)
        val existingSummary = getDailySummaryOnce(date)
        if (existingSummary != null) {
            insertDailySummary(
                existingSummary.copy(
                    totalScreenTimeMillis = updatedUsage.sumOf { it.usageDurationMillis },
                    monitoredUsageMillis = updatedUsage
                        .filter { it.packageName in monitoredPackages }
                        .sumOf { it.usageDurationMillis },
                    mostUsedApp = updatedUsage.maxByOrNull { it.usageDurationMillis }?.appName,
                )
            )
        }
    }

    @Query("SELECT * FROM app_usage ORDER BY date DESC")
    suspend fun getAllAppUsageList(): List<AppUsageEntity>

    @Query("SELECT * FROM daily_summary ORDER BY date DESC")
    suspend fun getAllDailySummariesList(): List<DailySummaryEntity>
}
