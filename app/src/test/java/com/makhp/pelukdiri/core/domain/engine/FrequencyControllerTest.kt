package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequencyControllerTest {

    private val config = ControlConfig(
        lambdaFrequency = 0.5,
        minFrequencyMinutes = 3.0,
        maxFrequencyMinutes = 30.0
    )
    private val controller = FrequencyController(config)

    @Test
    fun `calculate - base mapping`() {
        // D=0.0, Q=0.0 -> C_F = 0.0 -> Interval = 30 - 27*0 = 30.0
        val res1 = controller.calculate(0.0, 0.0)
        assertEquals(30.0, res1.intervalMinutes, 0.001)

        // D=0.5, Q=0.0 -> C_F = 0.5 -> Interval = 30 - 27*0.5 = 16.5
        val res2 = controller.calculate(0.5, 0.0)
        assertEquals(16.5, res2.intervalMinutes, 0.001)

        // D=1.0, Q=0.0 -> C_F = 1.0 -> Interval = 30 - 27*1 = 3.0
        val res3 = controller.calculate(1.0, 0.0)
        assertEquals(3.0, res3.intervalMinutes, 0.001)
    }

    @Test
    fun `calculate - sensitivity shortens interval`() {
        // D=0.5, Q=0.0 -> C_F = 0.5 -> Interval = 16.5
        val res1 = controller.calculate(0.5, 0.0)
        // D=0.5, Q=1.0 -> C_F = 0.5 + 0.5 * 1.0 * 0.5 = 0.75 -> Interval = 30 - 27*0.75 = 9.75
        val res2 = controller.calculate(0.5, 1.0)
        
        assertTrue(res2.intervalMinutes < res1.intervalMinutes)
        assertEquals(9.75, res2.intervalMinutes, 0.001)
    }

    @Test
    fun `calculate - bounds clamping`() {
        // D=1.0, Q=1.0 -> C_F = 1.0 -> Interval = 3.0
        val res = controller.calculate(1.0, 1.0)
        assertEquals(3.0, res.intervalMinutes, 0.001)
    }
}
