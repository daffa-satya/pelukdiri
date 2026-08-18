package com.makhp.pelukdiri.core.database.dao

import androidx.room.*
import com.makhp.pelukdiri.core.database.entity.AppUsageEntity
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM app_usage WHERE date = :date")
    fun getAppUsageByDate(date: String): Flow<List<AppUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppUsage(usage: List<AppUsageEntity>)

    @Query("SELECT * FROM daily_summary WHERE date = :date")
    fun getDailySummary(date: String): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summary WHERE date BETWEEN :startDate AND :endDate")
    fun getSummaryHistory(startDate: String, endDate: String): Flow<List<DailySummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySummary(summary: DailySummaryEntity)

    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun deleteAppUsageByDate(date: String)

    @Transaction
    suspend fun saveUsageDataWithSummary(usage: List<AppUsageEntity>, summary: DailySummaryEntity) {
        deleteAppUsageByDate(summary.date)
        insertAppUsage(usage)
        insertDailySummary(summary)
    }

    @Query("SELECT * FROM app_usage ORDER BY date DESC")
    suspend fun getAllAppUsageList(): List<AppUsageEntity>

    @Query("SELECT * FROM daily_summary ORDER BY date DESC")
    suspend fun getAllDailySummariesList(): List<DailySummaryEntity>
}
