package com.makhp.pelukdiri.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.core.domain.model.UsageSensorLog
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UsageSensorRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import com.makhp.pelukdiri.core.domain.usecase.InitializeDailyAdaptiveLimitUseCase
import com.makhp.pelukdiri.core.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone

@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageRepository: UsageRepository,
    private val usageSensorRepository: UsageSensorRepository,
    private val appUsageCollector: AppUsageCollector,
    private val adaptiveLimitRepository: AdaptiveLimitRepository,
    private val initializeDailyAdaptiveLimitUseCase: InitializeDailyAdaptiveLimitUseCase,
    private val notificationHelper: NotificationHelper,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Refresh general usage data (AppUsage & DailySummary)
            usageRepository.refreshUsageData()

            // 2. Ambil data dari UsageStats & Sensor untuk logs (Variabel H, F, L)
            val currentTimestamp = timeProvider.nowMillis()
            val zoneId = timeProvider.zoneId()
            val startTime = Instant.ofEpochMilli(currentTimestamp)
                .atZone(zoneId)
                .toLocalDate()
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
            
            val activeApps = appUsageCollector.fetchRecentEvents(startTime, currentTimestamp)
            appUsageCollector.startLightSensor()
            val ambientLux = try {
                appUsageCollector.getCurrentAmbientLightLux()
            } finally {
                appUsageCollector.stopLightSensor()
            }

            val launchCounts = appUsageCollector.getLaunchCountsForAllPackages(
                startTime = startTime,
                endTime = currentTimestamp,
            )

            // 3. Batch save logs
            val sensorLogs = activeApps.map { app ->
                val pkg = app.packageName
                val screenTimeMs = app.usageDurationMillis
                val openFreq = launchCounts[pkg] ?: 0

                UsageSensorLog(
                    timestamp = currentTimestamp,
                    packageName = pkg,
                    rawScreenTimeMs = screenTimeMs,
                    appOpeningFrequency = openFreq,
                    ambientLightLux = ambientLux
                )
            }

            if (sensorLogs.isNotEmpty()) {
                usageSensorRepository.insertAllLogs(sensorLogs)
            }

            // 4. Initialize today's adaptive limit if missing (Idempotent)
            initializeDailyAdaptiveLimitUseCase()

            // 5. Update Notifications
            handleNotifications(currentTimestamp)

            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Log.e(TAG, "Usage synchronization failed")
            Result.retry()
        }
    }

    private suspend fun handleNotifications(currentTimestamp: Long) {
        // MANDATORY: DND check removed as per user request to delete DND
        val zoneId = timeProvider.zoneId()
        val today = Instant.ofEpochMilli(currentTimestamp).atZone(zoneId).toLocalDate()
        val todayStr = today.toString()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(zoneId)).apply {
            timeInMillis = currentTimestamp
        }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday = 1

        val summary = usageRepository.getDailySummary(today).firstOrNull()
        val limit = adaptiveLimitRepository.getLimitForDate(todayStr)
        val monitoredUsageMillis = summary?.monitoredUsageMillis ?: 0L
        val limitMinutes = limit?.calculatedLimitMinutes

        // 4. Update the persistent daily status notification
        notificationHelper.updateDailyUsageNotification(
            totalUsageMillis = monitoredUsageMillis,
            adaptiveLimitMinutes = limitMinutes
        )

        val shouldCheckDailySummary = currentHour >= 20
        val shouldCheckWeeklyReflection = dayOfWeek == Calendar.SUNDAY && currentHour >= 19
        val shouldCheckLimitReminder = limitMinutes != null &&
            limitMinutes > 0 &&
            monitoredUsageMillis >= limitMinutes * 54_000L
        if (!shouldCheckDailySummary && !shouldCheckWeeklyReflection && !shouldCheckLimitReminder) {
            return
        }
        val preferences = userPreferencesRepository.snapshot.first()

        // 1. Daily Summary (around 20:00) - MANDATORY
        if (shouldCheckDailySummary) {
            if (preferences.lastDailySummaryDate != todayStr) {
                notificationHelper.showDailySummaryNotification(monitoredUsageMillis)
                userPreferencesRepository.setLastDailySummaryDate(todayStr)
            }
        }

        // 2. Weekly Reflection (Sundays around 19:00) - MANDATORY
        if (shouldCheckWeeklyReflection) {
            val weekId = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
            if (preferences.lastWeeklyReflectionDate != weekId) {
                notificationHelper.showWeeklyReflectionNotification()
                userPreferencesRepository.setLastWeeklyReflectionDate(weekId)
            }
        }

        // 3. Limit Reminder (when usage > 90% of limit) - MANDATORY
        if (shouldCheckLimitReminder) {
            val oneHourMillis = 60 * 60 * 1000L
            if (currentTimestamp - preferences.lastLimitReminderTimestamp > oneHourMillis) {
                notificationHelper.showLimitReminderNotification()
                userPreferencesRepository.setLastLimitReminderTimestamp(currentTimestamp)
            }
        }
    }

    private companion object {
        const val TAG = "UsageSyncWorker"
    }
}
