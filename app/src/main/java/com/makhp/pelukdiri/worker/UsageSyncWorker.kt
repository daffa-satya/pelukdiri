package com.makhp.pelukdiri.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import com.makhp.pelukdiri.core.database.entity.UsageSensorLogEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageSensorDao: UsageSensorDao,
    private val appUsageCollector: AppUsageCollector
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Ambil data dari UsageStats & Sensor
            val currentTimestamp = System.currentTimeMillis()
            
            // Query apps used today (since 00:00)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = calendar.timeInMillis
            
            val activeApps = appUsageCollector.fetchRecentEvents(startTime, currentTimestamp)
            val ambientLux = appUsageCollector.getCurrentAmbientLightLux()

            // 2. Loop setiap package yang terdeteksi aktif hari ini dan simpan log-nya
            for (app in activeApps) {
                val pkg = app.packageName
                val screenTimeMs = app.usageDurationMillis
                val openFreq = appUsageCollector.getLaunchCountForPackage(pkg)

                val sensorLog = UsageSensorLogEntity(
                    timestamp = currentTimestamp,
                    packageName = pkg,
                    rawScreenTimeMs = screenTimeMs,
                    appOpeningFrequency = openFreq,
                    ambientLightLux = ambientLux
                )

                usageSensorDao.insertLog(sensorLog)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}