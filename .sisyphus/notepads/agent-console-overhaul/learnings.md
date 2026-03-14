## Task 4 - CI/CD + Community Files (2026-03-13)

### Patterns / Conventions
- GitHub Actions: use `gradle/actions/setup-gradle@v3` (not deprecated `gradle/gradle-build-action`)
- `actions/upload-artifact@v4` for lint report upload (use v4, not v3 - v3 deprecated)
- `actions/setup-java@v4` with `distribution: 'temurin'` is the current standard
- Topics API: `PUT /repos/{owner}/{repo}/topics` with `names[]` array, returns sorted topics
- Community files in `.github/` directory raise GitHub health score significantly
- Issue templates use YAML front-matter (`name`, `about`, `title`, `labels`) for GitHub UI

### Successful Approaches
- `gh api --method PUT repos/owner/repo/topics --field 'names[]=topic'` works cleanly
- README CI badge format: `[![Name](badge_url)](workflow_url)`
- Combining badge + build-from-source section + clean title in one README edit is efficient

## Task 3b - TermuxRepository injectable class (2026-03-13)

### Patterns / Conventions
- Keep Termux execution logic centralized in one class (`TermuxRepository`) with pure helper validation (`validateWorkingDir`) and side-effect methods (`runAgent`, `openTermux`).
- Preserve current execution flow when migrating logic: validation -> installation check -> execution id allocation -> RUN_COMMAND intent dispatch -> `ExecutionStore` update.
- App-level DI module uses `@Module` + `@InstallIn(SingletonComponent::class)` + `@Provides` to expose singleton repository binding.

### Successful Approaches
- Direct logic migration from `TermuxRunner` into `TermuxRepository` minimized behavior drift and preserved existing error messaging.
- Keeping method signatures context-driven (`context`, `agent`, `prompt`, `workingDir`) maintains compatibility with current call sites for incremental refactor tasks.

## Task 2 - Build Foundation (2026-03-13)

### Patterns / Conventions
- `com.termux.termux-app:termux-shared` is the resolvable coordinate for this project setup; `com.termux:termux-shared` failed resolution.
- Compose BOM `2024.09.00` with Kotlin 2.0.20 required explicit opt-in for `androidx.compose.material3.ExperimentalMaterial3Api` in Gradle compiler args for current UI code.
- XML theme parent `Theme.Material3.DayNight.NoActionBar` requires `com.google.android.material:material` on the app classpath.

### Successful Approaches
- When AGP version checks blocked local `gradle wrapper`, generating wrapper files in a temporary minimal Gradle project and copying `gradlew`, `gradlew.bat`, and `gradle/wrapper/*` produced a valid 8.7 wrapper.
- On this machine, `./gradlew` required explicitly exporting `JAVA_HOME` from `mise which java` to run reproducibly.

## Task 3c - MainViewModel + ExecutionStore deletion (2026-03-13)

### Patterns / Conventions
- For staged architecture refactors, a lightweight event bridge (`ResultBus`) keeps Service/Repository decoupled from UI state holders while migration is in progress.
- Keep stale-result protection logic identical when moving execution state from singleton store to ViewModel (`lastExecutionId` guard before applying results).

### Successful Approaches
- Moving `ExecutionUiState` into `MainViewModel.kt` while keeping `TermuxRepository` and `TermuxResultService` on bus events allows incremental adoption without touching `MainActivity` in this step.
- Replacing direct `ExecutionStore` writes in repository/service with `ResultBus` calls cleanly isolates side effects from state rendering concerns.

## Task 3d - MainActivity ViewModel wiring + TermuxRunner deletion (2026-03-13)

### Patterns / Conventions
- Compose screen migration can use `val viewModel: MainViewModel = viewModel()` once `MainActivity` is annotated with `@AndroidEntryPoint`.
- During staged refactors, UI helper actions (`openTermux`, `validateWorkingDir`) can temporarily call `TermuxRepository` while execution control shifts to ViewModel methods.

### Successful Approaches
- Replacing `ExecutionStore.state.collectAsState()` with `viewModel.uiState.collectAsState()` and swapping run/install calls to `viewModel.run` and `viewModel.checkTermuxInstalled` kept UI behavior intact.
- Removing `TermuxRunner.kt` immediately after updating call sites exposed stale references early and kept migration scope clear.

## Task 4: Robustness features (output truncation, prompt validation, foreground service, battery warning)

### Output truncation
- Added `truncateOutput(raw, label)` static helper in `TermuxRepository.companion`
- Converts to UTF-8 bytes, truncates to MAX_OUTPUT_SIZE (50*1024), appends "[...truncated — N bytes total in <label>]"
- Called in `MainViewModel.publishResult()` before storing in UiState

### Prompt validation
- Added null-byte check (`\u0000`) and 10KB max size in both `TermuxRepository.runAgent()` AND `MainViewModel.run()` (belt-and-suspenders)
- UI `promptError` in `AgentConsoleApp` also now validates null bytes and size, showing error inline

### Foreground service
- `TermuxRepository.runAgent()` uses `PendingIntent.getForegroundService()` for API 26+ (Build.VERSION_CODES.O), falls back to `getService()` for older
- `TermuxResultService` now calls `startForeground()` in `onCreate()` with a minimal low-priority notification
- Notification channel `agent_result_channel` created on O+ with IMPORTANCE_LOW

### Battery optimization warning
- In `AgentConsoleApp`, `batteryOptimized` is computed via `PowerManager.isIgnoringBatteryOptimizations(packageName)`; true means NOT exempt (i.e., still optimized)
- Passed to `StatusCard` and shown as error-colored warning text when `batteryOptimized == true`

### Manifest changes
- Added `FOREGROUND_SERVICE` permission (normal permission, automatically granted)
- Added `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission
- `TermuxResultService` now has `android:foregroundServiceType="dataSync"`

### Pitfalls
- Edit tool gotcha: replacing a blank line `\n` with actual content required a single-line replace directly on the blank line's tag
- `batteryOptimized` was briefly missing its closing `}` for the `remember { }` lambda — fixed by a targeted single-line replace
