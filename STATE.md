# PELUKDIRI Project State - Aug 6, 2026

## 1. Project Overview
PELUKDIRI is an Android application designed for research and intervention regarding excessive smartphone usage. It utilizes background monitoring, sensor data (ambient light), and adaptive cognitive challenges to promote mindful digital behavior.

## 2. Completed Milestones

### Core Infrastructure
- **Unified Database**: Consolidated all data into a single `PelukDiriDatabase` (Room) with 6 primary tables:
  - `app_usage`: Per-app daily foreground duration.
  - `daily_summary`: High-level daily metrics (total time, unlock count).
  - `usage_sensor_logs`: Raw snapshots of app usage combined with ambient light levels.
  - `interventions`: UI state for user-facing nudges.
  - `intervention_logs`: Research-grade analytical logs of cognitive task performance.
  - `daily_adaptive_limits`: Dynamically calculated limits based on risk assessment.
- **Hilt Dependency Injection**: Fully migrated to Dagger-Hilt for automated constructor injection across ViewModels, Repositories, Workers, and Services.
- **DataStore Integration**: Implemented `UserPreferencesRepository` for persistent application flags (e.g., backfill status, last sync timestamp).

### Data Collection & Sync
- **AppUsageCollector**: Engine for querying Android's `UsageStatsManager` and the hardware `Light Sensor`.
- **Background Synchronization**:
  - Implemented `UsageSyncWorker` (WorkManager) running every 15 minutes.
  - Promoted to **Foreground Service** with persistent notifications for high reliability.
  - Optimized with "Battery Optimization Whitelisting" detection.
- **On-Demand Backfill**: Converted historical usage scanning to a strictly manual, on-demand action via UI button with DataStore guard check to prevent startup UI thread starvation.

### Intervention System
- **Accessibility Service**: Implemented `AppBlockerAccessibilityService` for real-time app launch detection (`TYPE_WINDOW_STATE_CHANGED`).
- **Accessibility Status Banner**: Added a dynamic warning card on `MainStatsScreen` that detects if the service is disabled and provides a direct shortcut to system settings.
- **Defensive Error Handling & Fallbacks**:
  - Wrapped event processing in defensive `try-catch` blocks to resolve OS "malfunctioning" status caused by uncaught DI/Initialization exceptions.
  - Added real-time usage fallback mechanism when Room historical database is empty (fresh installs).
  - Implemented 3-second debouncing/cooldown guard to prevent rapid activity launch loops.
- **Emergency Bypass Feature**: Implemented a 3-minute emergency bypass option on the intervention overlay. Confirmed bypasses are logged with `isBypassed = true` for research analytics.
- **Risk Engine**: Integrated `CalculateRiskScoreUseCase` evaluating Daily Screen Time ($H$), Launch Frequency ($F$), and Ambient Light ($L$).
- **Cognitive Engine Calibration**: Calibrated Level 4 math generator boundaries ($A \in [14, 35], B \in [13, 28]$) and added asymmetric mixed operations to ensure proper cognitive friction.
- **Transparent Overlay**: Developed `InterventionActivity` as a `singleInstance` task with `taskAffinity=""` and transparent theme (`Theme.PELUKDIRI.Transparent`), allowing it to float seamlessly over target apps (Instagram, TikTok, YouTube).
- **Dynamic Target Apps**: Implemented a user-configurable target app selector in the Dashboard UI, persisting choices via DataStore.

### Research Tooling
- **Database Migration**: Successfully migrated `intervention_logs` to include `isBypassed` flag (Version 3).
- **CSV Export Engine**: `CsvExporter` generates a secure, timestamped ZIP package of the entire database.
- **File Sharing**: Integrated `FileProvider` to allow the research ZIP to be exported via the system share sheet.

## 3. Current State
- **Build Status**: Stable (`./gradlew assembleDebug` success).
- **Architecture**: MVVM + Clean Architecture (Data -> Domain -> UI).
- **Primary Screens**:
  - Dashboard: Stats display, manual sync trigger, on-demand backfill button, and DB export.
  - Intervention: Cognitive challenge overlay hosting adaptive math tasks.

## 4. Pending / TODO
- [ ] **Extended Cognitive Friction**: Extend the cognitive engine up to Level 7 (algebraic linear equations / modulo friction).
- [ ] **Time-Based Active Monitoring**: Transition from `TYPE_WINDOW_STATE_CHANGED` only to periodic active foreground app duration tracking inside `AccessibilityService`.
- [ ] **Data Retention Worker**: Implement an automated cleanup worker to purge raw sensor logs older than 30 days.
- [ ] **Enhanced Analytics**: Add visual longitudinal graphs (Compose Charts) for daily risk score trends and usage habits.
