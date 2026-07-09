package com.makhp.pelukdiri.di

import com.makhp.pelukdiri.core.data.repository.InterventionRepositoryImpl
import com.makhp.pelukdiri.core.data.repository.UsageRepositoryImpl
import com.makhp.pelukdiri.core.domain.repository.InterventionRepository
import com.makhp.pelukdiri.core.domain.repository.UsageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUsageRepository(
        usageRepositoryImpl: UsageRepositoryImpl
    ): UsageRepository

    @Binds
    @Singleton
    abstract fun bindInterventionRepository(
        interventionRepositoryImpl: InterventionRepositoryImpl
    ): InterventionRepository
}
