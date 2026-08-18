package com.makhp.pelukdiri

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.makhp.pelukdiri.collector.UsageEvent
import com.makhp.pelukdiri.collector.UsageEventReconstructor
import com.makhp.pelukdiri.core.database.PelukDiriDatabase
import com.makhp.pelukdiri.core.database.entity.AppUsageEntity
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

@RunWith(AndroidJUnit4::class)
class ForensicUsageEventsTest {

    private val tag = "USAGE_FORENSIC"
    private val zoneId: ZoneId = ZoneId.of("Asia/Jakarta")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    @Test
    fun forensicAuditAug10() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val reconstructor = UsageEventReconstructor()
        val db = Room.databaseBuilder(context, PelukDiriDatabase::class.java, "pelukdiri_db")
            .fallbackToDestructiveMigration() // Should match app config or be safe
            .build()
        val dao = db.usageDao()

        val date = LocalDate.of(2026, 8, 10)
        val dateStr = date.toString()
        val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val contextStart = dayStart - 24L * 60L * 60L * 1000L

        Log.d(tag, "Audit for $dateStr (Jakarta Time)")

        // TASK 1: Inspect PELUKDIRI data
        Log.d(tag, "--- TASK 1: DATABASE INSPECTION ---")
        val appUsages = dao.getAppUsageByDate(dateStr).firstOrNull() ?: emptyList()
        val instagramRecord = appUsages.find { it.packageName == "com.instagram.android" }
        val dailySummary = dao.getDailySummary(dateStr).firstOrNull()

        if (instagramRecord != null) {
            Log.d(tag, "Stored Instagram Record:")
            Log.d(tag, "  appName: ${instagramRecord.appName}")
            Log.d(tag, "  durationMillis: ${instagramRecord.usageDurationMillis}")
            Log.d(tag, "  formatted: ${formatDuration(instagramRecord.usageDurationMillis)}")
            Log.d(tag, "  date key: ${instagramRecord.date}")
            Log.d(tag, "  lastUsedTimestamp: ${formatTime(instagramRecord.lastUsedTimestamp)}")
        } else {
            Log.d(tag, "Stored Instagram Record: NOT FOUND")
        }

        if (dailySummary != null) {
            Log.d(tag, "Stored Daily Summary:")
            Log.d(tag, "  totalScreenTime: ${formatDuration(dailySummary.totalScreenTimeMillis)}")
            Log.d(tag, "  mostUsedApp: ${dailySummary.mostUsedApp}")
        } else {
            Log.d(tag, "Stored Daily Summary: NOT FOUND")
        }

        // TASK 2: Independent Reconstruction
        Log.d(tag, "--- TASK 2: INDEPENDENT RECONSTRUCTION ---")
        val contextEvents = fetchEvents(usageStatsManager, contextStart, dayStart)
        val initialState = findStateAtTimestamp(contextEvents)
        val events = fetchEvents(usageStatsManager, dayStart, dayEnd)

        Log.d(tag, "initialState at dayStart: ${initialState?.packageName ?: "NONE"} type=${initialState?.type}")

        val rawSessions = reconstructor.reconstructSessions(
            events = events,
            queryEnd = dayEnd,
            initialPackage = initialState?.packageName,
            initialStartTime = initialState?.timestamp ?: dayStart
        )

        Log.d(tag, "Sessions for Instagram:")
        val instagramSessions = rawSessions.filter { it.packageName == "com.instagram.android" }
        instagramSessions.forEach { session ->
            val actualStart = max(session.startTime, dayStart)
            val actualEnd = min(session.endTime, dayEnd)
            if (actualStart < actualEnd) {
                Log.d(tag, "  ${formatTime(actualStart)} -> ${formatTime(actualEnd)} = ${formatDuration(actualEnd - actualStart)}")
            }
        }

        val usageMap = reconstructor.aggregateUsage(rawSessions, dayStart, dayEnd)
        val independentTotal = usageMap["com.instagram.android"]?.duration ?: 0L
        Log.d(tag, "Independent Total Instagram: ${formatDuration(independentTotal)} ($independentTotal ms)")

        // TASK 3: Comparison
        Log.d(tag, "--- TASK 3: COMPARISON ---")
        val storedTotal = instagramRecord?.usageDurationMillis ?: 0L
        Log.d(tag, "One Sec Reference: ~1h 50m (6600000 ms)")
        Log.d(tag, "PELUKDIRI stored: ${formatDuration(storedTotal)} ($storedTotal ms)")
        Log.d(tag, "Independent reconstructed: ${formatDuration(independentTotal)} ($independentTotal ms)")

        // TASK 5: Conservation
        Log.d(tag, "--- TASK 5: CONSERVATION ---")
        val screenInteractiveMillis = calculateScreenInteractiveMillis(events, dayStart, dayEnd)
        val allPackageTotal = usageMap.values.sumOf { it.duration }
        Log.d(tag, "Total Screen Interactive: ${formatDuration(screenInteractiveMillis)}")
        Log.d(tag, "Sum of all app usage: ${formatDuration(allPackageTotal)}")
        Log.d(tag, "Conservation OK (Sum <= Screen): ${allPackageTotal <= screenInteractiveMillis}")
        Log.d(tag, "Instagram OK (Insta <= Screen): ${independentTotal <= screenInteractiveMillis}")

        // TASK 6: Idempotency
        Log.d(tag, "--- TASK 6: IDEMPOTENCY ---")
        val secondRunTotal = reconstructor.aggregateUsage(
            reconstructor.reconstructSessions(events, dayEnd, initialState?.packageName, initialState?.timestamp ?: dayStart),
            dayStart, dayEnd
        )["com.instagram.android"]?.duration ?: 0L
        Log.d(tag, "Second run total: $secondRunTotal ms")
        Log.d(tag, "Idempotency OK: ${independentTotal == secondRunTotal}")

        db.close()
    }


    private fun fetchEvents(
        usageStatsManager: UsageStatsManager,
        startTime: Long,
        endTime: Long
    ): List<UsageEvent> {
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        val result = mutableListOf<UsageEvent>()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            result.add(
                UsageEvent(
                    packageName = event.packageName ?: "",
                    timestamp = event.timeStamp,
                    type = event.eventType
                )
            )
        }
        return result
    }

    private fun findStateAtTimestamp(events: List<UsageEvent>): UsageEvent? {
        val lastResumed = events.lastOrNull { it.type == UsageEventReconstructor.ACTIVITY_RESUMED } ?: return null
        val closer = events.lastOrNull {
            (it.type == UsageEventReconstructor.ACTIVITY_PAUSED || it.type == UsageEventReconstructor.SCREEN_NON_INTERACTIVE) &&
                it.timestamp >= lastResumed.timestamp
        }
        return if (closer == null) lastResumed else null
    }

    private fun calculateScreenInteractiveMillis(
        events: List<UsageEvent>,
        rangeStart: Long,
        rangeEnd: Long
    ): Long {
        var total = 0L
        var screenOnAt: Long? = null
        events.sortedBy { it.timestamp }.forEach { event ->
            if (event.type == UsageEvents.Event.SCREEN_INTERACTIVE) {
                screenOnAt = event.timestamp
            } else if (event.type == UsageEventReconstructor.SCREEN_NON_INTERACTIVE) {
                val start = screenOnAt
                if (start != null) {
                    val clippedStart = max(start, rangeStart)
                    val clippedEnd = min(event.timestamp, rangeEnd)
                    if (clippedStart < clippedEnd) {
                        total += clippedEnd - clippedStart
                    }
                }
                screenOnAt = null
            }
        }
        return total
    }

    private fun formatEvent(event: UsageEvent): String {
        return "${formatTime(event.timestamp)} type=${event.type} package=${event.packageName}"
    }

    private fun formatTime(timestamp: Long): String {
        return Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDateTime().format(timeFormatter)
    }

    private fun formatDuration(duration: Long): String {
        val hours = duration / 3_600_000
        val minutes = (duration % 3_600_000) / 60_000
        val seconds = (duration % 60_000) / 1_000
        val millis = duration % 1_000
        return "%02dh %02dm %02d.%03ds".format(hours, minutes, seconds, millis)
    }
}
