package com.makhp.pelukdiri.core.domain.model

sealed interface AdaptiveLimitResult {
    data class Personalized(val limitMinutes: Int) : AdaptiveLimitResult
    data object InsufficientHistory : AdaptiveLimitResult
}
