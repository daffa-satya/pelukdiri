package com.makhp.pelukdiri.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.makhp.pelukdiri.R
import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.core.domain.model.UsageSensorLog
import com.makhp.pelukdiri.core.domain.repository.AdaptiveLimitRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UsageSensorRepository
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import com.makhp.pelukdiri.core.domain.usecase.InitializeDailyAdaptiveLimitUseCase
import com.makhp.pelukdiri.core.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar

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
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Set as foreground service to prevent being killed
            setForeground(createForegroundInfo())

            // 1. Refresh general usage data (AppUsage & DailySummary)
            usageRepository.refreshUsageData()

            // 2. Ambil data dari UsageStats & Sensor untuk logs (Variabel H, F, L)
            val currentTimestamp = System.currentTimeMillis()
            
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            
            val activeApps = appUsageCollector.fetchRecentEvents(startTime, currentTimestamp)
            val ambientLux = appUsageCollector.getCurrentAmbientLightLux()

            // 3. Batch save logs
            val sensorLogs = activeApps.map { app ->
                val pkg = app.packageName
                val screenTimeMs = app.usageDurationMillis
                val openFreq = appUsageCollector.getLaunchCountForPackage(pkg)

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
            handleNotifications()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun handleNotifications() {
        // MANDATORY: DND check removed as per user request to delete DND
        val today = LocalDate.now()
        val todayStr = today.toString()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // Sunday = 1

        val summary = usageRepository.getDailySummary(today).firstOrNull()
        val limit = adaptiveLimitRepository.getLimitForDate(todayStr)
        val monitoredUsageMillis = summary?.monitoredUsageMillis ?: 0L
        val limitMinutes = limit?.calculatedLimitMinutes

        android.util.Log.d("UsageSyncWorker", "Sending status notification: usage=$monitoredUsageMillis, limit=$limitMinutes")
        
        // 4. Update the persistent daily status notification
        notificationHelper.updateDailyUsageNotification(
            totalUsageMillis = monitoredUsageMillis,
            adaptiveLimitMinutes = limitMinutes
        )

        // 1. Daily Summary (around 20:00) - MANDATORY
        if (currentHour >= 20) {
            val lastSentDate = userPreferencesRepository.lastDailySummaryDate.firstOrNull()
            android.util.Log.d("UsageSyncWorker", "DailySummary check: lastSent=$lastSentDate, current=$todayStr")
            if (lastSentDate != todayStr) {
                notificationHelper.showDailySummaryNotification(monitoredUsageMillis)
                userPreferencesRepository.setLastDailySummaryDate(todayStr)
            }
        }

        // 2. Weekly Reflection (Sundays around 19:00) - MANDATORY
        if (dayOfWeek == Calendar.SUNDAY && currentHour >= 19) {
            val weekId = "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.WEEK_OF_YEAR)}"
            val lastSentWeek = userPreferencesRepository.lastWeeklyReflectionDate.firstOrNull()
            android.util.Log.d("UsageSyncWorker", "WeeklyReflection check: lastSent=$lastSentWeek, current=$weekId")
            if (lastSentWeek != weekId) {
                notificationHelper.showWeeklyReflectionNotification()
                userPreferencesRepository.setLastWeeklyReflectionDate(weekId)
            }
        }

        // 3. Limit Reminder (when usage > 90% of limit) - MANDATORY
        if (limitMinutes != null && limitMinutes > 0) {
            val limitMillis = limitMinutes * 60_000L
            val threshold = 0.9f
            if (monitoredUsageMillis >= limitMillis * threshold) {
                val lastSentTime = userPreferencesRepository.lastLimitReminderTimestamp.firstOrNull() ?: 0L
                val oneHourMillis = 60 * 60 * 1000L
                if (System.currentTimeMillis() - lastSentTime > oneHourMillis) {
                    notificationHelper.showLimitReminderNotification()
                    userPreferencesRepository.setLastLimitReminderTimestamp(System.currentTimeMillis())
                }
            }
        }

        // 4. Update the persistent daily status notification
        notificationHelper.updateDailyUsageNotification(
            totalUsageMillis = monitoredUsageMillis,
            adaptiveLimitMinutes = limitMinutes
        )
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "usage_sync_channel"
        val notificationId = 1
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.notification_sync_channel_name)
            val descriptionText = applicationContext.getString(R.string.notification_sync_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(applicationContext.getString(R.string.notification_sync_active_title))
            .setContentText(applicationContext.getString(R.string.notification_sync_collecting))
            .setSmallIcon(R.mipmap.ic_launcher) // Use default icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}
