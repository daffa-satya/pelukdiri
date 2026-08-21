package com.makhp.pelukdiri.core.domain.engine

import javax.inject.Inject

enum class InterventionChallengeType {
    AUTO,
    MATH,
    PATTERN,
}

class InterventionChallengeSelector @Inject constructor() {
    fun select(previous: InterventionChallengeType? = null): InterventionChallengeType =
        if (previous == InterventionChallengeType.MATH) {
            InterventionChallengeType.PATTERN
        } else {
            InterventionChallengeType.MATH
        }
}
