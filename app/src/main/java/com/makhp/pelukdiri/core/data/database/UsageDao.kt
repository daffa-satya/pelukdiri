package com.makhp.pelukdiri.core.data.database

import androidx.room.*
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
}
