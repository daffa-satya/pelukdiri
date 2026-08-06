# Project Guidelines & AI Agent Rules

This document outlines the architectural standards, code style, and technical constraints that AI Agents **must strictly adhere to** when regenerating, restructuring, or adding new code to the "PELUKDIRI" project.

---

## 1. Tech Stack & Environment

- **Language:** Kotlin 1.9+
- **UI Framework:** Jetpack Compose (Banned: XML Layouts, DataBinding, ViewBinding)
- **Architecture:** MVVM + Clean Architecture (Data -> Domain -> UI)
- **Dependency Injection:** Hilt / Dagger-Hilt
- **Asynchronous / Reactive:** Kotlin Coroutines & `StateFlow` / `SharedFlow`
- **Navigation:** Jetpack Navigation Compose (Type-safe Navigation)
- **Database & Local Data:** Room Database + DataStore
- **Background Processing:** WorkManager (HiltWorker integration)
- **External Data:** CSV Export Engine for Research Analytics

---

## 2. Architecture & Layering Rules

1. **Core Layer (`core/`)**
   - Contains shared infrastructure such as Database, central Repositories, and global Domain Models.
   - **Data (`core/data/`):** Repository implementations and Data Sources.
   - **Domain (`core/domain/`):** Pure Kotlin models, UseCases, and Repository Interfaces.
   - **Database (`core/database/`):** Unified Room Database (`PelukDiriDatabase`), Entities, DAOs, and Export engines.

2. **Feature Layer (`features/`)**
   - Contains feature-specific UI (Dashboard, Intervention, Settings).
   - Each feature includes `Screen.kt`, `ViewModel.kt`, and `UiState.kt`.
   - ViewModels **must not hold Android Context** or Composable references.

3. **Collector & Worker (`collector/` & `worker/`)**
   - **Collector:** Services/Singletons for sensor data collection (Light, UsageStats) using Android APIs.
   - **Worker:** `CoroutineWorker` implementations for background data synchronization.

---

## 3. Jetpack Compose Rules

- **State Management:**
  - Use `StateFlow` and `collectAsStateWithLifecycle()` for UI data consumption.
  - Apply **State Hoisting**: Keep UI Composables *stateless* and delegate state management to container/screen components.
  - Event callbacks must use lambdas (e.g., `onExportClick: () -> Unit`).

- **Performance & Stability:**
  - Always use `@Immutable` or `@Stable` annotations on `UiState` data classes.
  - Use `remember` and `derivedStateOf` for expensive calculations to avoid unnecessary recompositions.
  - Do not perform I/O operations or network calls directly inside `@Composable` functions.

- **Previews & Styling:**
  - Must provide `@Preview` (Light & Dark Theme) for reusable UI components.
  - Must use `MaterialTheme` (colorScheme, typography). Hardcoded colors inside Composables are strictly banned.

---

## 4. Kotlin Code Style & Conventions

- **Naming Conventions:**
  - `@Composable` functions must be named using Nouns in PascalCase (e.g., `UsageChart`).
  - Event callbacks must be prefixed with `on` (e.g., `onRefresh`).
  - ViewModels must be suffixed with `ViewModel`, States with `UiState`.

- **Coroutines & Error Handling:**
  - Use `viewModelScope` in ViewModels and `Dispatchers.IO` for database/file operations.
  - Represent UI states using a Sealed Interface:
    ```kotlin
    sealed interface UiState<out T> {
        data object Loading : UiState<Nothing>
        data class Success<T>(val data: T) : UiState<T>
        data class Error(val message: String) : UiState<Nothing>
    }
    ```

- **Dependency Injection (Hilt):**
  - Use Constructor Injection (`@Inject constructor(...)`).
  - For WorkManager, use `@HiltWorker` and `@AssistedInject`.

---

## 5. File & Folder Structure

```text
app/src/main/java/com/makhp/pelukdiri/
├── core/                # Shared logic & infrastructure
│   ├── data/            # Repository Impls, Mappers
│   ├── domain/          # Models, Repository Interfaces, UseCases
│   └── database/        # Room Database
│       ├── dao/         # Data Access Objects
│       ├── entity/      # Room Entities
│       └── export/      # CSV/ZIP Export Engines
├── features/            # Feature Screens (MVVM)
│   └── dashboard/       # DashboardScreen.kt, DashboardViewModel.kt
├── collector/           # Sensor & System data collectors
├── worker/              # Background Work (WorkManager + Foreground Services)
├── di/                  # Hilt Modules
└── ui/                  # Global Theme & Components
```

## 6. AI Agent Constraints & Instructions

### 🛑 STRICTLY BANNED
- **NO XML Layouts:** Do not use XML layouts, `ViewBinding`, `DataBinding`, or `findViewById`. Use 100% Jetpack Compose.
- **NO Java Code:** All code must be written in pure Kotlin.
- **NO Business Logic in UI:** Never execute database operations, state mutations, or business rules inside `@Composable` functions.
- **NO Hardcoded Styling:** Never hardcode colors (`Color(0xFF...)`), dimensions, or text sizes. Always use tokens from `MaterialTheme`.
- **NO Context Leaks:** ViewModels must never hold references to Android `Context`, `Activity`, or `@Composable` elements.
- **NO Unhandled Exceptions:** Always wrap I/O, Database, and Sensor interactions with `runCatching` or `try-catch`.

---

### ⚠️ MANDATORY WORKFLOWS

#### Workflow A: Adding New Data / Entity Pipeline
1. **Entity Definition:** Define the Room `@Entity` in `core/database/entity/`.
2. **DAO Contract:** Create or update the `@Dao` interface in `core/database/dao/`. Ensure "read-all" methods are available for the export engine.
3. **Database Registration:** Register the new entity in `PelukDiriDatabase` (unified source). Current tables: `app_usage`, `daily_summary`, `usage_sensor_logs`, `interventions`, `intervention_logs`, `daily_adaptive_limits`.
4. **Domain Model & Mapper:** Create pure Kotlin models in `core/domain/model/` and extension mappers in `core/data/mapper/`.
5. **Repository Layer:** Declare contract in `core/domain/repository/` and implement in `core/data/repository/`.

#### Workflow B: Implementing New UI Feature
1. **Package Setup:** Create a dedicated feature directory in `features/<feature_name>/`.
2. **UI State Definition:** Declare a sealed interface `UiState` (`Loading`, `Success`, `Error`) in `<Feature>UiState.kt`.
3. **ViewModel Integration:** Create an `@HiltViewModel` using `StateFlow` and expose unmodifiable state (`asStateFlow()`).
4. **Stateless UI:** Implement screen composables using **State Hoisting** (separate stateful screen container from stateless UI components).
5. **Preview Verification:** Always supply `@Preview` annotations for both Light and Dark themes.

#### Workflow C: Data Collection & Background Workers
1. **Sensor Collectors:** Ensure sensor listeners (`Light`, `UsageStats`) in `collector/` properly register and unregister to prevent memory leaks.
2. **WorkManager Setup:** Implement background synchronization using `@HiltWorker` and `@AssistedInject` inside `worker/`.
3. **Reliability:** Critical workers (like `UsageSyncWorker`) must use **Foreground Service** (`setForeground`) with a dedicated notification channel to prevent system termination.
4. **Execution Constraints:** Schedule routine tasks via `PeriodicWorkRequest` with explicit constraints (e.g., Battery Not Low, Storage Not Low).

#### Workflow D: Research & CSV Export Pipeline
1. **Data Retrieval:** Aggregate database records via `core/domain/usecase/` or DAOs directly for full exports.
2. **Asynchronous Processing:** Execute CSV formatting and ZIP compression strictly on `Dispatchers.IO`.
3. **Escape Strings:** All String fields in CSV must be escaped with double quotes (`"..."`) for compatibility with R/Python.
4. **Secure Sharing:** Use `androidx.core.content.FileProvider` to share export ZIP files from the app's internal `exports` directory.

---

## 7. System & Power Management Rules

1. **Battery Optimization:** For longitudinal data collection, the app must check and request "Battery Optimization Exemption" (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) from the user.
2. **Permissions:** Always check for `PACKAGE_USAGE_STATS` and `POST_NOTIFICATIONS` (for Android 13+) before initiating sync tasks.
3. **Foreground Services:** Use the `dataSync` foreground service type for background synchronization to ensure compliance with Android 14+ requirements.
