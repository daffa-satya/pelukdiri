package com.makhp.pelukdiri.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.makhp.pelukdiri.core.domain.model.AggressivenessLevel
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
    @param:ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val IS_HISTORY_BACKFILLED = booleanPreferencesKey("is_history_backfilled")
        val LAST_SYNCED_TIMESTAMP = longPreferencesKey("last_synced_timestamp")
        val EMERGENCY_BYPASS_UNTIL = longPreferencesKey("emergency_bypass_until")
        val MONITORED_PACKAGES = stringSetPreferencesKey("monitored_package_names")
        val AGGRESSIVENESS_LEVEL = stringPreferencesKey("aggressiveness_level")
        val IS_FIXED_LIMIT_ENABLED = booleanPreferencesKey("is_fixed_limit_enabled")
        val FIXED_DAILY_LIMIT_MINUTES = intPreferencesKey("fixed_daily_limit_minutes")
        val BEDTIME = stringPreferencesKey("bedtime")
        val WAKE_TIME = stringPreferencesKey("wake_time")
        val CURRENT_DIFFICULTY = intPreferencesKey("current_difficulty")
        val NEXT_ELIGIBLE_INTERVENTION_AT = longPreferencesKey("next_eligible_intervention_at")
        val ACTIVE_INTERVENTION_SESSION = stringPreferencesKey("active_intervention_session_v1")
        val USER_NICKNAME = stringPreferencesKey("user_nickname")
        val USERNAME = stringPreferencesKey("username")
        val PROFILE_IMAGE_PATH = stringPreferencesKey("profile_image_path")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
        val IS_DAILY_SUMMARY_ENABLED = booleanPreferencesKey("is_daily_summary_enabled")
        val IS_WEEKLY_REFLECTION_ENABLED = booleanPreferencesKey("is_weekly_reflection_enabled")
        val IS_LIMIT_REMINDER_ENABLED = booleanPreferencesKey("is_limit_reminder_enabled")
        val IS_INTERVENTION_REMINDER_ENABLED = booleanPreferencesKey("is_intervention_reminder_enabled")
        val IS_DND_ENABLED = booleanPreferencesKey("is_dnd_enabled_global")
        val LAST_DAILY_SUMMARY_DATE = stringPreferencesKey("last_daily_summary_date")
        val LAST_WEEKLY_REFLECTION_DATE = stringPreferencesKey("last_weekly_reflection_date")
        val LAST_LIMIT_REMINDER_TIMESTAMP = longPreferencesKey("last_limit_reminder_timestamp")
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

    override val aggressivenessLevel: Flow<AggressivenessLevel> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val levelName = preferences[PreferencesKeys.AGGRESSIVENESS_LEVEL] ?: AggressivenessLevel.BALANCED.name
            AggressivenessLevel.valueOf(levelName)
        }

    override val isFixedLimitEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.IS_FIXED_LIMIT_ENABLED] ?: false
        }

    override val fixedDailyLimitMinutes: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.FIXED_DAILY_LIMIT_MINUTES] ?: 60
        }

    override val bedtime: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.BEDTIME]
        }

    override val wakeTime: Flow<String?> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.WAKE_TIME]
        }

    override val currentDifficulty: Flow<Int> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.CURRENT_DIFFICULTY] ?: 2
        }

    override val nextEligibleInterventionAt: Flow<Long> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.NEXT_ELIGIBLE_INTERVENTION_AT] ?: 0L
        }

    override val activeInterventionSession: Flow<String?> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.ACTIVE_INTERVENTION_SESSION] }

    override val userNickname: Flow<String> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.USER_NICKNAME] ?: "User" }

    override val username: Flow<String> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.USERNAME] ?: "@user" }

    override val profileImagePath: Flow<String?> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.PROFILE_IMAGE_PATH] }

    override val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false }

    override val isDailySummaryEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.IS_DAILY_SUMMARY_ENABLED] ?: true }

    override val isWeeklyReflectionEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.IS_WEEKLY_REFLECTION_ENABLED] ?: true }

    override val isLimitReminderEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.IS_LIMIT_REMINDER_ENABLED] ?: true }

    override val isInterventionReminderEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.IS_INTERVENTION_REMINDER_ENABLED] ?: true }

    override val isDndEnabled: Flow<Boolean> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.IS_DND_ENABLED] ?: false }

    override val lastDailySummaryDate: Flow<String?> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.LAST_DAILY_SUMMARY_DATE] }

    override val lastWeeklyReflectionDate: Flow<String?> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.LAST_WEEKLY_REFLECTION_DATE] }

    override val lastLimitReminderTimestamp: Flow<Long> = context.dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { preferences -> preferences[PreferencesKeys.LAST_LIMIT_REMINDER_TIMESTAMP] ?: 0L }

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

    override suspend fun setAggressivenessLevel(level: AggressivenessLevel) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AGGRESSIVENESS_LEVEL] = level.name
        }
    }

    override suspend fun setFixedLimitEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIXED_LIMIT_ENABLED] = enabled
        }
    }

    override suspend fun setFixedDailyLimitMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIXED_DAILY_LIMIT_MINUTES] = minutes
        }
    }

    override suspend fun setBedtime(time: String?) {
        context.dataStore.edit { preferences ->
            if (time == null) {
                preferences.remove(PreferencesKeys.BEDTIME)
            } else {
                preferences[PreferencesKeys.BEDTIME] = time
            }
        }
    }

    override suspend fun setWakeTime(time: String?) {
        context.dataStore.edit { preferences ->
            if (time == null) {
                preferences.remove(PreferencesKeys.WAKE_TIME)
            } else {
                preferences[PreferencesKeys.WAKE_TIME] = time
            }
        }
    }

    override suspend fun setCurrentDifficulty(difficulty: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_DIFFICULTY] = difficulty
        }
    }

    override suspend fun setNextEligibleInterventionAt(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NEXT_ELIGIBLE_INTERVENTION_AT] = timestamp
        }
    }

    override suspend fun setActiveInterventionSession(encodedSession: String?) {
        context.dataStore.edit { preferences ->
            if (encodedSession == null) preferences.remove(PreferencesKeys.ACTIVE_INTERVENTION_SESSION)
            else preferences[PreferencesKeys.ACTIVE_INTERVENTION_SESSION] = encodedSession
        }
    }

    override suspend fun setUserNickname(nickname: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.USER_NICKNAME] = nickname }
    }

    override suspend fun setUsername(username: String) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.USERNAME] = username }
    }

    override suspend fun setProfileImagePath(path: String?) {
        context.dataStore.edit { preferences ->
            if (path == null) preferences.remove(PreferencesKeys.PROFILE_IMAGE_PATH)
            else preferences[PreferencesKeys.PROFILE_IMAGE_PATH] = path
        }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setDailySummaryEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_DAILY_SUMMARY_ENABLED] = enabled }
    }

    override suspend fun setWeeklyReflectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_WEEKLY_REFLECTION_ENABLED] = enabled }
    }

    override suspend fun setLimitReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_LIMIT_REMINDER_ENABLED] = enabled }
    }

    override suspend fun setInterventionReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_INTERVENTION_REMINDER_ENABLED] = enabled }
    }

    override suspend fun setDndEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences -> preferences[PreferencesKeys.IS_DND_ENABLED] = enabled }
    }

    override suspend fun setLastDailySummaryDate(date: String?) {
        context.dataStore.edit { preferences ->
            if (date == null) preferences.remove(PreferencesKeys.LAST_DAILY_SUMMARY_DATE)
            else preferences[PreferencesKeys.LAST_DAILY_SUMMARY_DATE] = date
        }
    }

    override suspend fun setLastWeeklyReflectionDate(date: String?) {
        context.dataStore.edit { preferences ->
            if (date == null) preferences.remove(PreferencesKeys.LAST_WEEKLY_REFLECTION_DATE)
            else preferences[PreferencesKeys.LAST_WEEKLY_REFLECTION_DATE] = date
        }
    }

    override suspend fun setLastLimitReminderTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_LIMIT_REMINDER_TIMESTAMP] = timestamp
        }
    }
}
