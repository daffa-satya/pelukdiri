package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import java.time.Duration
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.max

@Singleton
class SensitivityCalculator @Inject constructor(
    private val config: ControlConfig,
) {
    /**
     * Calculates the sensitivity modifier Q = max(Q_lux, Q_time).
     */
    fun calculate(
        lux: Float?,
        bedtime: LocalTime?,
        wakeTime: LocalTime?,
        currentTime: LocalTime = LocalTime.now(),
    ): Double {
        val qLux = calculateLuxSensitivity(lux)
        val qTime = calculateTimeSensitivity(bedtime, wakeTime, currentTime)
        return max(qLux, qTime)
    }

    private fun calculateLuxSensitivity(lux: Float?): Double {
        if (lux == null) return 0.0
        
        val l = lux.toDouble()
        val lDark = config.luxDarkReference.toDouble()
        val lBright = config.luxBrightReference.toDouble()
        
        val logL = ln(1.0 + l)
        val logDark = ln(1.0 + lDark)
        val logBright = ln(1.0 + lBright)
        
        val q = 1.0 - (logL - logDark) / (logBright - logDark)
        return q.coerceIn(0.0, 1.0)
    }

    private fun calculateTimeSensitivity(
        bedtime: LocalTime?,
        wakeTime: LocalTime?,
        currentTime: LocalTime
    ): Double {
        if ((bedtime == null) || (wakeTime == null)) return 0.0

        // Check if current time is within sleep interval [bedtime, wakeTime)
        if (isTimeBetween(currentTime, bedtime, wakeTime)) {
            return 1.0
        }

        // Check for ramp period: 90 minutes before bedtime
        val rampMinutes = config.sleepSensitivityRampMinutes.toLong()
        val rampStartTime = bedtime.minusMinutes(rampMinutes)
        
        if (isTimeBetween(currentTime, rampStartTime, bedtime)) {
            val totalRampSeconds = rampMinutes * 60.0
            val secondsIntoRamp = getSecondsBetween(rampStartTime, currentTime)
            return (secondsIntoRamp / totalRampSeconds).coerceIn(0.0, 1.0)
        }

        return 0.0
    }

    private fun isTimeBetween(target: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (start.isBefore(end)) {
            !target.isBefore(start) && target.isBefore(end)
        } else {
            // Midnight crossing
            !target.isBefore(start) || target.isBefore(end)
        }
    }

    private fun getSecondsBetween(start: LocalTime, end: LocalTime): Double {
        val duration = Duration.between(start, end)
        val seconds = duration.seconds
        return if (seconds < 0) {
            // Cross midnight
            seconds + 24 * 3600.0
        } else {
            seconds.toDouble()
        }
    }
}
