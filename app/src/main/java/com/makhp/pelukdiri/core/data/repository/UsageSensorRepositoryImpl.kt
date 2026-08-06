package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.core.data.mapper.toDomainModel
import com.makhp.pelukdiri.core.data.mapper.toEntity
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import com.makhp.pelukdiri.core.domain.model.UsageSensorLog
import com.makhp.pelukdiri.core.domain.repository.UsageSensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UsageSensorRepositoryImpl @Inject constructor(
    private val dao: UsageSensorDao
) : UsageSensorRepository {

    override suspend fun insertLog(log: UsageSensorLog) {
        dao.insertLog(log.toEntity())
    }

    override suspend fun insertAllLogs(logs: List<UsageSensorLog>) {
        dao.insertAllLogs(logs.map { it.toEntity() })
    }

    override fun getAllLogs(): Flow<List<UsageSensorLog>> {
        return dao.getAllLogs().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAllLogsList(): List<UsageSensorLog> {
        return dao.getAllLogsList().map { it.toDomainModel() }
    }

    override suspend fun getLogsInRange(startTime: Long, endTime: Long): List<UsageSensorLog> {
        return dao.getLogsInRange(startTime, endTime).map { it.toDomainModel() }
    }
}
