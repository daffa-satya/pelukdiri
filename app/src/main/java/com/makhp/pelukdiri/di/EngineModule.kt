package com.makhp.pelukdiri.di

import com.makhp.pelukdiri.core.domain.model.ControlConfig
import com.makhp.pelukdiri.core.domain.model.DeviationConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object EngineModule {

    @Provides
    @Singleton
    fun provideControlConfig(): ControlConfig = ControlConfig()

    @Provides
    @Singleton
    fun provideDeviationConfig(): DeviationConfig = DeviationConfig()
}
