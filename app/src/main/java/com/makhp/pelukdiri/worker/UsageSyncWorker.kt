package com.makhp.pelukdiri.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.core.domain.model.UsageSensorLog
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import com.makhp.pelukdiri.core.domain.repository.UsageSensorRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageRepository: UsageRepository,
    private val usageSensorRepository: UsageSensorRepository,
    private val appUsageCollector: AppUsageCollector
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
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

            // 3. Loop setiap package yang terdeteksi aktif hari ini dan simpan log-nya
            for (app in activeApps) {
                val pkg = app.packageName
                val screenTimeMs = app.usageDurationMillis
                val openFreq = appUsageCollector.getLaunchCountForPackage(pkg)

                val sensorLog = UsageSensorLog(
                    timestamp = currentTimestamp,
                    packageName = pkg,
                    rawScreenTimeMs = screenTimeMs,
                    appOpeningFrequency = openFreq,
                    ambientLightLux = ambientLux
                )

                usageSensorRepository.insertLog(sensorLog)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
