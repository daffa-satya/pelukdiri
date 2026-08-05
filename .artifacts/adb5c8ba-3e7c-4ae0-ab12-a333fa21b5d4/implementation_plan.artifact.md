# Fix Dagger/Hilt Missing Binding for UsageDao

The goal is to resolve the build error `[Dagger/MissingBinding] com.makhp.pelukdiri.core.data.database.UsageDao cannot be provided without an @Provides-annotated method`. This occurred because `UsageDao` was defined in `AppDatabase`, but Hilt was only providing `PelukDiriDatabase`. We will consolidate the database structure according to the project guidelines in `AGENTS.md`.

## User Review Required

> [!IMPORTANT]
> I will be merging `AppDatabase` into `PelukDiriDatabase`. This involves moving entities and DAOs from `com.makhp.pelukdiri.core.data.database` to `com.makhp.pelukdiri.core.database` (and its sub-packages `entity` and `dao`).
> `InterventionDao` and `InterventionEntity` in `core.data.database` are also redundant or conflicting with those in `core.database`. I will consolidate them to avoid future Hilt errors.

## Proposed Changes

### Core Database Consolidation

#### [MODIFY] [PelukDiriDatabase.kt](file:///home/daffa-satya/StudioProjects/PELUKDIRI/app/src/main/java/com/makhp/pelukdiri/core/database/PelukDiriDatabase.kt)
- Add `AppUsageEntity` and `DailySummaryEntity` to the `@Database` entities list.
- Add `abstract fun usageDao(): UsageDao`.
- Consider if `InterventionEntity` (from `core.data.database`) should be added or merged with `InterventionLogEntity`. For now, I'll focus on fixing the `UsageDao` error, but I might need to add `InterventionEntity` if it's used elsewhere.

#### [MOVE] [UsageDao.kt](file:///home/daffa-satya/StudioProjects/PELUKDIRI/app/src/main/java/com/makhp/pelukdiri/core/data/database/UsageDao.kt) -> `core/database/dao/UsageDao.kt`
- Update package name to `com.makhp.pelukdiri.core.database.dao`.
- Update imports for entities.

#### [MOVE] [AppUsageEntity.kt](file:///home/daffa-satya/StudioProjects/PELUKDIRI/app/src/main/java/com/makhp/pelukdiri/core/data/database/AppUsageEntity.kt) -> `core/database/entity/AppUsageEntity.kt`
- Update package name to `com.makhp.pelukdiri.core.database.entity`.

#### [MOVE] [DailySummaryEntity.kt](file:///home/daffa-satya/StudioProjects/PELUKDIRI/app/src/main/java/com/makhp/pelukdiri/core/data/database/DailySummaryEntity.kt) -> `core/database/entity/DailySummaryEntity.kt`
- Update package name to `com.makhp.pelukdiri.core.database.entity`.

#### [DELETE] [AppDatabase.kt](file:///home/daffa-satya/StudioProjects/PELUKDIRI/app/src/main/java/com/makhp/pelukdiri/core/data/database/AppDatabase.kt)
- This file is now redundant as its functionality is merged into `PelukDiriDatabase`.

### Hilt Dependency Injection

#### [MODIFY] [DatabaseModule.kt](file:///home/daffa-satya/StudioProjects/PELUKDIRI/app/src/main/java/com/makhp/pelukdiri/di/DatabaseModule.kt)
- Add a `@Provides` method for `UsageDao`.

### Repository & Worker Updates

#### [MODIFY] [UsageRepositoryImpl.kt](file:///home/daffa-satya/StudioProjects/PELUKDIRI/app/src/main/java/com/makhp/pelukdiri/core/data/repository/UsageRepositoryImpl.kt)
- Update imports for the moved `UsageDao` and entities.

#### [MODIFY] [UsageSyncWorker.kt](file:///home/daffa-satya/StudioProjects/PELUKDIRI/app/src/main/java/com/makhp/pelukdiri/worker/UsageSyncWorker.kt)
- Ensure it uses the correct `UsageRepository`.

## Verification Plan

### Automated Tests
- Run `./gradlew :app:hiltJavaCompileDebug` to verify that Hilt can now generate the components without the `MissingBinding` error.
- Run `./gradlew assembleDebug` to ensure the whole app builds.

### Manual Verification
- Deploy the app to a device/emulator to ensure Room database is initialized correctly.
