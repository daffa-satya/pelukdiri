package com.makhp.pelukdiri.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.makhp.pelukdiri.core.domain.repository.UsageSensorRepository
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class DataRetentionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageSensorRepository: UsageSensorRepository,
    private val timeProvider: TimeProvider,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val retentionDays = 30L
            val cutoffEpochMillis = timeProvider.nowMillis() - TimeUnit.DAYS.toMillis(retentionDays)
            usageSensorRepository.deleteLogsBefore(cutoffEpochMillis)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Log.e(TAG, "Data retention cleanup failed")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "DataRetentionWorker"
        const val WORK_NAME = "DataRetentionWorker"
    }
}
