package com.makhp.pelukdiri.core.domain.repository

import com.makhp.pelukdiri.core.domain.model.InterventionLog
import kotlinx.coroutines.flow.Flow

interface InterventionLogRepository {
    suspend fun insertLog(log: InterventionLog)
    fun getAllLogs(): Flow<List<InterventionLog>>
    suspend fun getAllLogsList(): List<InterventionLog>
    suspend fun getAverageResponseTime(difficulty: Int): Double?
}
