package com.makhp.pelukdiri

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.makhp.pelukdiri.collector.AppUsageCollector
import com.makhp.pelukdiri.collector.UsageEventCollector
import com.makhp.pelukdiri.collector.UsageEventReconstructor
import com.makhp.pelukdiri.collector.ScreenInteractiveReconstructor
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class UsageEventCollectorValidationTest {

    @Test
    fun validateAug5TopApps() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val appUsageCollector = AppUsageCollector(context)
        val reconstructor = UsageEventReconstructor()
        val collector = UsageEventCollector(context, appUsageCollector, reconstructor, ScreenInteractiveReconstructor())
        
        val target = LocalDate.of(2026, 8, 5)
        val usage = collector.getUsageForDay(target)
        
        println("--- AUG 5 TOP APPS ---")
        usage.sortedByDescending { it.usageDurationMillis }.take(5).forEach { app ->
            println("${app.packageName}: ${app.usageDurationMillis / 60000.0} min")
        }
    }
}
