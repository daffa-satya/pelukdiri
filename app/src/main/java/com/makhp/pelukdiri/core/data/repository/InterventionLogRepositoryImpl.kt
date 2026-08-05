package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.core.data.mapper.toDomainModel
import com.makhp.pelukdiri.core.data.mapper.toEntity
import com.makhp.pelukdiri.core.database.dao.InterventionDao
import com.makhp.pelukdiri.core.domain.model.InterventionLog
import com.makhp.pelukdiri.core.domain.repository.InterventionLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class InterventionLogRepositoryImpl @Inject constructor(
    private val dao: InterventionDao
) : InterventionLogRepository {

    override suspend fun insertLog(log: InterventionLog) {
        dao.insertLog(log.toEntity())
    }

    override fun getAllLogs(): Flow<List<InterventionLog>> {
        return dao.getAllLogs().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAllLogsList(): List<InterventionLog> {
        return dao.getAllLogsList().map { it.toDomainModel() }
    }

    override suspend fun getAverageResponseTime(difficulty: String): Long? {
        return dao.getAverageResponseTime(difficulty)
    }
}
