package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.PerformanceMetrics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PerformanceCalculator @Inject constructor(
    private val config: ControlConfig,
) {
    /**
     * Calculates the performance score P ∈ [0,1].
     *
     * @param current The latest performance metric.
     * @param history List of recent successful valid response times for the SAME difficulty.
     * @return Performance score P.
     */
    fun calculate(
        current: PerformanceMetrics,
        history: List<Long>,
    ): Double {
        if (!current.isSuccess) return 0.0

        if (history.size < config.performanceEvidenceWindow) {
            return 0.5 // Neutral performance
        }

        val baseline = median(history.map { it.toDouble() }.sorted())
        if (baseline <= 0.0) return 1.0 

        val speedScore = 1.0 / (1.0 + (current.responseTimeMs.toDouble() / baseline))
        
        // P = 0.5 + 0.5 * S
        return (config.performanceCorrectnessFloor + ((1.0 - config.performanceCorrectnessFloor) * speedScore))
            .coerceIn(0.0, 1.0)
    }

    private fun median(sortedList: List<Double>): Double {
        if (sortedList.isEmpty()) return 0.0
        val size = sortedList.size
        return if (size % 2 == 0) {
            (sortedList[size / 2 - 1] + sortedList[size / 2]) / 2.0
        } else {
            sortedList[size / 2]
        }
    }
}
