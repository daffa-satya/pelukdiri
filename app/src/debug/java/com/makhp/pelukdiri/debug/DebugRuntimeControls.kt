package com.makhp.pelukdiri.debug

import android.content.Context
import android.content.SharedPreferences
import com.makhp.pelukdiri.core.domain.InterventionLaunchPolicy
import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugRuntimeControls @Inject constructor(
    @ApplicationContext context: Context
) : TimeProvider, InterventionLaunchPolicy {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val virtualNow = AtomicLong(preferences.getLong(KEY_VIRTUAL_NOW, USE_SYSTEM_TIME))
    private val failNextLaunch = AtomicBoolean(false)

    override fun nowMillis(): Long = virtualNow.get().takeUnless { it == USE_SYSTEM_TIME } ?: System.currentTimeMillis()
    override fun zoneId(): ZoneId = ZoneId.systemDefault()
    override fun consumeForcedFailure(): Boolean = failNextLaunch.compareAndSet(true, false)
    @Synchronized override fun consumeForcedChallenge(): InterventionChallengeType? {
        val challenge = pendingForcedChallenge() ?: return null
        preferences.edit().remove(KEY_FORCED_CHALLENGE).commit()
        return challenge
    }

    fun useSystemTime() = persistTime(USE_SYSTEM_TIME)
    fun setTime(epochMillis: Long) = persistTime(epochMillis)
    fun advanceBy(millis: Long) {
        val advanced = virtualNow.updateAndGet { current ->
            (current.takeUnless { it == USE_SYSTEM_TIME } ?: System.currentTimeMillis()) + millis
        }
        preferences.edit().putLong(KEY_VIRTUAL_NOW, advanced).commit()
    }
    fun forceNextLaunchFailure() = failNextLaunch.set(true)
    fun armForcedChallenge(challengeType: InterventionChallengeType) {
        preferences.edit().putString(KEY_FORCED_CHALLENGE, challengeType.name).commit()
    }
    fun pendingForcedChallenge(): InterventionChallengeType? =
        preferences.getString(KEY_FORCED_CHALLENGE, null)
            ?.let { runCatching { InterventionChallengeType.valueOf(it) }.getOrNull() }

    private fun persistTime(epochMillis: Long) {
        virtualNow.set(epochMillis)
        // Boundary tests may reboot immediately, so wait for this tiny debug-only write.
        preferences.edit().putLong(KEY_VIRTUAL_NOW, epochMillis).commit()
    }

    companion object {
        private const val PREFERENCES_NAME = "pelukdiri_debug_runtime"
        private const val KEY_VIRTUAL_NOW = "virtual_now"
        private const val KEY_FORCED_CHALLENGE = "forced_challenge"
        private const val USE_SYSTEM_TIME = Long.MIN_VALUE
    }
}
