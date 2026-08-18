package com.makhp.pelukdiri.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.makhp.pelukdiri.core.database.entity.InterventionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: InterventionLogEntity)

    @Query("SELECT * FROM intervention_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<InterventionLogEntity>>

    @Query("SELECT * FROM intervention_logs ORDER BY timestamp ASC")
    suspend fun getAllLogsList(): List<InterventionLogEntity>

    @Query("SELECT AVG(responseTimeMs) FROM intervention_logs WHERE difficultyLevel = :difficulty")
    suspend fun getAverageResponseTime(difficulty: Int): Double?

    @Query("SELECT * FROM intervention_logs WHERE difficultyLevel = :difficulty AND isSuccess = 1 AND responseTimeMs > 0 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentValidSuccessfulLogsByDifficulty(difficulty: Int, limit: Int): List<InterventionLogEntity>

    @Query("SELECT * FROM intervention_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLog(): InterventionLogEntity?

    @Query("SELECT COUNT(*) FROM intervention_logs WHERE isBypassed = 1 AND timestamp >= :start AND timestamp < :end")
    suspend fun getBypassCountInInterval(start: Long, end: Long): Int
}
