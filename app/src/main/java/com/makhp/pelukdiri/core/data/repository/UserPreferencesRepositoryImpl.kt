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
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesSnapshot
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

    private val preferences = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }

    private fun <T> preference(key: Preferences.Key<T>, default: T): Flow<T> =
        preferences.map { it[key] ?: default }

    private fun <T> optionalPreference(key: Preferences.Key<T>): Flow<T?> =
        preferences.map { it[key] }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T?) {
        context.dataStore.edit { preferences ->
            if (value == null) preferences.remove(key) else preferences[key] = value
        }
    }

    override val snapshot = preferences.map { values ->
        UserPreferencesSnapshot(
            isHistoryBackfilled = values[PreferencesKeys.IS_HISTORY_BACKFILLED] ?: false,
            lastSyncedTimestamp = values[PreferencesKeys.LAST_SYNCED_TIMESTAMP] ?: 0L,
            emergencyBypassUntil = values[PreferencesKeys.EMERGENCY_BYPASS_UNTIL] ?: 0L,
            monitoredPackages = values[PreferencesKeys.MONITORED_PACKAGES] ?: defaultTargetApps,
            aggressivenessLevel = parseAggressivenessLevel(values[PreferencesKeys.AGGRESSIVENESS_LEVEL]),
            isFixedLimitEnabled = values[PreferencesKeys.IS_FIXED_LIMIT_ENABLED] ?: false,
            fixedDailyLimitMinutes = values[PreferencesKeys.FIXED_DAILY_LIMIT_MINUTES] ?: 60,
            bedtime = values[PreferencesKeys.BEDTIME],
            wakeTime = values[PreferencesKeys.WAKE_TIME],
            currentDifficulty = values[PreferencesKeys.CURRENT_DIFFICULTY] ?: 2,
            nextEligibleInterventionAt = values[PreferencesKeys.NEXT_ELIGIBLE_INTERVENTION_AT] ?: 0L,
            activeInterventionSession = values[PreferencesKeys.ACTIVE_INTERVENTION_SESSION],
            userNickname = values[PreferencesKeys.USER_NICKNAME] ?: "User",
            username = values[PreferencesKeys.USERNAME] ?: "@user",
            profileImagePath = values[PreferencesKeys.PROFILE_IMAGE_PATH],
            isOnboardingCompleted = values[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false,
            isDailySummaryEnabled = values[PreferencesKeys.IS_DAILY_SUMMARY_ENABLED] ?: true,
            isWeeklyReflectionEnabled = values[PreferencesKeys.IS_WEEKLY_REFLECTION_ENABLED] ?: true,
            isLimitReminderEnabled = values[PreferencesKeys.IS_LIMIT_REMINDER_ENABLED] ?: true,
            isInterventionReminderEnabled = values[PreferencesKeys.IS_INTERVENTION_REMINDER_ENABLED] ?: true,
            isDndEnabled = values[PreferencesKeys.IS_DND_ENABLED] ?: false,
            lastDailySummaryDate = values[PreferencesKeys.LAST_DAILY_SUMMARY_DATE],
            lastWeeklyReflectionDate = values[PreferencesKeys.LAST_WEEKLY_REFLECTION_DATE],
            lastLimitReminderTimestamp = values[PreferencesKeys.LAST_LIMIT_REMINDER_TIMESTAMP] ?: 0L,
        )
    }

    override val isHistoryBackfilled = preference(PreferencesKeys.IS_HISTORY_BACKFILLED, false)
    override val lastSyncedTimestamp = preference(PreferencesKeys.LAST_SYNCED_TIMESTAMP, 0L)
    override val emergencyBypassUntil = preference(PreferencesKeys.EMERGENCY_BYPASS_UNTIL, 0L)
    override val monitoredPackages = preference(PreferencesKeys.MONITORED_PACKAGES, defaultTargetApps)

    override val aggressivenessLevel: Flow<AggressivenessLevel> = preferences.map {
        parseAggressivenessLevel(it[PreferencesKeys.AGGRESSIVENESS_LEVEL])
    }

    override val isFixedLimitEnabled = preference(PreferencesKeys.IS_FIXED_LIMIT_ENABLED, false)
    override val fixedDailyLimitMinutes = preference(PreferencesKeys.FIXED_DAILY_LIMIT_MINUTES, 60)
    override val bedtime = optionalPreference(PreferencesKeys.BEDTIME)
    override val wakeTime = optionalPreference(PreferencesKeys.WAKE_TIME)
    override val currentDifficulty = preference(PreferencesKeys.CURRENT_DIFFICULTY, 2)
    override val nextEligibleInterventionAt = preference(PreferencesKeys.NEXT_ELIGIBLE_INTERVENTION_AT, 0L)
    override val activeInterventionSession = optionalPreference(PreferencesKeys.ACTIVE_INTERVENTION_SESSION)
    override val userNickname = preference(PreferencesKeys.USER_NICKNAME, "User")
    override val username = preference(PreferencesKeys.USERNAME, "@user")
    override val profileImagePath = optionalPreference(PreferencesKeys.PROFILE_IMAGE_PATH)
    override val isOnboardingCompleted = preference(PreferencesKeys.IS_ONBOARDING_COMPLETED, false)
    override val isDailySummaryEnabled = preference(PreferencesKeys.IS_DAILY_SUMMARY_ENABLED, true)
    override val isWeeklyReflectionEnabled = preference(PreferencesKeys.IS_WEEKLY_REFLECTION_ENABLED, true)
    override val isLimitReminderEnabled = preference(PreferencesKeys.IS_LIMIT_REMINDER_ENABLED, true)
    override val isInterventionReminderEnabled = preference(PreferencesKeys.IS_INTERVENTION_REMINDER_ENABLED, true)
    override val isDndEnabled = preference(PreferencesKeys.IS_DND_ENABLED, false)
    override val lastDailySummaryDate = optionalPreference(PreferencesKeys.LAST_DAILY_SUMMARY_DATE)
    override val lastWeeklyReflectionDate = optionalPreference(PreferencesKeys.LAST_WEEKLY_REFLECTION_DATE)
    override val lastLimitReminderTimestamp = preference(PreferencesKeys.LAST_LIMIT_REMINDER_TIMESTAMP, 0L)

    override suspend fun setHistoryBackfilled(isBackfilled: Boolean) =
        setPreference(PreferencesKeys.IS_HISTORY_BACKFILLED, isBackfilled)

    override suspend fun setLastSyncedTimestamp(timestamp: Long) =
        setPreference(PreferencesKeys.LAST_SYNCED_TIMESTAMP, timestamp)

    override suspend fun setEmergencyBypassUntil(timestamp: Long) =
        setPreference(PreferencesKeys.EMERGENCY_BYPASS_UNTIL, timestamp)

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

    override suspend fun setAggressivenessLevel(level: AggressivenessLevel) =
        setPreference(PreferencesKeys.AGGRESSIVENESS_LEVEL, level.name)

    override suspend fun setFixedLimitEnabled(enabled: Boolean) =
        setPreference(PreferencesKeys.IS_FIXED_LIMIT_ENABLED, enabled)

    override suspend fun setFixedDailyLimitMinutes(minutes: Int) =
        setPreference(PreferencesKeys.FIXED_DAILY_LIMIT_MINUTES, minutes)

    override suspend fun setBedtime(time: String?) = setPreference(PreferencesKeys.BEDTIME, time)

    override suspend fun setWakeTime(time: String?) = setPreference(PreferencesKeys.WAKE_TIME, time)

    override suspend fun setCurrentDifficulty(difficulty: Int) =
        setPreference(PreferencesKeys.CURRENT_DIFFICULTY, difficulty)

    override suspend fun setNextEligibleInterventionAt(timestamp: Long) =
        setPreference(PreferencesKeys.NEXT_ELIGIBLE_INTERVENTION_AT, timestamp)

    override suspend fun setActiveInterventionSession(encodedSession: String?) =
        setPreference(PreferencesKeys.ACTIVE_INTERVENTION_SESSION, encodedSession)

    override suspend fun setUserNickname(nickname: String) =
        setPreference(PreferencesKeys.USER_NICKNAME, nickname)

    override suspend fun setUsername(username: String) =
        setPreference(PreferencesKeys.USERNAME, username)

    override suspend fun setProfileImagePath(path: String?) =
        setPreference(PreferencesKeys.PROFILE_IMAGE_PATH, path)

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        setPreference(PreferencesKeys.IS_ONBOARDING_COMPLETED, completed)

    override suspend fun setDailySummaryEnabled(enabled: Boolean) =
        setPreference(PreferencesKeys.IS_DAILY_SUMMARY_ENABLED, enabled)

    override suspend fun setWeeklyReflectionEnabled(enabled: Boolean) =
        setPreference(PreferencesKeys.IS_WEEKLY_REFLECTION_ENABLED, enabled)

    override suspend fun setLimitReminderEnabled(enabled: Boolean) =
        setPreference(PreferencesKeys.IS_LIMIT_REMINDER_ENABLED, enabled)

    override suspend fun setInterventionReminderEnabled(enabled: Boolean) =
        setPreference(PreferencesKeys.IS_INTERVENTION_REMINDER_ENABLED, enabled)

    override suspend fun setDndEnabled(enabled: Boolean) =
        setPreference(PreferencesKeys.IS_DND_ENABLED, enabled)

    override suspend fun setLastDailySummaryDate(date: String?) =
        setPreference(PreferencesKeys.LAST_DAILY_SUMMARY_DATE, date)

    override suspend fun setLastWeeklyReflectionDate(date: String?) =
        setPreference(PreferencesKeys.LAST_WEEKLY_REFLECTION_DATE, date)

    override suspend fun setLastLimitReminderTimestamp(timestamp: Long) =
        setPreference(PreferencesKeys.LAST_LIMIT_REMINDER_TIMESTAMP, timestamp)
}

internal fun parseAggressivenessLevel(value: String?): AggressivenessLevel =
    AggressivenessLevel.entries.firstOrNull { it.name == value } ?: AggressivenessLevel.BALANCED
