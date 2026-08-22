package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.DeviationConfig
import com.makhp.pelukdiri.core.domain.model.DeviationStatus
import com.makhp.pelukdiri.core.domain.model.HistoricalConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.exp

class DeviationEngineTest {

    // 1. Verify Default Configuration values
    @Test
    fun `default configuration matches frozen v0_1 parameters`() {
        val config = DeviationConfig()
        assertEquals(0.1, config.alpha, 0.0)
        assertEquals(0.75, config.k, 0.0)
        assertEquals(2.0, config.s0, 0.0)
        assertEquals(7, config.minimumHistory)
    }

    // 2. Exactly 7 observations produce a valid result
    @Test
    fun `exactly 7 observations produces valid result`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = List(HistoricalConfig.MINIMUM_HISTORY_DAYS) { 60.0 }
        val result = engine.calculate(90.0, history)
        assertEquals(DeviationStatus.Success, result.status)
        assertTrue(result.deviation != null)
    }

    // 3. Fewer than 7 observations return InsufficientHistory
    @Test
    fun `fewer than 7 observations returns insufficient history`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = List(HistoricalConfig.MINIMUM_HISTORY_DAYS - 1) { 60.0 }
        val result = engine.calculate(90.0, history)
        assertEquals(DeviationStatus.InsufficientHistory, result.status)
        assertNull(result.deviation)
        assertNull(result.baseline)
        assertNull(result.mad)
    }

    // 4. currentUsage == baseline -> deviation == 0.0
    // 18. Anchored logistic: S = 0 -> D = 0 exactly.
    @Test
    fun `usage equal to baseline results in exactly zero deviation`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = List(HistoricalConfig.HISTORY_SAMPLE_DAYS) { 60.0 }
        val result = engine.calculate(60.0, history)
        assertEquals(0.0, result.signal!!, 0.0)
        assertEquals(0.0, result.deviation!!, 0.0)
    }

    // 5. currentUsage < baseline -> deviation == 0.0
    @Test
    fun `usage below baseline results in exactly zero deviation`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = List(HistoricalConfig.HISTORY_SAMPLE_DAYS) { 60.0 }
        val result = engine.calculate(30.0, history)
        assertEquals(0.0, result.signal!!, 0.0)
        assertEquals(0.0, result.deviation!!, 0.0)
    }

    // 6. currentUsage > baseline -> deviation > 0
    @Test
    fun `usage above baseline results in positive deviation`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = variedHistory() // B=60, M=5
        val result = engine.calculate(61.0, history)
        assertTrue(result.deviation!! > 0.0)
    }

    // 7. Correct median calculation
    // 8. Correct MAD calculation
    // 9. Correct R
    // 10. Correct A
    // 11. Correct S
    @Test
    fun `calculates correct statistics and signals`() {
        val config = DeviationConfig(alpha = 0.1)
        val engine = DeviationEngine(config)
        
        // history: [50, 55, 60, 60, 60, 65, 70]
        // B = median = 60
        // absolute deviations: [10, 5, 0, 0, 0, 5, 10]
        // sorted abs dev: [0, 0, 0, 5, 5, 10, 10] -> M = 5
        val history = variedHistory()
        
        // x = 90
        // Δ+ = 90 - 60 = 30
        // R = 30 / 5 = 6.0
        // A = 30 / 60 = 0.5
        // S = 6.0 * (1 + 0.1 * 0.5) = 6.0 * 1.05 = 6.3
        val result = engine.calculate(90.0, history)
        
        assertEquals(60.0, result.baseline!!, 0.0)
        assertEquals(5.0, result.mad!!, 0.0)
        assertEquals(6.0, result.relativeDeviation!!, 0.0)
        assertEquals(0.5, result.relativeMagnitude!!, 0.0)
        assertEquals(6.3, result.signal!!, 0.0001)
    }

    // 12. MAD = 0 with zero excess -> D = 0
    @Test
    fun `MAD is zero and no excess results in zero deviation`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = List(HistoricalConfig.HISTORY_SAMPLE_DAYS) { 60.0 }
        val result = engine.calculate(60.0, history)
        assertEquals(0.0, result.mad!!, 0.0)
        assertEquals(0.0, result.signal!!, 0.0)
        assertEquals(0.0, result.deviation!!, 0.0)
    }

    // 13. MAD = 0 with positive excess -> D = 1
    @Test
    fun `MAD is zero and positive excess results in saturation`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = List(HistoricalConfig.HISTORY_SAMPLE_DAYS) { 60.0 }
        val result = engine.calculate(60.1, history)
        assertEquals(Double.POSITIVE_INFINITY, result.signal!!, 0.0)
        assertEquals(1.0, result.deviation!!, 0.0)
    }

    // 14. Baseline = 0 with positive excess -> D = 1
    @Test
    fun `baseline is zero and positive excess results in saturation`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = List(HistoricalConfig.HISTORY_SAMPLE_DAYS) { 0.0 }
        val result = engine.calculate(0.1, history)
        assertEquals(Double.POSITIVE_INFINITY, result.signal!!, 0.0)
        assertEquals(1.0, result.deviation!!, 0.0)
    }

    // 15. Output is always within [0,1] for valid finite inputs
    @Test
    fun `output is always within inclusive range 0 to 1`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = variedHistory()
        for (usage in 0..2000 step 5) {
            val result = engine.calculate(usage.toDouble(), history)
            assertTrue("Value $usage produced D=${result.deviation}", result.deviation!! in 0.0..1.0)
        }
    }

    // 16. Determinism: identical input + identical config -> identical output
    @Test
    fun `determinism test`() {
        val config = DeviationConfig()
        val engine1 = DeviationEngine(config)
        val engine2 = DeviationEngine(config)
        val history = variedHistory()
        val usage = 120.0
        val res1 = engine1.calculate(usage, history)
        val res2 = engine2.calculate(usage, history)
        assertEquals(res1.deviation, res2.deviation)
        assertEquals(res1.signal, res2.signal)
    }

    // 17. Monotonicity: for fixed history, increasing currentUsage must never decrease D
    @Test
    fun `monotonicity test`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = variedHistory()
        var lastD = -1.0
        for (usage in 0..1000) {
            val currentD = engine.calculate(usage.toDouble(), history).deviation!!
            assertTrue("Monotonicity failed at usage $usage: $currentD < $lastD", currentD >= lastD)
            lastD = currentD
        }
    }

    // 19. Very large positive signal: D approaches/saturates at 1
    @Test
    fun `saturation test for large signal`() {
        val engine = DeviationEngine(DeviationConfig())
        val history = variedHistory()
        val result = engine.calculate(10000.0, history)
        assertEquals(1.0, result.deviation!!, 0.000001)
    }

    // 20. Configuration can be supplied explicitly for calibration/testing without mutating shared engine state
    @Test
    fun `configuration can be supplied explicitly`() {
        val history = variedHistory()
        val usage = 120.0
        
        val engine1 = DeviationEngine(DeviationConfig(k = 0.5))
        val engine2 = DeviationEngine(DeviationConfig(k = 2.0))
        
        val res1 = engine1.calculate(usage, history)
        val res2 = engine2.calculate(usage, history)
        
        assertTrue(res1.deviation != res2.deviation)
    }

    private fun variedHistory(): List<Double> =
        listOf(50.0, 55.0, 60.0, 60.0, 60.0, 65.0, 70.0).flatMap { listOf(it, it) }
}
