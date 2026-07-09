package com.makhp.pelukdiri.di

import android.content.Context
import androidx.room.Room
import com.makhp.pelukdiri.core.data.database.AppDatabase
import com.makhp.pelukdiri.core.data.database.InterventionDao
import com.makhp.pelukdiri.core.data.database.UsageDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideUsageDao(db: AppDatabase): UsageDao {
        return db.usageDao()
    }

    @Provides
    fun provideInterventionDao(db: AppDatabase): InterventionDao {
        return db.interventionDao()
    }
}
