package com.makhp.pelukdiri.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.makhp.pelukdiri.core.database.entity.DailyAdaptiveLimitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdaptiveLimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLimit(limit: DailyAdaptiveLimitEntity)

    @Query("SELECT * FROM daily_adaptive_limits WHERE dateString = :date")
    suspend fun getLimitForDate(date: String): DailyAdaptiveLimitEntity?

    @Query("SELECT * FROM daily_adaptive_limits ORDER BY dateString DESC")
    fun getAllLimits(): Flow<List<DailyAdaptiveLimitEntity>>

    @Query("SELECT * FROM daily_adaptive_limits ORDER BY dateString ASC")
    suspend fun getAllLimitsList(): List<DailyAdaptiveLimitEntity>
}
