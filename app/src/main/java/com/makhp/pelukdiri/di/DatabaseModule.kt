package com.makhp.pelukdiri.di

import android.content.Context
import androidx.room.Room
import com.makhp.pelukdiri.core.database.PelukDiriDatabase
import com.makhp.pelukdiri.core.database.dao.AdaptiveLimitDao
import com.makhp.pelukdiri.core.database.dao.InterventionDao
import com.makhp.pelukdiri.core.database.dao.InterventionNotificationDao
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PelukDiriDatabase {
        return Room.databaseBuilder(
            context,
            PelukDiriDatabase::class.java,
            "pelukdiri_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideUsageSensorDao(db: PelukDiriDatabase): UsageSensorDao = db.usageSensorDao()

    @Provides
    @Singleton
    fun provideInterventionDao(db: PelukDiriDatabase): InterventionDao = db.interventionDao()

    @Provides
    @Singleton
    fun provideInterventionNotificationDao(db: PelukDiriDatabase): InterventionNotificationDao = db.interventionNotificationDao()

    @Provides
    @Singleton
    fun provideAdaptiveLimitDao(db: PelukDiriDatabase): AdaptiveLimitDao = db.adaptiveLimitDao()

    @Provides
    @Singleton
    fun provideUsageDao(db: PelukDiriDatabase): UsageDao = db.usageDao()
}
