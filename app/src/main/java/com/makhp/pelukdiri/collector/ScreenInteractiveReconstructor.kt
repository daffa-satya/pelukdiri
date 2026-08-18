package com.makhp.pelukdiri.collector

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class ScreenInteractiveReconstructor @Inject constructor() {
    companion object {
        const val SCREEN_INTERACTIVE = 15
        const val SCREEN_NON_INTERACTIVE = 16
    }

    /**
     * Calculates total screen-interactive time (in milliseconds) for the given events within the day bounds.
     * Uses only SCREEN_INTERACTIVE and SCREEN_NON_INTERACTIVE events.
     *
     * @param events List of usage events for the day.
     * @param dayStart Start of the day in milliseconds.
     * @param queryEnd End of the query window in milliseconds.
     * @param initialStartTime If the screen was already interactive at dayStart, this is the timestamp of the last SCREEN_INTERACTIVE event.
     */
    fun calculateTotalScreenOn(
        events: List<UsageEvent>,
        dayStart: Long,
        queryEnd: Long,
        initialStartTime: Long? = null
    ): Long {
        val sorted = events.sortedBy { it.timestamp }
        var total = 0L
        var interactiveStart: Long? = initialStartTime

        for (event in sorted) {
            when (event.type) {
                SCREEN_INTERACTIVE -> {
                    if (interactiveStart == null) {
                        interactiveStart = event.timestamp
                    }
                }
                SCREEN_NON_INTERACTIVE -> {
                    interactiveStart?.let { start ->
                        val intervalStart = max(start, dayStart)
                        val intervalEnd = min(event.timestamp, queryEnd)
                        if (intervalEnd > intervalStart) {
                            total += intervalEnd - intervalStart
                        }
                        interactiveStart = null
                    }
                }
            }
        }
        // If still interactive at end of day, count until queryEnd
        interactiveStart?.let { start ->
            val intervalStart = max(start, dayStart)
            if (queryEnd > intervalStart) {
                total += queryEnd - intervalStart
            }
        }
        return total
    }
}
