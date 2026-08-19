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
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
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

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun exportFullDatabaseToZip(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val csvFiles = mutableListOf<File>()
            
            // 1. Export all tables to CSV
            exportAppUsage()?.let { csvFiles.add(it) }
            exportDailySummaries()?.let { csvFiles.add(it) }
            exportUsageSensorLogs()?.let { csvFiles.add(it) }
            exportInterventions()?.let { csvFiles.add(it) }
            exportInterventionLogs()?.let { csvFiles.add(it) }
            exportDailyAdaptiveLimits()?.let { csvFiles.add(it) }

            if (csvFiles.isEmpty()) {
                return@withContext Result.failure(Exception("No data found to export"))
            }

            // 2. Create ZIP package
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
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

    private suspend fun exportAppUsage(): File? {
        val data = usageDao.getAllAppUsageList()
        if (data.isEmpty()) return null
        val file = createTempFile("app_usage.csv")
        FileWriter(file).use { writer ->
            writer.append("PackageName,AppName,UsageDurationMillis,LastUsedTimestamp,Date\n")
            data.forEach {
                writer.append("\"${it.packageName}\",\"${it.appName}\",${it.usageDurationMillis},${it.lastUsedTimestamp},\"${it.date}\"\n")
            }
        }
        return file
    }

    private suspend fun exportDailySummaries(): File? {
        val data = usageDao.getAllDailySummariesList()
        if (data.isEmpty()) return null
        val file = createTempFile("daily_summaries.csv")
        FileWriter(file).use { writer ->
            writer.append("Date,TotalScreenTimeMillis,TotalScreenOnMillis,MonitoredUsageMillis,UnlockCount,MostUsedApp,WellbeingScore\n")
            data.forEach {
                writer.append("\"${it.date}\",${it.totalScreenTimeMillis},${it.totalScreenOnMillis},${it.monitoredUsageMillis},${it.unlockCount},\"${it.mostUsedApp ?: ""}\",${it.wellbeingScore ?: ""}\n")
            }
        }
        return file
    }

    private suspend fun exportUsageSensorLogs(): File? {
        val data = usageSensorDao.getAllLogsList()
        if (data.isEmpty()) return null
        val file = createTempFile("usage_sensor_logs.csv")
        FileWriter(file).use { writer ->
            writer.append("Timestamp,Date,PackageName,RawScreenTimeMs,AppOpeningFrequency,AmbientLightLux\n")
            data.forEach {
                writer.append("${it.timestamp},\"${dateFormatter.format(Date(it.timestamp))}\",\"${it.packageName}\",${it.rawScreenTimeMs},${it.appOpeningFrequency},${it.ambientLightLux}\n")
            }
        }
        return file
    }

    private suspend fun exportInterventions(): File? {
        val data = interventionNotificationDao.getAllInterventionsList()
        if (data.isEmpty()) return null
        val file = createTempFile("interventions.csv")
        FileWriter(file).use { writer ->
            writer.append("ID,Title,Message,Type,Timestamp,Date,IsAcknowledged\n")
            data.forEach {
                writer.append("\"${it.id}\",\"${it.title}\",\"${it.message}\",\"${it.type}\",${it.timestamp},\"${dateFormatter.format(Date(it.timestamp))}\",${it.isAcknowledged}\n")
            }
        }
        return file
    }

    private suspend fun exportInterventionLogs(): File? {
        val data = interventionDao.getAllLogsList()
        if (data.isEmpty()) return null
        val file = createTempFile("intervention_logs.csv")
        FileWriter(file).use { writer ->
            writer.append("ID,Timestamp,Date,Deviation,DifficultyControlSignal,DifficultyLevel,ResponseTimeMs,IsSuccess,PenaltyAppliedMinutes\n")
            data.forEach {
                writer.append("${it.id},${it.timestamp},\"${dateFormatter.format(Date(it.timestamp))}\",${it.deviation},${it.difficultyControlSignal},${it.difficultyLevel},${it.responseTimeMs},${it.isSuccess},${it.penaltyAppliedMinutes}\n")
            }
        }
        return file
    }

    private suspend fun exportDailyAdaptiveLimits(): File? {
        val data = adaptiveLimitDao.getAllLimitsList()
        if (data.isEmpty()) return null
        val file = createTempFile("daily_adaptive_limits.csv")
        FileWriter(file).use { writer ->
            writer.append("DateString,CalculatedLimitMinutes,ActualScreenTimeMinutes,ReclaimedTimeMinutes\n")
            data.forEach {
                writer.append("\"${it.dateString}\",${it.calculatedLimitMinutes},${it.actualScreenTimeMinutes},${it.reclaimedTimeMinutes}\n")
            }
        }
        return file
    }

    private fun createTempFile(fileName: String): File {
        val exportDir = File(context.cacheDir, "temp_exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        return File(exportDir, fileName)
    }
}
