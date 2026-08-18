package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class SensitivityCalculatorTest {

    private val config = ControlConfig(
        luxDarkReference = 10f,
        luxBrightReference = 500f,
        sleepSensitivityRampMinutes = 90
    )
    private val calculator = SensitivityCalculator(config)

    @Test
    fun `lux sensitivity - log normalized behavior`() {
        // At or below dark reference -> 1.0
        assertEquals(1.0, calculator.calculate(0f, null, null), 0.001)
        assertEquals(1.0, calculator.calculate(10f, null, null), 0.001)
        
        // At or above bright reference -> 0.0
        assertEquals(0.0, calculator.calculate(500f, null, null), 0.001)
        assertEquals(0.0, calculator.calculate(1000f, null, null), 0.001)
        
        // Intermediate value
        // L=100 -> Q = 1 - (ln(101)-ln(11))/(ln(501)-ln(11)) approx 0.418
        assertEquals(0.418, calculator.calculate(100f, null, null), 0.01)
    }

    @Test
    fun `time sensitivity - bedtime ramp`() {
        val bedtime = LocalTime.of(22, 0)
        val wakeTime = LocalTime.of(6, 0)
        
        // 90 minutes before bedtime is 20:30
        assertEquals(0.0, calculator.calculate(500f, bedtime, wakeTime, LocalTime.of(20, 0)), 0.001)
        assertEquals(0.5, calculator.calculate(500f, bedtime, wakeTime, LocalTime.of(21, 15)), 0.001)
        assertEquals(1.0, calculator.calculate(500f, bedtime, wakeTime, LocalTime.of(22, 0)), 0.001)
    }

    @Test
    fun `time sensitivity - sleep interval boundary`() {
        val bedtime = LocalTime.of(22, 0)
        val wakeTime = LocalTime.of(6, 0)
        
        assertEquals(1.0, calculator.calculate(500f, bedtime, wakeTime, LocalTime.of(23, 0)), 0.001)
        assertEquals(1.0, calculator.calculate(500f, bedtime, wakeTime, LocalTime.of(0, 0)), 0.001)
        assertEquals(1.0, calculator.calculate(500f, bedtime, wakeTime, LocalTime.of(5, 59)), 0.001)
        
        // Exact wake time -> 0.0 (Interval [bedtime, wakeTime))
        assertEquals(0.0, calculator.calculate(500f, bedtime, wakeTime, LocalTime.of(6, 0)), 0.001)
        assertEquals(0.0, calculator.calculate(500f, bedtime, wakeTime, LocalTime.of(6, 1)), 0.001)
    }

    @Test
    fun `time sensitivity - midnight crossing ramp`() {
        val bedtime = LocalTime.of(0, 30) // 00:30
        val wakeTime = LocalTime.of(8, 0)
        
        // 90 minutes before is 23:00
        assertEquals(0.0, calculator.calculate(1000f, bedtime, wakeTime, LocalTime.of(22, 59)), 0.001)
        assertEquals(0.5, calculator.calculate(1000f, bedtime, wakeTime, LocalTime.of(23, 45)), 0.001)
        assertEquals(1.0, calculator.calculate(1000f, bedtime, wakeTime, LocalTime.of(0, 30)), 0.001)
    }
}
