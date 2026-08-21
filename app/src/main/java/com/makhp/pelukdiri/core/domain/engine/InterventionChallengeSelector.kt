package com.makhp.pelukdiri.core.domain.engine

import kotlin.random.Random
import javax.inject.Inject

enum class InterventionChallengeType {
    AUTO,
    MATH,
    PATTERN,
}

class InterventionChallengeSelector @Inject constructor() {
    private var coinFlip: () -> Boolean = { Random.Default.nextBoolean() }

    internal constructor(coinFlip: () -> Boolean) : this() {
        this.coinFlip = coinFlip
    }

    fun select(): InterventionChallengeType =
        if (coinFlip()) InterventionChallengeType.MATH else InterventionChallengeType.PATTERN
}
