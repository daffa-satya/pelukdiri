package com.makhp.pelukdiri.core.domain

import com.makhp.pelukdiri.core.domain.engine.InterventionChallengeType

interface InterventionLaunchPolicy {
    fun consumeForcedFailure(): Boolean
    fun consumeForcedChallenge(): InterventionChallengeType? = null
}
