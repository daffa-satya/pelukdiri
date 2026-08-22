package com.makhp.pelukdiri.core.domain.repository

import com.makhp.pelukdiri.core.domain.model.UsageSensorLog
import kotlinx.coroutines.flow.Flow

interface UsageSensorRepository {
    suspend fun insertLog(log: UsageSensorLog)
    suspend fun insertAllLogs(logs: List<UsageSensorLog>)
    fun getAllLogs(): Flow<List<UsageSensorLog>>
    suspend fun getAllLogsList(): List<UsageSensorLog>
    suspend fun getLogsInRange(startTime: Long, endTime: Long): List<UsageSensorLog>
    suspend fun deleteLogsBefore(cutoffEpochMillis: Long): Int
}
