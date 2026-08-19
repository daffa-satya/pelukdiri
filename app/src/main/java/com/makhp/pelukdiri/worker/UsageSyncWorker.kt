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
    private val notificationHelper: NotificationHelper
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

            // 5. Update Daily Usage Notification
            val today = LocalDate.now()
            val summary = usageRepository.getDailySummary(today).firstOrNull()
            val limit = adaptiveLimitRepository.getLimitForDate(today.toString())

            notificationHelper.updateDailyUsageNotification(
                totalUsageMillis = summary?.monitoredUsageMillis ?: 0L,
                adaptiveLimitMinutes = limit?.calculatedLimitMinutes
            )

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val channelId = "usage_sync_channel"
        val notificationId = 1
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Usage Sync Service"
            val descriptionText = "Collecting research data in background"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("PELUKDIRI is active")
            .setContentText("Collecting usage data...")
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
