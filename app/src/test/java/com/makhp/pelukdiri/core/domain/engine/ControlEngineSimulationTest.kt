package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.PerformanceMetrics
import org.junit.Test
import java.time.LocalTime

class ControlEngineSimulationTest {

    private val config = ControlConfig(
        lambdaDifficulty = 0.5,
        lambdaFrequency = 0.5,
        performanceEvidenceWindow = 5
    )
    private val sensitivityCalculator = SensitivityCalculator(config)
    private val performanceCalculator = PerformanceCalculator(config)
    private val difficultyController = DifficultyController(config)
    private val frequencyController = FrequencyController(config)
    
    private val engine = ControlEngine(
        config,
        sensitivityCalculator,
        performanceCalculator,
        difficultyController,
        frequencyController
    )

    private fun runScenario(
        name: String,
        deviation: Double?,
        lastPerf: PerformanceMetrics?,
        history: List<Long>,
        lux: Float?,
        bedtime: LocalTime?,
        wakeTime: LocalTime?,
        currentLevel: Int,
        currentTime: LocalTime = LocalTime.of(12, 0)
    ) {
        val result = engine.calculateNextIntervention(
            deviation, lastPerf, history, lux, bedtime, wakeTime, currentLevel, currentTime
        )
        
        println("--- Scenario: $name ---")
        println("Input: D=${deviation ?: "N/A"}, P_last=${lastPerf?.isSuccess ?: "N/A"}, Q_lux=${result.qLux}, Q_time=${result.qTime}")
        println("Engine State: Mode=${result.mode}, Sensitivity=${result.sensitivity}, Performance=${result.performance}")
        println("Difficulty: Signal=${result.difficultyControl}, Target=${result.difficultyTarget}, Level: ${result.currentDifficulty} -> ${result.nextDifficulty}")
        println("Frequency: Interval=${result.intervalMinutes} min")
        println("")
    }

    @Test
    fun simulateScenarios() {
        val establishedHistory = listOf(1000L, 1000L, 1000L, 1000L, 1000L)
        val bedtime = LocalTime.of(22, 0)
        val wakeTime = LocalTime.of(6, 0)

        // Scenario A: Low D + Normal P
        runScenario(
            "A (Low D + Normal P)",
            deviation = 0.1,
            lastPerf = PerformanceMetrics(1000, true, 2),
            history = establishedHistory,
            lux = 1000f,
            bedtime = null,
            wakeTime = null,
            currentLevel = 2
        )

        // Scenario B: High D + High P
        runScenario(
            "B (High D + High P)",
            deviation = 0.8,
            lastPerf = PerformanceMetrics(500, true, 3),
            history = establishedHistory,
            lux = 1000f,
            bedtime = null,
            wakeTime = null,
            currentLevel = 3
        )

        // Scenario C: High D + Low P
        runScenario(
            "C (High D + Low P)",
            deviation = 0.8,
            lastPerf = PerformanceMetrics(1000, false, 4),
            history = establishedHistory,
            lux = 1000f,
            bedtime = null,
            wakeTime = null,
            currentLevel = 4
        )

        // Scenario D: High D + High P + High Q (Strongest v0.1 adaptive tendency)
        runScenario(
            "D (High D + High P + High Q)",
            deviation = 0.9,
            lastPerf = PerformanceMetrics(400, true, 4),
            history = establishedHistory,
            lux = 0f, // Q_lux = 1.0
            bedtime = bedtime,
            wakeTime = wakeTime,
            currentLevel = 4,
            currentTime = LocalTime.of(23, 0) // Q_time = 1.0
        )

        // Scenario E: Low D + High P + High Q
        runScenario(
            "E (Low D + High P + High Q)",
            deviation = 0.05,
            lastPerf = PerformanceMetrics(400, true, 1),
            history = establishedHistory,
            lux = 0f,
            bedtime = bedtime,
            wakeTime = wakeTime,
            currentLevel = 1,
            currentTime = LocalTime.of(23, 0)
        )

        // Scenario F: No usable adaptive data
        runScenario(
            "F (No Data / Fallback)",
            deviation = null,
            lastPerf = null,
            history = emptyList(),
            lux = null,
            bedtime = null,
            wakeTime = null,
            currentLevel = 2
        )

        // Scenario G: High D + High Q + performance history < 5
        runScenario(
            "G (Insufficient History Guard)",
            deviation = 0.9,
            lastPerf = PerformanceMetrics(400, true, 2),
            history = listOf(1000L, 1000L), // only 2
            lux = 0f,
            bedtime = bedtime,
            wakeTime = wakeTime,
            currentLevel = 2,
            currentTime = LocalTime.of(23, 0)
        )
    }
}
