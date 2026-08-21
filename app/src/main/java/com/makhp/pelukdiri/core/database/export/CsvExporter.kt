package com.makhp.pelukdiri.core.database.export

import android.content.Context
import com.makhp.pelukdiri.core.database.dao.AdaptiveLimitDao
import com.makhp.pelukdiri.core.database.dao.InterventionDao
import com.makhp.pelukdiri.core.database.dao.InterventionNotificationDao
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageDao: UsageDao,
    private val usageSensorDao: UsageSensorDao,
    private val interventionDao: InterventionDao,
    private val interventionNotificationDao: InterventionNotificationDao,
    private val adaptiveLimitDao: AdaptiveLimitDao
) {

    suspend fun exportFullDatabaseToZip(): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Always include every expected CSV. Empty tables still produce a
            // header-only file so exports have a stable, machine-readable schema.
            val csvFiles = listOf(
                exportAppUsage(),
                exportDailySummaries(),
                exportUsageSensorLogs(),
                exportInterventions(),
                exportInterventionLogs(),
                exportDailyAdaptiveLimits(),
            )

            // 2. Create ZIP package
            val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC).format(Instant.now())
            val zipFileName = "PELUKDIRI_FullExport_$timestamp.zip"
            val exportDir = File(context.getExternalFilesDir(null), "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val zipFile = File(exportDir, zipFileName)
            
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                csvFiles.forEach { file ->
                    zos.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                    // Delete temp CSV file after zipping
                    file.delete()
                }
            }

            Result.success(zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private suspend fun exportAppUsage(): File {
        val data = usageDao.getAllAppUsageList()
        val file = createTempFile("app_usage.csv")
        writerFor(file).use { writer ->
            writer.append("PackageName,AppName,UsageDurationMillis,LastUsedTimestamp,Date\r\n")
            data.forEach {
                writer.append(CsvFormat.row(it.packageName, it.appName, it.usageDurationMillis, it.lastUsedTimestamp, it.date))
            }
        }
        return file
    }

    private suspend fun exportDailySummaries(): File {
        val data = usageDao.getAllDailySummariesList()
        val file = createTempFile("daily_summaries.csv")
        writerFor(file).use { writer ->
            writer.append("Date,TotalScreenTimeMillis,TotalScreenOnMillis,MonitoredUsageMillis,UnlockCount,MostUsedApp,WellbeingScore\r\n")
            data.forEach {
                writer.append(CsvFormat.row(it.date, it.totalScreenTimeMillis, it.totalScreenOnMillis, it.monitoredUsageMillis, it.unlockCount, it.mostUsedApp ?: "", it.wellbeingScore))
            }
        }
        return file
    }

    private suspend fun exportUsageSensorLogs(): File {
        val data = usageSensorDao.getAllLogsList()
        val file = createTempFile("usage_sensor_logs.csv")
        writerFor(file).use { writer ->
            writer.append("Timestamp,Date,PackageName,RawScreenTimeMs,AppOpeningFrequency,AmbientLightLux\r\n")
            data.forEach {
                writer.append(CsvFormat.row(it.timestamp, CsvFormat.timestamp(it.timestamp), it.packageName, it.rawScreenTimeMs, it.appOpeningFrequency, it.ambientLightLux))
            }
        }
        return file
    }

    private suspend fun exportInterventions(): File {
        val data = interventionNotificationDao.getAllInterventionsList()
        val file = createTempFile("interventions.csv")
        writerFor(file).use { writer ->
            writer.append("ID,Title,Message,Type,Timestamp,Date,IsAcknowledged\r\n")
            data.forEach {
                writer.append(CsvFormat.row(it.id, it.title, it.message, it.type, it.timestamp, CsvFormat.timestamp(it.timestamp), it.isAcknowledged))
            }
        }
        return file
    }

    private suspend fun exportInterventionLogs(): File {
        val data = interventionDao.getAllLogsList()
        val file = createTempFile("intervention_logs.csv")
        writerFor(file).use { writer ->
            writer.append("ID,Timestamp,Date,Deviation,DifficultyControlSignal,DifficultyLevel,ResponseTimeMs,IsSuccess,IsBypassed,PenaltyAppliedMinutes\r\n")
            data.forEach {
                writer.append(CsvFormat.row(it.id, it.timestamp, CsvFormat.timestamp(it.timestamp), it.deviation, it.difficultyControlSignal, it.difficultyLevel, it.responseTimeMs, it.isSuccess, it.isBypassed, it.penaltyAppliedMinutes))
            }
        }
        return file
    }

    private suspend fun exportDailyAdaptiveLimits(): File {
        val data = adaptiveLimitDao.getAllLimitsList()
        val file = createTempFile("daily_adaptive_limits.csv")
        writerFor(file).use { writer ->
            writer.append("DateString,CalculatedLimitMinutes,ActualScreenTimeMinutes,ReclaimedTimeMinutes\r\n")
            data.forEach {
                writer.append(CsvFormat.row(it.dateString, it.calculatedLimitMinutes, it.actualScreenTimeMinutes, it.reclaimedTimeMinutes))
            }
        }
        return file
    }

    private fun createTempFile(fileName: String): File {
        val exportDir = File(context.cacheDir, "temp_exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, fileName)
    }

    private fun writerFor(file: File) = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
}
