package com.makhp.pelukdiri.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.makhp.pelukdiri.collector.AppUsageCollector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UsageSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appUsageCollector: AppUsageCollector // 1. Hilt automatically injects your collector here!
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // 2. Define the time window (e.g., check the last 15 minutes)
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (1000 * 60 * 15)

        return try {
            // 3. Trigger your collector to gather the data!
            appUsageCollector.fetchRecentEvents(startTime, endTime)

            Result.success()
        } catch (e: Exception) {
            Result.retry() // If something fails, try again later
        }
    }
}