package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.DeviationConfig
import com.makhp.pelukdiri.core.domain.model.DeviationResult
import com.makhp.pelukdiri.core.domain.model.DeviationStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * Deviation Engine (v0.1)
 *
 * Implements the research methodology for calculating usage deviation signal D ∈ [0,1].
 * This engine is stateless and deterministic.
 *
 * All calculations use minutes as the base unit.
 */
@Singleton
class DeviationEngine @Inject constructor(
    private val config: DeviationConfig
) {

    /**
     * Calculates the deviation signal based on current usage and historical context.
     *
     * @param currentUsage Current usage value in minutes.
     * @param history List of recent historical observations in minutes.
     * @return [DeviationResult] containing the signal D and intermediate statistics.
     */
    fun calculate(
        currentUsage: Double,
        history: List<Double>
    ): DeviationResult {
        // 1. Handle insufficient history
        if (history.size < config.minimumHistory) {
            return DeviationResult(
                deviation = null,
                baseline = null,
                mad = null,
                signal = null,
                relativeDeviation = null,
                relativeMagnitude = null,
                status = DeviationStatus.InsufficientHistory
            )
        }

        // 2. Baseline B = median(history)
        val sortedHistory = history.sorted()
        val baseline = median(sortedHistory)

        // 3. MAD M = median(|xi - B|)
        val absoluteDeviations = history.map { abs(it - baseline) }.sorted()
        val mad = median(absoluteDeviations)

        // 4. Positive excess Δ+ = max(0, x - B)
        val deltaPlus = max(0.0, currentUsage - baseline)

        // 5. Stabilize zero/near-zero historical scales so a tiny excess does not
        // saturate D. Raw MAD remains in telemetry; the effective scale is
        // reproducible from the baseline and versioned policy parameters.
        val effectiveMad = max(
            mad,
            max(
                config.minimumMadMinutes,
                baseline * config.minimumMadFractionOfBaseline,
            ),
        )

        // Relative deviation R = Δ+ / effective MAD
        val relativeDeviation = when {
            deltaPlus == 0.0 -> 0.0
            effectiveMad == 0.0 -> Double.POSITIVE_INFINITY
            else -> deltaPlus / effectiveMad
        }

        // Relative magnitude A = Δ+ / max(B, minimum scale)
        val magnitudeScale = max(baseline, config.minimumMadMinutes)
        val relativeMagnitude = when {
            deltaPlus == 0.0 -> 0.0
            magnitudeScale == 0.0 -> Double.POSITIVE_INFINITY
            else -> deltaPlus / magnitudeScale
        }

        // 6. Combined signal S = R(1 + alpha * A)
        val signal = when {
            relativeDeviation == Double.POSITIVE_INFINITY || relativeMagnitude == Double.POSITIVE_INFINITY -> 
                Double.POSITIVE_INFINITY
            else -> relativeDeviation * (1.0 + config.alpha * relativeMagnitude)
        }

        // 7. Final deviation D = (L(S) - L(0)) / (1 - L(0))
        // where L(S) = 1 / (1 + exp(-k(S - s0)))
        // This anchors the logistic so that D(0) = 0 exactly and smoothly.
        val deviation = when (signal) {
            Double.POSITIVE_INFINITY -> 1.0
            else -> {
                val lS = 1.0 / (1.0 + exp(-config.k * (signal - config.s0)))
                val l0 = 1.0 / (1.0 + exp(config.k * config.s0))
                (lS - l0) / (1.0 - l0)
            }
        }

        return DeviationResult(
            deviation = deviation,
            baseline = baseline,
            mad = mad,
            signal = signal,
            relativeDeviation = relativeDeviation,
            relativeMagnitude = relativeMagnitude,
            status = DeviationStatus.Success
        )
    }

    /**
     * Calculates median of a sorted list.
     */
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
