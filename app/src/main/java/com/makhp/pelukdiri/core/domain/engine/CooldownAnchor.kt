package com.makhp.pelukdiri.core.domain.engine

/** Keeps the engine-selected interval intact while anchoring it to UI completion. */
object CooldownAnchor {
    fun afterCompletion(
        sessionCreatedAtMs: Long,
        originallyEligibleAtMs: Long,
        completedAtMs: Long,
    ): Long? {
        val intervalMs = originallyEligibleAtMs - sessionCreatedAtMs
        return if (intervalMs > 0L) completedAtMs + intervalMs else null
    }
}
