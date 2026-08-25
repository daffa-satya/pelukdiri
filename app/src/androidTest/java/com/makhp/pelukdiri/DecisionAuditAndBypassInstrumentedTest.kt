package com.makhp.pelukdiri

import android.os.Environment
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.makhp.pelukdiri.core.database.PelukDiriDatabase
import com.makhp.pelukdiri.core.database.entity.AppUsageEntity
import com.makhp.pelukdiri.core.database.entity.InterventionDecisionEntity
import com.makhp.pelukdiri.core.database.entity.InterventionLogEntity
import com.makhp.pelukdiri.core.database.export.CsvExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.zip.ZipFile

@RunWith(AndroidJUnit4::class)
class DecisionAuditAndBypassInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: PelukDiriDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(context, PelukDiriDatabase::class.java).build()
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun concurrentBypassWritersCannotExceedQuota() = runBlocking {
        val dao = database.interventionDao()
        val results = withContext(Dispatchers.IO) {
            coroutineScope {
                List(20) { index ->
                    async {
                        dao.insertBypassIfQuotaAvailable(
                            log = InterventionLogEntity(
                                timestamp = 1_000L + index,
                                deviation = 0.4,
                                difficultyControlSignal = 0.5,
                                difficultyLevel = 2,
                                responseTimeMs = 500L,
                                isSuccess = false,
                                isBypassed = true,
                                penaltyAppliedMinutes = 0,
                                challengeType = "PATTERN",
                            ),
                            start = 0L,
                            end = 10_000L,
                            limit = 5,
                        )
                    }
                }.awaitAll()
            }
        }

        assertEquals(5, results.count { it != null })
        assertEquals(5, dao.getBypassCountInInterval(0L, 10_000L))
        assertEquals(5, dao.getAllLogsList().size)
    }

    @Test
    fun decisionAuditIsIncludedInFullExport() = runBlocking {
        database.usageDao().insertAppUsage(
            listOf(
                AppUsageEntity(
                    packageName = "com.example.older-than-14-days",
                    appName = "Old App",
                    usageDurationMillis = 60_000L,
                    lastUsedTimestamp = 1_700_000_000_000L,
                    date = "2020-01-01",
                )
            )
        )
        database.interventionDecisionDao().insert(
            InterventionDecisionEntity(
                timestamp = 1_787_303_917_996L,
                packageName = "com.example,quoted",
                monitoredUsageMinutes = 90.0,
                totalUsageMinutes = 120.0,
                ambientLux = 25f,
                historyCount = 7,
                baselineMedianMinutes = 60.0,
                madMinutes = 10.0,
                deviationSignal = 2.0,
                relativeDeviation = 3.0,
                relativeMagnitude = 0.5,
                deviation = 0.8,
                performance = 0.6,
                qLux = 0.2,
                qTime = 0.3,
                sensitivity = 0.5,
                difficultyControl = 0.4,
                difficultyControlSignal = 0.7,
                difficultyTarget = 3.2,
                currentDifficulty = 3,
                nextDifficulty = 4,
                challengeType = "PATTERN",
                frequencyControl = 0.8,
                normalizedFrequencyControl = 0.9,
                proposedIntervalMinutes = 5.0,
                nextEligibleAt = 1_787_304_217_996L,
                shouldTrigger = true,
                reason = "TRIGGERED",
                controlMode = "PERSONALIZED",
                errorType = null,
            )
        )
        database.interventionDao().insertLog(
            InterventionLogEntity(
                timestamp = 1_787_303_920_000L,
                deviation = 0.8,
                difficultyControlSignal = 0.7,
                difficultyLevel = 4,
                responseTimeMs = 2_004L,
                isSuccess = true,
                isBypassed = false,
                penaltyAppliedMinutes = 0,
                challengeType = "PATTERN",
            )
        )
        val exporter = CsvExporter(
            context,
            database.usageDao(),
            database.usageSensorDao(),
            database.interventionDao(),
            database.interventionDecisionDao(),
            database.interventionNotificationDao(),
            database.adaptiveLimitDao(),
        )

        val export = exporter.exportFullDatabaseToZip().getOrThrow()
        val file = export.archiveFile
        try {
            assertEquals("${Environment.DIRECTORY_DOWNLOADS}/${file.name}", export.savedPath)
            context.contentResolver.query(
                export.downloadUri,
                arrayOf(MediaStore.Downloads.DISPLAY_NAME),
                null,
                null,
                null,
            )!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(file.name, cursor.getString(0))
            }
            ZipFile(file).use { zip ->
                assertEquals(8, zip.entries().toList().size)
                val entry = zip.getEntry("intervention_decisions.csv")
                assertNotNull(entry)
                val csv = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                assertTrue(csv.contains("BaselineMedianMinutes,MADMinutes"))
                assertTrue(csv.contains("\"com.example,quoted\""))
                assertTrue(csv.contains("\"PATTERN\""))
                assertTrue(csv.contains("\"TRIGGERED\",\"PERSONALIZED\""))

                val interventionEntry = zip.getEntry("intervention_logs.csv")
                assertNotNull(interventionEntry)
                val interventionCsv = zip.getInputStream(interventionEntry).bufferedReader().use {
                    it.readText()
                }
                assertEquals(2, interventionCsv.lineSequence().count { it.isNotEmpty() })
                assertTrue(interventionCsv.contains("\"PATTERN\""))
                assertTrue(interventionCsv.contains(",2004,true,false,0\r\n"))

                val appUsageEntry = zip.getEntry("app_usage.csv")
                assertNotNull(appUsageEntry)
                val appUsageCsv = zip.getInputStream(appUsageEntry).bufferedReader().use { it.readText() }
                assertTrue(appUsageCsv.contains("\"com.example.older-than-14-days\""))
                assertTrue(appUsageCsv.contains("\"2020-01-01\""))

                val deviceInfoEntry = zip.getEntry("device_info.txt")
                assertNotNull(deviceInfoEntry)
                val deviceInfo = zip.getInputStream(deviceInfoEntry).bufferedReader().use { it.readText() }
                assertTrue(deviceInfo.contains("app.package_name=${context.packageName}\r\n"))
                assertTrue(deviceInfo.contains("intervention.policy_version=v1.1-failure-streak-decrease\r\n"))
                assertTrue(deviceInfo.contains("device.model="))
                assertTrue(deviceInfo.contains("os.api_level="))
                assertTrue(deviceInfo.contains("battery.level_percent="))
                assertTrue(deviceInfo.contains("permissions.usage_access="))
            }
        } finally {
            context.contentResolver.delete(export.downloadUri, null, null)
            file.delete()
        }
    }
}
