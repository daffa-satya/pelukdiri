package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.PerformanceMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class PerformanceCalculatorTest {

    private val config = ControlConfig(performanceEvidenceWindow = 5)
    private val calculator = PerformanceCalculator(config)

    @Test
    fun `calculate - incorrect answer produces zero performance`() {
        val current = PerformanceMetrics(responseTimeMs = 1000, isSuccess = false, difficulty = 3)
        val history = listOf(1000L, 1100L, 900L, 1050L, 950L)
        
        val p = calculator.calculate(current, history)
        assertEquals(0.0, p, 0.001)
    }

    @Test
    fun `calculate - insufficient history produces neutral performance`() {
        val current = PerformanceMetrics(responseTimeMs = 1000, isSuccess = true, difficulty = 3)
        val history = listOf(1000L, 1100L, 900L) // only 3
        
        val p = calculator.calculate(current, history)
        assertEquals(0.5, p, 0.001)
    }

    @Test
    fun `calculate - correct fast answer produces high performance`() {
        val current = PerformanceMetrics(responseTimeMs = 500, isSuccess = true, difficulty = 3)
        val history = listOf(1000L, 1000L, 1000L, 1000L, 1000L) // Median = 1000
        
        // S = 1 / (1 + 500/1000) = 1 / 1.5 = 0.666...
        // P = 0.5 + 0.5 * 0.666... = 0.833...
        val p = calculator.calculate(current, history)
        assertEquals(0.833, p, 0.001)
    }

    @Test
    fun `calculate - correct slow answer produces low performance above floor`() {
        val current = PerformanceMetrics(responseTimeMs = 2000, isSuccess = true, difficulty = 3)
        val history = listOf(1000L, 1000L, 1000L, 1000L, 1000L) // Median = 1000
        
        // S = 1 / (1 + 2000/1000) = 1 / 3 = 0.333...
        // P = 0.5 + 0.5 * 0.333... = 0.666...
        val p = calculator.calculate(current, history)
        assertEquals(0.666, p, 0.001)
    }
}
