package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.DeviationConfig
import com.makhp.pelukdiri.core.domain.model.DeviationResult
import com.makhp.pelukdiri.core.domain.model.DeviationStatus
import org.junit.Before
import org.junit.Test

class DeviationSimulationTest {

    private lateinit var engine: DeviationEngine

    @Before
    fun setup() {
        // Phase 4 Frozen Parameters via DeviationConfig
        val config = DeviationConfig(alpha = 0.1, k = 1.0, s0 = 1.0)
        engine = DeviationEngine(config)
    }

    private fun runSim(profileName: String, history: List<Double>, usagePoints: List<Double>) {
        println("\n=== PROFILE: $profileName ===")
        println("History: $history")
        
        usagePoints.forEach { usage ->
            val res = engine.calculate(usage, history)
            report(usage, res)
        }
    }

    private fun report(usage: Double, res: DeviationResult) {
        if (res.status == DeviationStatus.InsufficientHistory) {
            println("Usage: ${usage}m | Status: INSUFFICIENT_HISTORY")
            return
        }
        
        val d = res.deviation ?: 0.0
        val s = res.signal ?: 0.0
        val r = res.relativeDeviation ?: 0.0
        val a = res.relativeMagnitude ?: 0.0
        
        println(String.format(
            "Usage: %6.1fm | B: %6.1f | M: %5.2f | Δ+: %6.1f | R: %6.2f | A: %5.2f | S: %7.3f | D: %5.3f | %s",
            usage, res.baseline, res.mad, (usage - (res.baseline ?: 0.0)).coerceAtLeast(0.0), 
            r, a, s, d, res.status
        ))
    }

    @Test
    fun simulationProfiles() {
        // PROFILE A: HIGHLY CONSISTENT USER
        runSim("A: HIGHLY CONSISTENT USER", 
            listOf(120.0, 120.0, 120.0, 120.0, 120.0, 120.0, 120.0),
            listOf(60.0, 120.0, 120.1, 180.0, 360.0, 1200.0)
        )

        // PROFILE B: FLUCTUATING USER
        runSim("B: FLUCTUATING USER", 
            listOf(60.0, 180.0, 120.0, 240.0, 120.0, 180.0, 60.0),
            listOf(60.0, 120.0, 180.0, 240.0, 300.0, 480.0)
        )

        // PROFILE C: LOW BASELINE
        runSim("C: LOW BASELINE", 
            listOf(25.0, 30.0, 35.0, 30.0, 25.0, 35.0, 30.0),
            listOf(30.0, 45.0, 60.0, 90.0, 120.0)
        )

        // PROFILE D: HIGH BASELINE
        runSim("D: HIGH BASELINE", 
            listOf(480.0, 480.0, 540.0, 480.0, 420.0, 480.0, 540.0),
            listOf(480.0, 600.0, 720.0, 840.0, 1080.0)
        )

        // PROFILE E: EXTREME USAGE
        runSim("E: EXTREME USAGE", 
            listOf(120.0, 120.0, 120.0, 120.0, 120.0, 120.0, 120.0),
            listOf(1200.0, 1380.0, 1440.0)
        )

        // PROFILE F: INSUFFICIENT HISTORY
        println("\n=== PROFILE: F: INSUFFICIENT HISTORY ===")
        val histories = listOf(
            emptyList<Double>(),
            listOf(60.0),
            listOf(60.0, 60.0, 60.0),
            listOf(60.0, 60.0, 60.0, 60.0, 60.0, 60.0)
        )
        histories.forEach { h ->
            println("History size: ${h.size}")
            val res = engine.calculate(90.0, h)
            report(90.0, res)
        }

        // PROFILE G: BASELINE ZERO
        runSim("G: BASELINE ZERO", 
            listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            listOf(0.0, 1.0, 60.0, 120.0)
        )
        
        // org.junit.Assert.assertTrue("Showing simulation results", false) // Removed intentional failure
    }
}
