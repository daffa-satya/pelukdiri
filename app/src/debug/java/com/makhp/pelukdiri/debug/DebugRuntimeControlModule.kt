package com.makhp.pelukdiri.debug

import com.makhp.pelukdiri.core.domain.InterventionLaunchPolicy
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DebugRuntimeControlModule {
    @Provides @Singleton fun timeProvider(controls: DebugRuntimeControls): TimeProvider = controls
    @Provides @Singleton fun launchPolicy(controls: DebugRuntimeControls): InterventionLaunchPolicy = controls
}
