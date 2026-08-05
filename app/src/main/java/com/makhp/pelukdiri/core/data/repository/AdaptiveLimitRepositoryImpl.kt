package com.makhp.pelukdiri.core.data.repository

import com.makhp.pelukdiri.core.data.mapper.toDomainModel
import com.makhp.pelukdiri.core.data.mapper.toEntity
import com.makhp.pelukdiri.core.database.dao.AdaptiveLimitDao
import com.makhp.pelukdiri.core.domain.model.DailyAdaptiveLimit
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AdaptiveLimitRepositoryImpl @Inject constructor(
    private val dao: AdaptiveLimitDao
) : AdaptiveLimitRepository {

    override suspend fun insertOrUpdateLimit(limit: DailyAdaptiveLimit) {
        dao.insertOrUpdateLimit(limit.toEntity())
    }

    override suspend fun getLimitForDate(date: String): DailyAdaptiveLimit? {
        return dao.getLimitForDate(date)?.toDomainModel()
    }

    override fun getAllLimits(): Flow<List<DailyAdaptiveLimit>> {
        return dao.getAllLimits().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAllLimitsList(): List<DailyAdaptiveLimit> {
        return dao.getAllLimitsList().map { it.toDomainModel() }
    }
}
