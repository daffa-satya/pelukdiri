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

    @Test
    fun `calculate - configured interval bounds define the full mapping`() {
        val configured = FrequencyController(
            ControlConfig(
                lambdaFrequency = 0.5,
                minFrequencyMinutes = 5.0,
                maxFrequencyMinutes = 45.0
            )
        )

        assertEquals(45.0, configured.calculate(0.0, 0.0).intervalMinutes, 0.001)
        assertEquals(25.0, configured.calculate(0.5, 0.0).intervalMinutes, 0.001)
        assertEquals(5.0, configured.calculate(1.0, 0.0).intervalMinutes, 0.001)
    }

    @Test
    fun `production floor is 15 minutes below half of adaptive limit`() {
        val production = FrequencyController(ControlConfig.CANDIDATE_3)

        assertEquals(15.0, production.calculate(1.0, 1.0, 0.499).intervalMinutes, 0.001)
    }

    @Test
    fun `production floor is 10 minutes from half until 80 percent`() {
        val production = FrequencyController(ControlConfig.CANDIDATE_3)

        assertEquals(10.0, production.calculate(1.0, 1.0, 0.5).intervalMinutes, 0.001)
        assertEquals(10.0, production.calculate(1.0, 1.0, 0.799).intervalMinutes, 0.001)
    }

    @Test
    fun `production allows full range at 80 percent or above`() {
        val production = FrequencyController(ControlConfig.CANDIDATE_3)

        assertEquals(3.0, production.calculate(1.0, 1.0, 0.8).intervalMinutes, 0.001)
        assertEquals(3.0, production.calculate(1.0, 1.0, 1.2).intervalMinutes, 0.001)
    }

    @Test
    fun `missing progress and historical policy preserve original mapping`() {
        val production = FrequencyController(ControlConfig.CANDIDATE_3)

        assertEquals(3.0, production.calculate(1.0, 1.0, null).intervalMinutes, 0.001)
        assertEquals(3.0, controller.calculate(1.0, 1.0, 0.1).intervalMinutes, 0.001)
    }
}
