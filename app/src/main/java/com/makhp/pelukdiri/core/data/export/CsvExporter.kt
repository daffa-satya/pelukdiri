package com.makhp.pelukdiri.core.data.export

import android.content.Context
import com.makhp.pelukdiri.core.database.dao.AdaptiveLimitDao
import com.makhp.pelukdiri.core.database.dao.InterventionDao
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageSensorDao: UsageSensorDao,
    private val interventionDao: InterventionDao,
    private val adaptiveLimitDao: AdaptiveLimitDao
) {

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Ekspor semua tabel database ke dalam file CSV terpisah.
     * Mengembalikan list path file yang berhasil dibuat.
     */
    suspend fun exportAllToCsv(): List<String> = withContext(Dispatchers.IO) {
        val exportPaths = mutableListOf<String>()
        
        try {
            exportUsageSensorLogs()?.let { exportPaths.add(it) }
            exportInterventionLogs()?.let { exportPaths.add(it) }
            exportDailyAdaptiveLimits()?.let { exportPaths.add(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        exportPaths
    }

    private suspend fun exportUsageSensorLogs(): String? {
        val logs = usageSensorDao.getAllLogsList()
        if (logs.isEmpty()) return null

        val file = createExportFile("usage_sensor_logs.csv")
        FileWriter(file).use { writer ->
            // Header
            writer.append("Timestamp,Date,PackageName,RawScreenTimeMs,AppOpeningFrequency,AmbientLightLux\n")
            
            // Data
            logs.forEach { log ->
                writer.append("${log.timestamp},")
                writer.append("\"${dateFormatter.format(Date(log.timestamp))}\",")
                writer.append("\"${log.packageName}\",")
                writer.append("${log.rawScreenTimeMs},")
                writer.append("${log.appOpeningFrequency},")
                writer.append("${log.ambientLightLux}\n")
            }
        }
        return file.absolutePath
    }

    private suspend fun exportInterventionLogs(): String? {
        val logs = interventionDao.getAllLogsList()
        if (logs.isEmpty()) return null

        val file = createExportFile("intervention_logs.csv")
        FileWriter(file).use { writer ->
            // Header
            writer.append("Timestamp,Date,TargetPackageName,QuestionType,DifficultyLevel,ResponseTimeMs,IsCorrect,IsBypassed\n")
            
            // Data
            logs.forEach { log ->
                writer.append("${log.timestamp},")
                writer.append("\"${dateFormatter.format(Date(log.timestamp))}\",")
                writer.append("\"${log.targetPackageName}\",")
                writer.append("\"${log.questionType}\",")
                writer.append("\"${log.difficultyLevel}\",")
                writer.append("${log.responseTimeMs},")
                writer.append("${log.isCorrect},")
                writer.append("${log.isBypassed}\n")
            }
        }
        return file.absolutePath
    }

    private suspend fun exportDailyAdaptiveLimits(): String? {
        val limits = adaptiveLimitDao.getAllLimitsList()
        if (limits.isEmpty()) return null

        val file = createExportFile("daily_adaptive_limits.csv")
        FileWriter(file).use { writer ->
            // Header
            writer.append("DateString,CalculatedLimitMinutes,ActualScreenTimeMinutes,ReclaimedTimeMinutes\n")
            
            // Data
            limits.forEach { limit ->
                writer.append("\"${limit.dateString}\",")
                writer.append("${limit.calculatedLimitMinutes},")
                writer.append("${limit.actualScreenTimeMinutes},")
                writer.append("${limit.reclaimedTimeMinutes}\n")
            }
        }
        return file.absolutePath
    }

    private fun createExportFile(fileName: String): File {
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        return File(exportDir, fileName)
    }
}
