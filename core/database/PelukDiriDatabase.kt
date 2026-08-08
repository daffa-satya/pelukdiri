package com.makhp.pelukdiri.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.makhp.pelukdiri.core.database.dao.AdaptiveLimitDao
import com.makhp.pelukdiri.core.database.dao.InterventionDao
import com.makhp.pelukdiri.core.database.dao.InterventionNotificationDao
import com.makhp.pelukdiri.core.database.dao.UsageDao
import com.makhp.pelukdiri.core.database.dao.UsageSensorDao
import com.makhp.pelukdiri.core.database.entity.AppUsageEntity
import com.makhp.pelukdiri.core.database.entity.DailyAdaptiveLimitEntity
import com.makhp.pelukdiri.core.database.entity.DailySummaryEntity
import com.makhp.pelukdiri.core.database.entity.InterventionLogEntity
import com.makhp.pelukdiri.core.database.entity.InterventionNotificationEntity
import com.makhp.pelukdiri.core.database.entity.UsageSensorLogEntity

@Database(
    entities = [
        UsageSensorLogEntity::class,
        InterventionLogEntity::class,
        DailyAdaptiveLimitEntity::class,
        AppUsageEntity::class,
        DailySummaryEntity::class,
        InterventionNotificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PelukDiriDatabase : RoomDatabase() {
    abstract fun usageSensorDao(): UsageSensorDao
    abstract fun interventionDao(): InterventionDao
    abstract fun interventionNotificationDao(): InterventionNotificationDao
    abstract fun adaptiveLimitDao(): AdaptiveLimitDao
    abstract fun usageDao(): UsageDao
}
