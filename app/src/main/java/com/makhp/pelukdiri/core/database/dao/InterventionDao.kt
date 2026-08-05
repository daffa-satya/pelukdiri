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
    suspend fun getAverageResponseTime(difficulty: String): Long?
}
