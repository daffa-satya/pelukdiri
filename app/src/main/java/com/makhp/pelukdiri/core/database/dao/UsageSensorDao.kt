package com.makhp.pelukdiri.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.makhp.pelukdiri.core.database.entity.UsageSensorLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageSensorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: UsageSensorLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllLogs(logs: List<UsageSensorLogEntity>)

    @Query("SELECT * FROM usage_sensor_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<UsageSensorLogEntity>>

    @Query("SELECT * FROM usage_sensor_logs ORDER BY timestamp ASC")
    suspend fun getAllLogsList(): List<UsageSensorLogEntity>

    @Query("SELECT * FROM usage_sensor_logs WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getLogsInRange(startTime: Long, endTime: Long): List<UsageSensorLogEntity>

    @Query("DELETE FROM usage_sensor_logs WHERE timestamp < :cutoffEpochMillis")
    suspend fun deleteLogsBefore(cutoffEpochMillis: Long): Int
}
