package com.makhp.pelukdiri

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class EventSequenceAuditTest {

    private val TAG = "SEQUENCE_AUDIT"

    @Test
    fun auditAug9Sequences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val date = LocalDate.of(2026, 8, 9)
        val zoneId = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEnd = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        
        val usageEvents = usm.queryEvents(dayStart, dayEnd)
        val events = mutableListOf<UsageEvents.Event>()
        while (usageEvents.hasNextEvent()) {
            val e = UsageEvents.Event()
            usageEvents.getNextEvent(e)
            events.add(e)
        }

        Log.d(TAG, "--- AUG 9 EVENT SEQUENCE AUDIT ---")
        Log.d(TAG, "Total Events: ${events.size}")

        // 1. Check for Duplicate RESUMES for same package
        var lastEvent: UsageEvents.Event? = null
        var doubleResumes = 0
        events.forEach { e ->
            if (e.eventType == 1 && lastEvent?.eventType == 1 && e.packageName == lastEvent?.packageName) {
                doubleResumes++
            }
            lastEvent = e
        }
        Log.d(TAG, "Consecutive RESUMES for same package: $doubleResumes")

        // 2. Check for Gaps (PAUSE -> RESUME same package)
        val gaps = mutableListOf<Long>()
        for (i in 0 until events.size - 1) {
            val e1 = events[i]
            val e2 = events[i+1]
            if (e1.eventType == 2 && e2.eventType == 1 && e1.packageName == e2.packageName) {
                gaps.add(e2.timeStamp - e1.timeStamp)
            }
        }
        
        Log.d(TAG, "Same-package PAUSE -> RESUME gaps count: ${gaps.size}")
        val buckets = listOf(10, 50, 100, 250, 500, 1000, 5000)
        buckets.forEach { b ->
             Log.d(TAG, "  Gaps < $b ms: ${gaps.count { it < b }}")
        }
        Log.d(TAG, "  Gaps > 1000 ms: ${gaps.count { it > 1000 }}")
        if (gaps.any { it > 5000 }) {
            Log.d(TAG, "  Max gap: ${gaps.maxOrNull()} ms")
        }

        // 3. Check for open sessions at end of day
        val lastPerPkg = events.groupBy { it.packageName }.mapValues { it.value.last() }
        val openAtEnd = lastPerPkg.filter { it.value.eventType == 1 }
        Log.d(TAG, "Packages open at end of day: ${openAtEnd.keys}")
    }
}
