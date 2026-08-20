package com.makhp.pelukdiri.di

import com.makhp.pelukdiri.core.domain.InterventionLaunchPolicy
import com.makhp.pelukdiri.core.domain.time.SystemTimeProvider
import com.makhp.pelukdiri.core.domain.time.TimeProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RuntimeControlModule {
    @Provides @Singleton fun timeProvider(): TimeProvider = SystemTimeProvider()
    @Provides @Singleton fun launchPolicy(): InterventionLaunchPolicy = object : InterventionLaunchPolicy {
        override fun consumeForcedFailure(): Boolean = false
    }
}
