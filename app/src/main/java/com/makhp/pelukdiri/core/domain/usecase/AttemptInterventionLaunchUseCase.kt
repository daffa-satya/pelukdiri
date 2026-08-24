package com.makhp.pelukdiri.core.domain.usecase

import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.core.domain.model.ControlResult
import com.makhp.pelukdiri.core.domain.repository.UserPreferencesRepository
import javax.inject.Inject

enum class InterventionLaunchResult {
    LAUNCHED,
    LOCKED,
    FAILED,
}

class AttemptInterventionLaunchUseCase @Inject constructor(
    private val lockManager: InterventionLockManager,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(
        controlResult: ControlResult,
        launch: () -> Boolean,
    ): InterventionLaunchResult {
        if (!lockManager.acquireLock()) return InterventionLaunchResult.LOCKED

        val launched = try {
            launch()
        } catch (error: Throwable) {
            lockManager.releaseLock()
            throw error
        }
        if (!launched) {
            lockManager.releaseLock()
            return InterventionLaunchResult.FAILED
        }

        userPreferencesRepository.setNextEligibleInterventionAt(
            controlResult.nextEligibleInterventionAt
        )
        userPreferencesRepository.setCurrentDifficulty(controlResult.nextDifficulty)
        return InterventionLaunchResult.LAUNCHED
    }
}
