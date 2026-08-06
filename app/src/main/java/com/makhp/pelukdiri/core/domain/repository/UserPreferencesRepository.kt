package com.makhp.pelukdiri.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val isHistoryBackfilled: Flow<Boolean>
    val lastSyncedTimestamp: Flow<Long>
    val emergencyBypassUntil: Flow<Long>
    
    suspend fun setHistoryBackfilled(isBackfilled: Boolean)
    suspend fun setLastSyncedTimestamp(timestamp: Long)
    suspend fun setEmergencyBypassUntil(timestamp: Long)
}
