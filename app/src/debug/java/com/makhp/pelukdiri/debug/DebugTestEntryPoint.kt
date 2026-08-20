package com.makhp.pelukdiri.debug

import com.makhp.pelukdiri.core.domain.InterventionLockManager
import com.makhp.pelukdiri.features.intervention.ActiveInterventionSession
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DebugTestEntryPoint {
    fun session(): ActiveInterventionSession
    fun lock(): InterventionLockManager
    fun controls(): DebugRuntimeControls
}
