package com.makhp.pelukdiri.core.database.export

import android.content.ContentValues
import android.content.Context
import android.app.ActivityManager
import android.app.NotificationManager
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.makhp.pelukdiri.collector.isUsageStatsPermissionGranted
import com.makhp.pelukdiri.core.database.dao.AdaptiveLimitDao
import com.makhp.pelukdiri.core.database.dao.InterventionDao
import com.makhp.pelukdiri.core.database.dao.InterventionDecisionDao
import com.makhp.pelukdiri.core.database.dao.InterventionNotificationDao
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import com.makhp.pelukdiri.core.domain.model.ControlConfig
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
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val usageDao: UsageDao,
    private val usageSensorDao: UsageSensorDao,
    private val interventionDao: InterventionDao,
    private val interventionDecisionDao: InterventionDecisionDao,
    private val interventionNotificationDao: InterventionNotificationDao,
    private val adaptiveLimitDao: AdaptiveLimitDao
) {

    data class ExportResult(
        val archiveFile: File,
        val savedPath: String,
        val downloadUri: Uri,
    )

    suspend fun exportFullDatabaseToZip(): Result<ExportResult> = withContext(Dispatchers.IO) {
        try {
            // Always include every expected CSV. Empty tables still produce a
            // header-only file so exports have a stable, machine-readable schema.
            val csvFiles = listOf(
                exportAppUsage(),
                exportDailySummaries(),
                exportUsageSensorLogs(),
                exportInterventions(),
                exportInterventionLogs(),
                exportInterventionDecisions(),
                exportDailyAdaptiveLimits(),
                exportDeviceInfo(),
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

            val downloadUri = saveToDownloads(zipFile)
            Result.success(
                ExportResult(
                    archiveFile = zipFile,
                    savedPath = "${Environment.DIRECTORY_DOWNLOADS}/${zipFile.name}",
                    downloadUri = downloadUri,
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun saveToDownloads(source: File): Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveToScopedDownloads(source)
    } else {
        saveToLegacyDownloads(source)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToScopedDownloads(source: File): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, source.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = checkNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)) {
            "Unable to create export in Downloads"
        }
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { output ->
                source.inputStream().use { it.copyTo(output) }
            }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToLegacyDownloads(source: File): Uri {
        check(
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        ) { context.getString(com.makhp.pelukdiri.R.string.export_storage_permission_required) }

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        check(downloadsDir.exists() || downloadsDir.mkdirs()) { "Unable to access Downloads" }
        val destination = File(downloadsDir, source.name)
        source.copyTo(destination, overwrite = true)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            destination,
        )
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
            writer.append("ID,Timestamp,Date,Deviation,DifficultyControlSignal,DifficultyLevel,ChallengeType,ResponseTimeMs,IsSuccess,IsBypassed,PenaltyAppliedMinutes\r\n")
            data.forEach {
                writer.append(CsvFormat.row(it.id, it.timestamp, CsvFormat.timestamp(it.timestamp), it.deviation, it.difficultyControlSignal, it.difficultyLevel, it.challengeType, it.responseTimeMs, it.isSuccess, it.isBypassed, it.penaltyAppliedMinutes))
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

    private suspend fun exportInterventionDecisions(): File {
        val data = interventionDecisionDao.getAllList()
        val file = createTempFile("intervention_decisions.csv")
        writerFor(file).use { writer ->
            writer.append(
                "ID,Timestamp,Date,PackageName,MonitoredUsageMinutes,TotalUsageMinutes,AmbientLux,HistoryCount," +
                    "BaselineMedianMinutes,MADMinutes,DeviationSignal,RelativeDeviation,RelativeMagnitude,Deviation," +
                    "Performance,QLux,QTime,Sensitivity,DifficultyControl,DifficultyControlSignal,DifficultyTarget," +
                    "CurrentDifficulty,NextDifficulty,ChallengeType,FrequencyControl,NormalizedFrequencyControl," +
                    "ProposedIntervalMinutes,NextEligibleAt,ShouldTrigger,Reason,ControlMode,ErrorType\r\n"
            )
            data.forEach {
                writer.append(
                    CsvFormat.row(
                        it.id, it.timestamp, CsvFormat.timestamp(it.timestamp), it.packageName,
                        it.monitoredUsageMinutes, it.totalUsageMinutes, it.ambientLux, it.historyCount,
                        it.baselineMedianMinutes, it.madMinutes, it.deviationSignal,
                        it.relativeDeviation, it.relativeMagnitude, it.deviation, it.performance,
                        it.qLux, it.qTime, it.sensitivity, it.difficultyControl,
                        it.difficultyControlSignal, it.difficultyTarget, it.currentDifficulty,
                        it.nextDifficulty, it.challengeType, it.frequencyControl,
                        it.normalizedFrequencyControl, it.proposedIntervalMinutes,
                        it.nextEligibleAt, it.shouldTrigger, it.reason, it.controlMode, it.errorType,
                    )
                )
            }
        }
        return file
    }

    @Suppress("DEPRECATION")
    private fun exportDeviceInfo(): File {
        val file = createTempFile("device_info.txt")
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val metrics = context.resources.displayMetrics
        val data = linkedMapOf(
            "export.generated_at_utc" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            "app.package_name" to context.packageName,
            "app.version_name" to (packageInfo.versionName ?: ""),
            "app.version_code" to packageInfo.longVersionCode,
            "intervention.policy_version" to ControlConfig.POLICY_VERSION,
            "app.first_install_time_utc" to CsvFormat.timestamp(packageInfo.firstInstallTime),
            "app.last_update_time_utc" to CsvFormat.timestamp(packageInfo.lastUpdateTime),
            "device.manufacturer" to Build.MANUFACTURER,
            "device.brand" to Build.BRAND,
            "device.model" to Build.MODEL,
            "device.device" to Build.DEVICE,
            "device.product" to Build.PRODUCT,
            "device.hardware" to Build.HARDWARE,
            "device.board" to Build.BOARD,
            "device.bootloader" to Build.BOOTLOADER,
            "device.supported_abis" to Build.SUPPORTED_ABIS.joinToString(","),
            "os.android_release" to Build.VERSION.RELEASE,
            "os.api_level" to Build.VERSION.SDK_INT,
            "os.security_patch" to Build.VERSION.SECURITY_PATCH,
            "os.build_id" to Build.ID,
            "os.build_display" to Build.DISPLAY,
            "os.build_fingerprint" to Build.FINGERPRINT,
            "os.build_type" to Build.TYPE,
            "os.build_tags" to Build.TAGS,
            "os.base_os" to Build.VERSION.BASE_OS,
            "os.incremental" to Build.VERSION.INCREMENTAL,
            "runtime.device_uptime_ms" to SystemClock.elapsedRealtime(),
            "runtime.available_processors" to Runtime.getRuntime().availableProcessors(),
            "locale.language_tag" to Locale.getDefault().toLanguageTag(),
            "locale.time_zone" to TimeZone.getDefault().id,
            "display.width_pixels" to metrics.widthPixels,
            "display.height_pixels" to metrics.heightPixels,
            "display.density_dpi" to metrics.densityDpi,
            "display.scaled_density" to metrics.scaledDensity,
            "memory.total_bytes" to memoryInfo.totalMem,
            "memory.available_bytes" to memoryInfo.availMem,
            "memory.low_memory" to memoryInfo.lowMemory,
            "storage.internal_total_bytes" to context.filesDir.totalSpace,
            "storage.internal_available_bytes" to context.filesDir.usableSpace,
            "battery.level_percent" to batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            "battery.is_charging" to batteryManager.isCharging,
            "permissions.usage_access" to isUsageStatsPermissionGranted(context),
            "permissions.accessibility_enabled" to (Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1),
            "permissions.notifications_enabled" to notificationManager.areNotificationsEnabled(),
            "permissions.battery_optimization_ignored" to powerManager.isIgnoringBatteryOptimizations(context.packageName),
        )
        writerFor(file).use { writer ->
            data.forEach { (key, value) -> writer.append(key).append('=').append(value.toString()).append("\r\n") }
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
