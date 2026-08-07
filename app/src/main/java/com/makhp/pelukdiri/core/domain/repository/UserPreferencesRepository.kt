package com.makhp.pelukdiri.core.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val isHistoryBackfilled: Flow<Boolean>
    val lastSyncedTimestamp: Flow<Long>
    val emergencyBypassUntil: Flow<Long>
    val monitoredPackages: Flow<Set<String>>
    
    suspend fun setHistoryBackfilled(isBackfilled: Boolean)
    suspend fun setLastSyncedTimestamp(timestamp: Long)
    suspend fun setEmergencyBypassUntil(timestamp: Long)
    suspend fun toggleMonitoredPackage(packageName: String)
}
