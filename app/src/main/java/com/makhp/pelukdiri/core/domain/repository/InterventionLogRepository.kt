package com.makhp.pelukdiri.core.domain.repository

import com.makhp.pelukdiri.core.domain.model.InterventionLog
import kotlinx.coroutines.flow.Flow

interface InterventionLogRepository {
    suspend fun insertLog(log: InterventionLog)
    fun getAllLogs(): Flow<List<InterventionLog>>
    suspend fun getAllLogsList(): List<InterventionLog>
    suspend fun getRecentLogs(limit: Int): List<InterventionLog>
    suspend fun getAverageResponseTime(difficulty: Int): Double?
    suspend fun getRecentValidSuccessfulLogsByDifficulty(difficulty: Int, limit: Int): List<InterventionLog>
    suspend fun getLatestValidPerformanceLogByDifficulty(difficulty: Int): InterventionLog?
    suspend fun getLatestLog(): InterventionLog?
    suspend fun getBypassCountForDay(startOfDay: Long, endOfDay: Long): Int
    suspend fun insertBypassIfQuotaAvailable(
        log: InterventionLog,
        startOfDay: Long,
        endOfDay: Long,
        limit: Int,
    ): Int?
}
