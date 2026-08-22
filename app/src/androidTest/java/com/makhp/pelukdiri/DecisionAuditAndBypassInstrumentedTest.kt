package com.makhp.pelukdiri

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
        val exporter = CsvExporter(
            context,
            database.usageDao(),
            database.usageSensorDao(),
            database.interventionDao(),
            database.interventionDecisionDao(),
            database.interventionNotificationDao(),
            database.adaptiveLimitDao(),
        )

        val file = exporter.exportFullDatabaseToZip().getOrThrow()
        try {
            ZipFile(file).use { zip ->
                assertEquals(7, zip.entries().toList().size)
                val entry = zip.getEntry("intervention_decisions.csv")
                assertNotNull(entry)
                val csv = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                assertTrue(csv.contains("BaselineMedianMinutes,MADMinutes"))
                assertTrue(csv.contains("\"com.example,quoted\""))
                assertTrue(csv.contains("\"PATTERN\""))
                assertTrue(csv.contains("\"TRIGGERED\",\"PERSONALIZED\""))

                val appUsageEntry = zip.getEntry("app_usage.csv")
                assertNotNull(appUsageEntry)
                val appUsageCsv = zip.getInputStream(appUsageEntry).bufferedReader().use { it.readText() }
                assertTrue(appUsageCsv.contains("\"com.example.older-than-14-days\""))
                assertTrue(appUsageCsv.contains("\"2020-01-01\""))
            }
        } finally {
            file.delete()
        }
    }
}
