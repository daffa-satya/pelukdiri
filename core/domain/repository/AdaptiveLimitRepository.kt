package com.makhp.pelukdiri.core.domain.repository

import com.makhp.pelukdiri.core.domain.model.DailyAdaptiveLimit
import kotlinx.coroutines.flow.Flow

interface AdaptiveLimitRepository {
    suspend fun insertOrUpdateLimit(limit: DailyAdaptiveLimit)
    suspend fun getLimitForDate(date: String): DailyAdaptiveLimit?
    fun getAllLimits(): Flow<List<DailyAdaptiveLimit>>
    suspend fun getAllLimitsList(): List<DailyAdaptiveLimit>
}
