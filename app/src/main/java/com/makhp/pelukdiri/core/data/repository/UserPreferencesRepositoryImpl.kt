package com.makhp.pelukdiri.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val IS_HISTORY_BACKFILLED = booleanPreferencesKey("is_history_backfilled")
        val LAST_SYNCED_TIMESTAMP = longPreferencesKey("last_synced_timestamp")
        val EMERGENCY_BYPASS_UNTIL = longPreferencesKey("emergency_bypass_until")
        val MONITORED_PACKAGES = stringSetPreferencesKey("monitored_package_names")
    }

    private val defaultTargetApps = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.google.android.youtube",
        "com.twitter.android",
        "com.facebook.katana"
    )

    override val isHistoryBackfilled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_HISTORY_BACKFILLED] ?: false
        }

    override val lastSyncedTimestamp: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.LAST_SYNCED_TIMESTAMP] ?: 0L
        }

    override val emergencyBypassUntil: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.EMERGENCY_BYPASS_UNTIL] ?: 0L
        }

    override val monitoredPackages: Flow<Set<String>> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.MONITORED_PACKAGES] ?: defaultTargetApps
        }

    override suspend fun setHistoryBackfilled(isBackfilled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_HISTORY_BACKFILLED] = isBackfilled
        }
    }

    override suspend fun setLastSyncedTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNCED_TIMESTAMP] = timestamp
        }
    }

    override suspend fun setEmergencyBypassUntil(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMERGENCY_BYPASS_UNTIL] = timestamp
        }
    }

    override suspend fun toggleMonitoredPackage(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.MONITORED_PACKAGES] ?: defaultTargetApps
            val newSet = current.toMutableSet()
            if (newSet.contains(packageName)) {
                newSet.remove(packageName)
            } else {
                newSet.add(packageName)
            }
            preferences[PreferencesKeys.MONITORED_PACKAGES] = newSet
        }
    }
}
