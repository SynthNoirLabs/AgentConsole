# AgentConsole — Full Project Overhaul

## TL;DR

> **Quick Summary**: Complete overhaul of the AgentConsole Android app — from an unbuildable 410-line prototype with global singletons to a properly architected, buildable, tested, CI-backed app with execution history, dark mode, and directory picker.
>
> **Deliverables**:
> - Buildable project (`./gradlew assembleDebug` works from fresh clone)
> - Clean architecture (ViewModel + Hilt DI + Repository pattern)
> - TDD test suite (JUnit + coroutines-test)
> - GitHub Actions CI pipeline
> - Robustness: output truncation, prompt validation, foreground service PendingIntent
> - Features: dark mode, execution history (Room), SAF directory picker, two-screen navigation
> - Full GitHub community profile (LICENSE, templates, CONTRIBUTING, topics)
>
> **Estimated Effort**: Large
> **Parallel Execution**: YES — 6 waves, max 2 concurrent tasks
> **Critical Path**: Task 1 → Task 2 → Task 3 → Task 5/6 → Task 7 → Task 8

---

## Context

### Original Request
User requested a comprehensive audit of the AgentConsole project (local + remote, branches, PRs, issues) followed by a plan to tackle ALL identified improvements. The audit revealed critical buildability gaps, architecture anti-patterns, missing CI/CD, and unmerged valuable work on a review branch.

### Interview Summary
**Key Discussions**:
- Full audit revealed 6 categories of issues: git hygiene, buildability, architecture, security, CI/CD, and missing features
- Review branch (`claude/review-repo-Z3MM8`) has +275 lines of security hardening, validation, logging, persistence, and unit tests
- Default branch is wrongly set to `claude/create-github-repo-AzKXB` instead of `main`
- Project cannot build from CLI — missing gradlew, res/, proguard-rules.pro, root build.gradle.kts

**Research Findings**:
- Explore agent: Complete architecture audit — no ViewModel, no DI, global singletons, business logic in UI, executionId not validated on main, missing critical project files
- Librarian: Termux RUN_COMMAND best practices — use `getForegroundService()`, `EXTRA_RESULT_DIRECTORY` for >100KB output, 100KB truncation causes silent `TransactionTooLargeException`, termux-shared 0.118.3 available

**Decisions**:
- Test strategy: TDD (Red-Green-Refactor)
- Review branch: Merge first into main, build everything on top
- Merge strategy: Regular merge (preserves attribution)
- Package name: Keep `com.example.agentconsole` (changing is painful, no Play Store plans)
- Streaming output: CUT from scope (too complex, separate future effort)

### Metis Review
**Identified Gaps** (addressed):
- Review branch enables minification without proguard-rules.pro → create proguard file during merge
- AGP/Kotlin/Compose version matrix unspecified → pin exact versions in version catalog
- TDD impossible until build works → Phase 1 (buildability) is hard prerequisite for TDD
- No acceptance criteria for verifying builds → every phase gates on `./gradlew assembleDebug` + `testDebugUnitTest`
- Concurrent execution edge case → document as known limitation, reject second run while first is active
- Process death during execution → partially addressed by Room history, rest is future work
- `isTermuxInstalled` stale on resume → fix during architecture refactor
- Output >100KB silently lost → truncate in UI layer with length indicator

---

## Work Objectives

### Core Objective
Transform AgentConsole from an unbuildable prototype into a properly architected, testable, CI-backed Android application with execution history, dark mode, and polished UX.

### Concrete Deliverables
- Buildable project with Gradle wrapper (`./gradlew assembleDebug` from fresh clone)
- `MainViewModel` + `TermuxRepository` + Hilt DI (replacing global singletons)
- Room database for execution history + history screen
- SAF directory picker replacing raw text input
- Dark mode with Material3 `DayNight`
- Two-screen navigation (main + history)
- GitHub Actions CI (build + lint + test)
- Full community profile (LICENSE, CONTRIBUTING, issue templates, topics)
- Robustness: output truncation, prompt validation, foreground service PendingIntent, battery warning

### Definition of Done
- [ ] `./gradlew clean assembleDebug assembleRelease testDebugUnitTest lint` — all pass
- [ ] `gh api repos/SynthNoirLabs/AgentConsole --jq '.default_branch'` → `main`
- [ ] No global `object` singletons for state (only `Agent` enum)
- [ ] All architecture components use Hilt injection
- [ ] CI workflow exists and would pass on push

### Must Have
- Gradle wrapper + root build.gradle.kts (project must build from CLI)
- ViewModel + Hilt DI (no global mutable singletons)
- Unit tests (TDD: written before implementation)
- GitHub Actions CI pipeline
- MIT LICENSE file
- proguard-rules.pro with Compose + Termux keep rules
- Output truncation handling
- termux-shared upgrade to 0.118.3

### Must NOT Have (Guardrails)
- **No multi-module project structure** (no `:core`, `:data`, `:domain` modules)
- **No UseCase/Interactor layers** — ViewModel calls Repository directly
- **No abstract interfaces for single implementations** (no `ITermuxRepository`)
- **No instrumented tests** (require emulator, block CI)
- **No Compose UI tests** (heavy setup, low value at this stage)
- **Robolectric allowed ONLY for Room DAO tests** (Room requires Android context; Robolectric provides this in JVM test scope without emulator. Exception is narrowly scoped to `ExecutionHistoryDaoTest.kt` only)
- **No streaming output feature** (CUT — too complex, separate future effort)
- **No package name change** (keep `com.example.agentconsole`)
- **No more than 1 Hilt `@Module`** for bindings
- **No over-abstraction** — this is a 5-file app growing to ~12 files, not an enterprise project
- **No `as any`/`@Suppress` hacks** to make things compile

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: YES (on review branch: JUnit 4.13.2 + kotlinx-coroutines-test 1.8.1)
- **Automated tests**: TDD (Red-Green-Refactor)
- **Framework**: JUnit 4.13.2 + kotlinx-coroutines-test 1.8.1 + Room testing + Robolectric 4.12.2 (Room DAO tests only)
- **TDD applies from Phase 2 onward** (Phase 1 must make project buildable first)

### QA Policy
Every task MUST include agent-executed QA scenarios.
Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **Build verification**: `./gradlew assembleDebug` + `./gradlew testDebugUnitTest` after EVERY task
- **Architecture checks**: `grep` for forbidden patterns (global singletons, missing annotations)
- **CI verification**: Workflow file exists with correct steps
- **Regression gate**: `./gradlew testDebugUnitTest` must pass after every phase — never break existing tests

### Phase Gate Rule (MANDATORY)
Every wave MUST pass `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` before the next wave begins. No exceptions.

---

## Execution Strategy

### Parallel Execution Waves

```
Wave 1 (Start immediately — git hygiene):
└── Task 1: Merge review branch + fix default branch + cleanup [quick]

Wave 2 (After Wave 1 — build foundation):
└── Task 2: Gradle wrapper + root build + resources + LICENSE + version catalog [deep]

Wave 3 (After Wave 2 — architecture + CI, PARALLEL):
├── Task 3: ViewModel + Hilt DI + TermuxRepository [deep]
└── Task 4: CI/CD + community files [quick]

Wave 4 (After Wave 3 — robustness + data, PARALLEL):
├── Task 5: Output truncation + prompt validation + foreground service [unspecified-high]
└── Task 6: Room execution history data layer [deep]

Wave 5 (After Wave 4 — UI features):
└── Task 7: Dark mode + navigation + SAF picker + history screen [visual-engineering]

Wave 6 (After Wave 5 — final verification):
└── Task 8: Full build verification + README update [quick]

Wave FINAL (After ALL tasks — independent review, 4 parallel):
├── Task F1: Plan compliance audit [oracle]
├── Task F2: Code quality review [unspecified-high]
├── Task F3: Real QA [unspecified-high]
└── Task F4: Scope fidelity check [deep]

Critical Path: Task 1 → Task 2 → Task 3 → Task 5/6 → Task 7 → Task 8 → F1-F4
Max Concurrent: 2 (Waves 3 and 4)
```

### Dependency Matrix

| Task | Depends On | Blocks | Wave |
|------|-----------|--------|------|
| 1 | — | 2,3,4,5,6,7,8 | 1 |
| 2 | 1 | 3,4,5,6,7,8 | 2 |
| 3 | 2 | 5,6,7,8 | 3 |
| 4 | 2 | 7 (CI green before features) | 3 |
| 5 | 3 | 7,8 | 4 |
| 6 | 3 | 7,8 | 4 |
| 7 | 5,6 | 8 | 5 |
| 8 | 7 | F1-F4 | 6 |
| F1-F4 | 8 | — | FINAL |

### Agent Dispatch Summary

- **Wave 1**: 1 task — T1 → `quick` + `git-master`
- **Wave 2**: 1 task — T2 → `deep` + `architecture`
- **Wave 3**: 2 tasks — T3 → `deep` + `architecture, refactor`; T4 → `quick` + `doc-writer`
- **Wave 4**: 2 tasks — T5 → `unspecified-high` + `test-writer, security-audit`; T6 → `deep` + `architecture, test-writer`
- **Wave 5**: 1 task — T7 → `visual-engineering` + `frontend-ui-ux`
- **Wave 6**: 1 task — T8 → `quick` + `doc-writer`
- **Wave FINAL**: 4 tasks — F1 → `oracle`; F2 → `unspecified-high` + `code-review`; F3 → `unspecified-high`; F4 → `deep`

---

## TODOs

> Implementation + Test = ONE Task. Never separate.
> EVERY task has: Recommended Agent Profile + Parallelization info + QA Scenarios.
> TDD applies from Task 3 onward (build must work first).

---

### WAVE 1 — Git Hygiene

- [ ] 1. Merge Review Branch, Fix Default Branch, Clean Up Stale Branches

  **What to do**:
  - Checkout `main` locally: `git checkout -b main origin/main`
  - Merge the README update from `claude/create-github-repo-AzKXB`: `git merge origin/claude/create-github-repo-AzKXB` (this is 1 commit: README title tweak)
  - Merge the review branch: `git merge origin/claude/review-repo-Z3MM8` (this adds security hardening, validation, logging, persistence, unit tests)
  - Resolve any conflicts (README was only changed on one branch, code only on the other — should be clean)
  - Create `app/proguard-rules.pro` with minimal keep rules for Compose + Termux (review branch enables `isMinifyEnabled=true` but no proguard file exists — release build would strip everything)
  - Push updated main: `git push origin main`
  - Change GitHub default branch to `main`: `gh api repos/SynthNoirLabs/AgentConsole -X PATCH -f default_branch=main`
  - Delete stale remote branches: `git push origin --delete claude/create-github-repo-AzKXB claude/review-repo-Z3MM8`
  - Verify: `git log main --oneline` shows all 4 commits (initial + README + review + proguard)

  **Must NOT do**:
  - Do not squash merge (preserve commit attribution)
  - Do not rewrite history
  - Do not change package name

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`git-master`]
    - `git-master`: Git operations — merge, branch management, remote operations
  - **Skills Evaluated but Omitted**:
    - `security-audit`: Not needed for git operations

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 1 (solo)
  - **Blocks**: Tasks 2, 3, 4, 5, 6, 7, 8
  - **Blocked By**: None (start immediately)

  **References**:

  **Pattern References**:
  - `origin/claude/review-repo-Z3MM8` — The branch to merge. Contains +275 lines: input validation, execution ID tracking, SharedPreferences, logging, unit tests
  - `origin/claude/create-github-repo-AzKXB` — Contains README title update only (+1/-1 line)

  **API/Type References**:
  - `app/build.gradle.kts:20-26` — Review branch enables `isMinifyEnabled=true` + `isShrinkResources=true`, needs proguard-rules.pro

  **External References**:
  - Compose proguard rules: https://developer.android.com/develop/ui/compose/performance/stability#configuration-file
  - Termux proguard: keep `com.termux.shared.**` classes used via reflection

  **WHY Each Reference Matters**:
  - The review branch modifies `ExecutionStore.publishResult()` signature (adds `executionId` param) — all subsequent architecture work builds on this version
  - proguard-rules.pro is required because review branch enables R8 minification

  **Acceptance Criteria**:
  - [ ] `gh api repos/SynthNoirLabs/AgentConsole --jq '.default_branch'` → `main`
  - [ ] `git log main --oneline` shows commits from both branches
  - [ ] `git branch -r` shows only `origin/main` (stale branches deleted)
  - [ ] `test -f app/proguard-rules.pro && echo exists` → `exists`

  **QA Scenarios:**

  ```
  Scenario: Default branch correctly set to main
    Tool: Bash (gh api)
    Preconditions: GitHub CLI authenticated, repo exists
    Steps:
      1. Run: gh api repos/SynthNoirLabs/AgentConsole --jq '.default_branch'
      2. Assert output is exactly: main
    Expected Result: Output is "main"
    Failure Indicators: Output is "claude/create-github-repo-AzKXB" or any non-main value
    Evidence: .sisyphus/evidence/task-1-default-branch.txt

  Scenario: Review branch changes present in main
    Tool: Bash (git)
    Preconditions: main branch checked out
    Steps:
      1. Run: git log main --oneline | grep 'harden security'
      2. Assert at least 1 match
      3. Run: git log main --oneline | grep 'Update README'
      4. Assert at least 1 match
    Expected Result: Both commits present in main history
    Failure Indicators: Either grep returns empty
    Evidence: .sisyphus/evidence/task-1-merge-verification.txt

  Scenario: Stale branches deleted
    Tool: Bash (git)
    Preconditions: Remote is up to date
    Steps:
      1. Run: git fetch --prune origin
      2. Run: git branch -r
      3. Assert output contains only origin/main and origin/HEAD -> origin/main
    Expected Result: No claude/* branches remain on remote
    Failure Indicators: Any branch matching 'claude/' appears
    Evidence: .sisyphus/evidence/task-1-branches-cleaned.txt

  Scenario: Proguard rules file exists and has content
    Tool: Bash
    Preconditions: main branch
    Steps:
      1. Run: test -f app/proguard-rules.pro && wc -l < app/proguard-rules.pro
      2. Assert line count > 0
    Expected Result: File exists with Compose and Termux keep rules
    Failure Indicators: File missing or empty
    Evidence: .sisyphus/evidence/task-1-proguard.txt
  ```

  **Commit**: YES
  - Message: `chore: merge review branch, fix default branch, add proguard rules`
  - Files: `app/proguard-rules.pro`, git merge operations
  - Pre-commit: n/a (git operations)

---

### WAVE 2 — Build Foundation

- [ ] 2. Make Project Buildable: Gradle Wrapper, Root Build, Resources, Version Catalog, LICENSE

  **What to do**:
  - Generate Gradle wrapper: `gradle wrapper --gradle-version 8.7` (creates `gradlew`, `gradlew.bat`, `gradle/wrapper/`)
  - Create root `build.gradle.kts` pinning exact plugin versions:
    - AGP `8.5.2`
    - Kotlin `2.0.20`
    - Compose compiler plugin `2.0.20`
    - Hilt `2.51.1` (apply false — used in Phase 3)
    - KSP `2.0.20-1.0.25` (apply false — needed for Hilt/Room)
  - Create `gradle/libs.versions.toml` version catalog with ALL dependency versions centralized
  - Migrate `app/build.gradle.kts` to use version catalog references (e.g., `libs.androidx.core.ktx`)
  - Update Compose BOM from `2024.06.00` to `2024.09.00` (compatible with Kotlin 2.0.20)
  - Create `app/src/main/res/values/strings.xml` with app name and basic strings
  - Create `app/src/main/res/values/themes.xml` — minimal Material3 theme (`Theme.AgentConsole` extending `Theme.Material3.DayNight.NoActionBar`)
  - Create `app/src/main/res/values/colors.xml` with Material3 seed color
  - Create launcher icons — use Android adaptive icon XML (`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` + simple foreground vector)
  - Add fallback PNG icons in `mipmap-hdpi`, `mipmap-mdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, `mipmap-xxxhdpi` (can be simple solid-color placeholders)
  - Create `LICENSE` file with MIT license text (as stated in README)
  - Verify build: `./gradlew assembleDebug` must succeed
  - Verify tests: `./gradlew testDebugUnitTest` must succeed (review branch tests should still pass)

  **Must NOT do**:
  - Do not set up Hilt yet (Phase 3)
  - Do not modify Kotlin source files beyond what's needed for version catalog migration
  - Do not add new dependencies beyond version catalog infrastructure
  - Do not create multi-module structure

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: [`architecture`]
    - `architecture`: Build system setup, dependency management, project structure
  - **Skills Evaluated but Omitted**:
    - `frontend-ui-ux`: No UI work in this task

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 2 (solo)
  - **Blocks**: Tasks 3, 4, 5, 6, 7, 8
  - **Blocked By**: Task 1

  **References**:

  **Pattern References**:
  - `app/build.gradle.kts` — Current module build file to migrate to version catalog
  - `settings.gradle.kts` — Current settings, needs JitPack repo for termux-shared

  **API/Type References**:
  - `app/src/main/AndroidManifest.xml:12-16` — References `@style/Theme.AgentConsole`, `@mipmap/ic_launcher`, `@mipmap/ic_launcher_round` — these resources MUST be created

  **External References**:
  - Gradle version catalog: https://docs.gradle.org/current/userguide/platforms.html
  - AGP 8.5.x compatibility: https://developer.android.com/build/releases/gradle-plugin
  - Compose BOM to Kotlin compatibility: https://developer.android.com/develop/ui/compose/bom/bom-mapping

  **WHY Each Reference Matters**:
  - The manifest references resources that don't exist — build WILL fail without them
  - Version catalog centralizes ALL versions — prevents AGP/Kotlin/Compose incompatibility
  - Gradle 8.7 is required for AGP 8.5.x

  **Acceptance Criteria**:
  - [ ] `./gradlew assembleDebug 2>&1 | tail -1` contains `BUILD SUCCESSFUL`
  - [ ] `./gradlew testDebugUnitTest 2>&1 | tail -1` contains `BUILD SUCCESSFUL`
  - [ ] `test -f gradlew && test -f gradle/wrapper/gradle-wrapper.jar && test -f gradle/libs.versions.toml && test -f LICENSE && echo all_exist` → `all_exist`
  - [ ] `test -f app/src/main/res/values/themes.xml && test -f app/src/main/res/values/strings.xml && echo res_exist` → `res_exist`

  **QA Scenarios:**

  ```
  Scenario: Debug build succeeds from fresh clone
    Tool: Bash
    Preconditions: ANDROID_HOME set, Gradle 8.7+ available
    Steps:
      1. Run: ./gradlew clean assembleDebug 2>&1
      2. Assert output contains "BUILD SUCCESSFUL"
      3. Assert APK exists: test -f app/build/outputs/apk/debug/app-debug.apk
    Expected Result: APK generated, build successful
    Failure Indicators: BUILD FAILED, missing resource errors, plugin resolution failure
    Evidence: .sisyphus/evidence/task-2-build-debug.txt

  Scenario: Unit tests pass
    Tool: Bash
    Preconditions: Build succeeds
    Steps:
      1. Run: ./gradlew testDebugUnitTest 2>&1
      2. Assert output contains "BUILD SUCCESSFUL"
      3. Check test report: test -f app/build/reports/tests/testDebugUnitTest/index.html
    Expected Result: All review branch tests pass (ExecutionStoreTest: 5 tests, TermuxRunnerValidationTest: 6 tests)
    Failure Indicators: Test failures, compilation errors
    Evidence: .sisyphus/evidence/task-2-tests-pass.txt

  Scenario: Version catalog exists and is used
    Tool: Bash (grep)
    Preconditions: Build files exist
    Steps:
      1. Run: test -f gradle/libs.versions.toml && echo exists
      2. Run: grep -c 'libs\.' app/build.gradle.kts
      3. Assert count > 5 (most deps migrated)
    Expected Result: Version catalog file exists and app build.gradle.kts references it
    Failure Indicators: No libs.versions.toml or zero catalog references in build file
    Evidence: .sisyphus/evidence/task-2-version-catalog.txt
  ```

  **Commit**: YES
  - Message: `build: add gradle wrapper, root build, resources, version catalog, LICENSE`
  - Files: `gradlew`, `gradlew.bat`, `gradle/`, `build.gradle.kts`, `app/build.gradle.kts`, `app/src/main/res/`, `gradle/libs.versions.toml`, `LICENSE`
  - Pre-commit: `./gradlew assembleDebug testDebugUnitTest`

---

### WAVE 3 — Architecture + CI (PARALLEL)

- [ ] 3. Architecture Refactor: ViewModel + Hilt DI + TermuxRepository

  **What to do**:
  - **TDD: Write tests FIRST for each component before implementing:**
  - Write `MainViewModelTest.kt` — test state transitions: idle → running → finished/failed, validate prompt/workdir before run, expose reactive state
  - Write `TermuxRepositoryTest.kt` — test validation logic, mock Context for isTermuxInstalled, test intent construction logic extractable as pure functions
  - Add Hilt dependencies to version catalog: `hilt-android`, `hilt-compiler`, `hilt-android-testing`
  - Add KSP plugin to app build.gradle.kts (for Hilt annotation processing)
  - Create `AgentConsoleApplication.kt` annotated with `@HiltAndroidApp`
  - Register Application class in `AndroidManifest.xml`: `android:name=".AgentConsoleApplication"`
  - Create `TermuxRepository.kt` — injectable class (`@Inject constructor`) extracting from current `TermuxRunner`:
    - `fun isTermuxInstalled(context: Context): Boolean`
    - `fun openTermux(context: Context)`
    - `fun validateWorkingDir(workingDir: String): String?`
    - `fun runAgent(context: Context, agent: Agent, prompt: String, workingDir: String): Int` (returns executionId)
  - Create `MainViewModel.kt` with `@HiltViewModel` — absorbs `ExecutionStore` state management:
    - Private `MutableStateFlow<ExecutionUiState>` (moved from ExecutionStore)
    - Public `val uiState: StateFlow<ExecutionUiState>`
    - `fun run(agent, prompt, workingDir)` — validates inputs, calls repository
    - `fun publishResult(executionId, stdout, stderr, exitCode, ...)` — with stale result discard
    - `fun fail(message)` 
    - `fun checkTermuxInstalled(context): Boolean` — re-checks on call (not cached)
  - Create `di/AppModule.kt` with `@Module @InstallIn(SingletonComponent::class)` — binds TermuxRepository
  - **DELETE** `ExecutionStore.kt` (absorbed into MainViewModel)
  - **DELETE** `TermuxRunner.kt` (absorbed into TermuxRepository)
  - Update `MainActivity.kt`:
    - Annotate with `@AndroidEntryPoint`
    - `AgentConsoleApp()` receives ViewModel via `viewModel<MainViewModel>()`
    - Replace `ExecutionStore.state.collectAsState()` with `viewModel.uiState.collectAsState()`
    - Replace `TermuxRunner.run()` calls with `viewModel.run()`
    - Replace `TermuxRunner.isTermuxInstalled()` with `viewModel.checkTermuxInstalled(context)` (no longer cached via `remember`)
    - Replace `TermuxRunner.openTermux()` with direct call (can stay as simple utility or move to ViewModel)
  - Update `TermuxResultService.kt`:
    - Needs to communicate results back to ViewModel. Since Service can't easily inject ViewModel, use an application-scoped `ResultBus` (simple `MutableSharedFlow` in the Hilt singleton scope) that the ViewModel collects
    - OR keep a minimal singleton `ResultBus` object just for Service→ViewModel communication (acceptable tradeoff — Service lifecycle prevents clean DI)
  - Upgrade `termux-shared` from `0.118.0` to `0.118.3` in version catalog
  - Migrate existing tests (`ExecutionStoreTest.kt`, `TermuxRunnerValidationTest.kt`) to test the new ViewModel and Repository instead
  - Run all tests: `./gradlew testDebugUnitTest`

  **Must NOT do**:
  - Do not create abstract interface `ITermuxRepository` — single implementation, no need
  - Do not create UseCase/Interactor layers
  - Do not create multi-module structure
  - Do not add more than 1 Hilt `@Module`
  - Do not add instrumented tests or Compose UI tests

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: [`architecture`, `refactor`]
    - `architecture`: ViewModel/DI setup, dependency graph design
    - `refactor`: Safe migration of existing code to new structure
  - **Skills Evaluated but Omitted**:
    - `test-writer`: Tests are part of TDD flow within this task, not a separate concern
    - `security-audit`: Security is Phase 4, not this task

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 4)
  - **Parallel Group**: Wave 3
  - **Blocks**: Tasks 5, 6, 7, 8 (everything needs architecture in place)
  - **Blocked By**: Task 2 (must be buildable first)

  **References**:

  **Pattern References**:
  - `app/src/main/java/com/example/agentconsole/ExecutionStore.kt` — Current state management to absorb into ViewModel. StateFlow pattern is correct, just needs to move from global singleton to ViewModel scope
  - `app/src/main/java/com/example/agentconsole/TermuxRunner.kt` — Current business logic to extract into Repository. `run()`, `isTermuxInstalled()`, `openTermux()`, `validateWorkingDir()` all move here
  - `app/src/main/java/com/example/agentconsole/TermuxResultService.kt` — Service that receives Termux callbacks. Needs bridge to ViewModel (ResultBus pattern)
  - `app/src/main/java/com/example/agentconsole/MainActivity.kt` — UI layer to update: replace direct singleton calls with ViewModel calls
  - `app/src/test/java/com/example/agentconsole/ExecutionStoreTest.kt` — Existing tests to migrate to MainViewModelTest
  - `app/src/test/java/com/example/agentconsole/TermuxRunnerValidationTest.kt` — Existing tests to migrate to TermuxRepositoryTest

  **External References**:
  - Hilt Android setup: https://developer.android.com/training/dependency-injection/hilt-android
  - ViewModel with Hilt: https://developer.android.com/training/dependency-injection/hilt-jetpack
  - Compose + ViewModel: https://developer.android.com/develop/ui/compose/libraries#viewmodel

  **WHY Each Reference Matters**:
  - ExecutionStore.kt shows the exact StateFlow pattern to preserve in ViewModel
  - TermuxRunner.kt shows all business logic methods that become Repository methods
  - Existing tests prove the current behavior — migrated tests ensure behavior preservation

  **Acceptance Criteria**:
  - [ ] `./gradlew assembleDebug` → BUILD SUCCESSFUL
  - [ ] `./gradlew testDebugUnitTest` → all tests pass (migrated + new)
  - [ ] `grep -r "^object ExecutionStore\|^object TermuxRunner" app/src/main/java/ --include="*.kt"` → no matches
  - [ ] `grep -r "@HiltAndroidApp" app/src/main/java/ --include="*.kt"` → exactly 1 match
  - [ ] `grep -r "@HiltViewModel" app/src/main/java/ --include="*.kt"` → exactly 1 match
  - [ ] `grep -r "@AndroidEntryPoint" app/src/main/java/ --include="*.kt"` → at least 1 match (MainActivity)
  - [ ] `test ! -f app/src/main/java/com/example/agentconsole/ExecutionStore.kt && echo deleted` → `deleted`
  - [ ] `test ! -f app/src/main/java/com/example/agentconsole/TermuxRunner.kt && echo deleted` → `deleted`

  **QA Scenarios:**

  ```
  Scenario: Architecture components present
    Tool: Bash (grep)
    Preconditions: Build succeeds
    Steps:
      1. grep -r "@HiltAndroidApp" app/src/main/java/ --include="*.kt" | wc -l
      2. Assert count == 1
      3. grep -r "@HiltViewModel" app/src/main/java/ --include="*.kt" | wc -l
      4. Assert count == 1
      5. grep -r "@AndroidEntryPoint" app/src/main/java/ --include="*.kt" | wc -l
      6. Assert count >= 1
      7. test -f app/src/main/java/com/example/agentconsole/di/AppModule.kt && echo exists
    Expected Result: All Hilt annotations present, AppModule exists
    Failure Indicators: Missing annotations, AppModule not found
    Evidence: .sisyphus/evidence/task-3-architecture.txt

  Scenario: Global singletons removed
    Tool: Bash (grep)
    Preconditions: Build succeeds
    Steps:
      1. grep -rn "^object" app/src/main/java/com/example/agentconsole/ --include="*.kt"
      2. Assert only matches are Agent.kt (enum is fine) or companion objects
      3. Verify ExecutionStore.kt deleted: test ! -f app/src/main/java/com/example/agentconsole/ExecutionStore.kt
      4. Verify TermuxRunner.kt deleted: test ! -f app/src/main/java/com/example/agentconsole/TermuxRunner.kt
    Expected Result: No global mutable singleton objects remain
    Failure Indicators: ExecutionStore or TermuxRunner still exist as files
    Evidence: .sisyphus/evidence/task-3-singletons-removed.txt

  Scenario: All tests pass after refactor
    Tool: Bash
    Preconditions: Architecture refactor complete
    Steps:
      1. Run: ./gradlew testDebugUnitTest 2>&1
      2. Assert output contains "BUILD SUCCESSFUL"
      3. Assert no test failures in output
    Expected Result: All migrated + new tests pass
    Failure Indicators: Test compilation errors, assertion failures
    Evidence: .sisyphus/evidence/task-3-tests.txt
  ```

  **Commit**: YES
  - Message: `refactor: introduce ViewModel, Hilt DI, TermuxRepository, remove global singletons`
  - Files: `AgentConsoleApplication.kt`, `MainViewModel.kt`, `TermuxRepository.kt`, `di/AppModule.kt`, updated `MainActivity.kt`, `TermuxResultService.kt`, deleted `ExecutionStore.kt`, `TermuxRunner.kt`, updated + new tests
  - Pre-commit: `./gradlew testDebugUnitTest`

---

- [ ] 4. CI/CD Pipeline + Community Files + GitHub Config

  **What to do**:
  - Create `.github/workflows/android.yml` GitHub Actions workflow:
    - Trigger: push to `main`, pull requests to `main`
    - Runner: `ubuntu-latest`
    - Steps: checkout, set up JDK 17, setup Gradle, run `./gradlew assembleDebug testDebugUnitTest lintDebug`
    - Cache Gradle dependencies
  - Create `.github/ISSUE_TEMPLATE/bug_report.md` with fields: description, steps to reproduce, expected behavior, device info, Termux version
  - Create `.github/ISSUE_TEMPLATE/feature_request.md` with fields: description, use case, alternatives considered
  - Create `.github/PULL_REQUEST_TEMPLATE.md` with sections: what changed, why, testing done, checklist
  - Create `CONTRIBUTING.md` — brief guide: how to build, how to test, PR process, code style
  - Set GitHub topics via: `gh api repos/SynthNoirLabs/AgentConsole/topics -X PUT -f names='["android","termux","ai","claude","jetpack-compose","kotlin","cli","gemini","opencode"]'`
  - Update `README.md`:
    - Add CI badge: `![CI](https://github.com/SynthNoirLabs/AgentConsole/actions/workflows/android.yml/badge.svg)`
    - Add build-from-source instructions: `git clone ... && ./gradlew assembleDebug`
    - Remove "Very early WIP" from title (it's becoming a real project now)

  **Must NOT do**:
  - Do not add deployment/release workflows (not needed yet)
  - Do not add code coverage reporting (premature for this stage)
  - Do not spend more than 30 minutes on template writing

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`doc-writer`]
    - `doc-writer`: Documentation, templates, markdown files
  - **Skills Evaluated but Omitted**:
    - `architecture`: No architecture work in this task

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 3)
  - **Parallel Group**: Wave 3
  - **Blocks**: Task 7 (feature work should have CI green first)
  - **Blocked By**: Task 2 (needs buildable project)

  **References**:

  **Pattern References**:
  - `README.md` — Current README to update with badge and build instructions

  **External References**:
  - GitHub Actions for Android: https://github.com/actions/setup-java
  - Gradle Build Action: https://github.com/gradle/actions/tree/main/setup-gradle

  **WHY Each Reference Matters**:
  - CI workflow ensures every push/PR verifies the build — catches regressions immediately
  - Community files raise GitHub health score from 37% toward 100%

  **Acceptance Criteria**:
  - [ ] `test -f .github/workflows/android.yml && echo exists` → `exists`
  - [ ] `grep -c 'assembleDebug\|testDebugUnitTest' .github/workflows/android.yml` → at least 2
  - [ ] `test -f CONTRIBUTING.md && echo exists` → `exists`
  - [ ] `test -f .github/ISSUE_TEMPLATE/bug_report.md && echo exists` → `exists`
  - [ ] `test -f .github/PULL_REQUEST_TEMPLATE.md && echo exists` → `exists`
  - [ ] `grep -c 'badge' README.md` → at least 1

  **QA Scenarios:**

  ```
  Scenario: CI workflow is valid YAML with required steps
    Tool: Bash
    Preconditions: .github/workflows/android.yml exists
    Steps:
      1. Run: cat .github/workflows/android.yml | python3 -c 'import sys,yaml; yaml.safe_load(sys.stdin)' && echo valid
      2. Assert output: valid
      3. Run: grep -c 'assembleDebug' .github/workflows/android.yml
      4. Assert count >= 1
      5. Run: grep -c 'testDebugUnitTest\|test' .github/workflows/android.yml
      6. Assert count >= 1
    Expected Result: Valid YAML with build and test steps
    Failure Indicators: YAML parse error, missing build/test commands
    Evidence: .sisyphus/evidence/task-4-ci-workflow.txt

  Scenario: GitHub topics set correctly
    Tool: Bash (gh api)
    Preconditions: GitHub CLI authenticated
    Steps:
      1. Run: gh api repos/SynthNoirLabs/AgentConsole/topics --jq '.names[]'
      2. Assert output includes: android, termux, jetpack-compose
    Expected Result: Topics visible on repo page
    Failure Indicators: Empty topic list or missing key topics
    Evidence: .sisyphus/evidence/task-4-topics.txt

  Scenario: Community files present
    Tool: Bash
    Steps:
      1. test -f CONTRIBUTING.md && echo ok
      2. test -f .github/ISSUE_TEMPLATE/bug_report.md && echo ok
      3. test -f .github/ISSUE_TEMPLATE/feature_request.md && echo ok
      4. test -f .github/PULL_REQUEST_TEMPLATE.md && echo ok
    Expected Result: All 4 files exist
    Failure Indicators: Any file missing
    Evidence: .sisyphus/evidence/task-4-community-files.txt
  ```

  **Commit**: YES
  - Message: `ci: add GitHub Actions workflow, community files, repo topics`
  - Files: `.github/`, `CONTRIBUTING.md`, `README.md`
  - Pre-commit: n/a

---

### WAVE 4 — Robustness + Data Layer (PARALLEL)

- [ ] 5. Robustness: Output Truncation, Prompt Validation, Foreground Service, Battery Warning

  **What to do**:
  - **TDD: Write tests FIRST:**
  - Write tests for output truncation logic: verify truncation at configurable limit (default 50KB), verify truncation suffix shows original length
  - Write tests for prompt validation: max length (10KB), reject null bytes, validate empty/blank
  - Write tests for battery optimization check (mock `PowerManager`)
  - **Then implement:**
  - In `TermuxRepository` or a new `OutputProcessor` utility:
    - Add output truncation: if stdout/stderr > MAX_OUTPUT_SIZE (50KB), truncate with `[...truncated — {originalLength} bytes total]` suffix
    - Check Termux extras `EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH` and `EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH` to detect server-side truncation
    - Show warning in UI if original length > received length
  - In `MainViewModel` or `TermuxRepository`:
    - Add `validatePrompt(prompt: String): String?` — max 10KB, reject if contains null bytes, reject if blank
    - Wire validation into UI with inline error display (same pattern as workdir validation from review branch)
  - In `TermuxRepository.runAgent()`:
    - Switch from `PendingIntent.getService()` to `PendingIntent.getForegroundService()` for Android 8+ (Background start restrictions — Termux wiki confirmed)
    - Update `TermuxResultService` to call `startForeground()` with a minimal notification, then `stopForeground()` after processing
    - Add notification channel setup in `AgentConsoleApplication`
  - In UI (StatusCard or new composable):
    - Check `PowerManager.isIgnoringBatteryOptimizations()` for Termux package
    - If NOT ignoring: show warning text in StatusCard: "Termux may be killed by battery optimization. Tap to exempt."
    - On tap: launch `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent for Termux package

  **Must NOT do**:
  - Do not implement `EXTRA_RESULT_DIRECTORY` (complex file-based approach — UI truncation is sufficient for now)
  - Do not sanitize prompt content (AI prompts are free-form)
  - Do not add notification styling beyond minimal

  **Recommended Agent Profile**:
  - **Category**: `unspecified-high`
  - **Skills**: [`test-writer`, `security-audit`]
    - `test-writer`: TDD test creation for validation/truncation logic
    - `security-audit`: Input validation, PendingIntent security considerations
  - **Skills Evaluated but Omitted**:
    - `frontend-ui-ux`: Minor UI additions only (warning text), not a UI task

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 6)
  - **Parallel Group**: Wave 4
  - **Blocks**: Tasks 7, 8
  - **Blocked By**: Task 3 (needs ViewModel/Repository architecture)

  **References**:

  **Pattern References**:
  - `app/src/main/java/com/example/agentconsole/TermuxRunner.kt:33-49` (after merge) — `validateWorkingDir()` pattern: returns null if valid, error string if invalid. Follow this exact pattern for `validatePrompt()`
  - `app/src/main/java/com/example/agentconsole/MainActivity.kt:92-115` (after merge) — Inline validation UI pattern with `isError` and `supportingText`. Copy for prompt validation
  - `app/src/main/java/com/example/agentconsole/TermuxResultService.kt:20-24` (after merge) — Where stdout/stderr are extracted from bundle. Add truncation check here

  **External References**:
  - Termux output truncation: `com.termux.shared.data.DataUtils.TRANSACTION_SIZE_LIMIT_IN_BYTES` = 100KB
  - Foreground service: https://developer.android.com/develop/background-work/services/foreground-services
  - Battery optimization: https://developer.android.com/reference/android/os/PowerManager#isIgnoringBatteryOptimizations(java.lang.String)

  **WHY Each Reference Matters**:
  - validateWorkingDir pattern shows the exact validation convention to follow for consistency
  - Termux truncates at 100KB silently — must detect and warn user
  - getForegroundService prevents silent PendingIntent failures on Android 12+

  **Acceptance Criteria**:
  - [ ] `./gradlew testDebugUnitTest` → all tests pass (including new truncation + validation tests)
  - [ ] `grep -r 'truncat\|TRUNCAT\|MAX_OUTPUT' app/src/main/java/ --include='*.kt'` → matches found
  - [ ] `grep -r 'validatePrompt\|PROMPT_MAX\|MAX_PROMPT' app/src/main/java/ --include='*.kt'` → matches found
  - [ ] `grep -r 'getForegroundService\|startForeground' app/src/main/java/ --include='*.kt'` → matches found
  - [ ] `grep -r 'isIgnoringBatteryOptimizations\|BATTERY' app/src/main/java/ --include='*.kt'` → matches found

  **QA Scenarios:**

  ```
  Scenario: Output truncation logic works correctly
    Tool: Bash (tests)
    Preconditions: Build succeeds
    Steps:
      1. Run: ./gradlew testDebugUnitTest --tests "*OutputProcessor*" 2>&1
      2. Assert BUILD SUCCESSFUL
      3. Grep test report for truncation tests passing
    Expected Result: Truncation tests pass — strings > 50KB get truncated with suffix
    Failure Indicators: Test failures on truncation assertions
    Evidence: .sisyphus/evidence/task-5-truncation-tests.txt

  Scenario: Prompt validation rejects invalid input
    Tool: Bash (tests)
    Preconditions: Build succeeds
    Steps:
      1. Run: ./gradlew testDebugUnitTest --tests "*validatePrompt*" 2>&1 || ./gradlew testDebugUnitTest --tests "*PromptValidat*" 2>&1
      2. Assert BUILD SUCCESSFUL
    Expected Result: Blank prompts rejected, prompts > 10KB rejected, null bytes rejected
    Failure Indicators: Validation tests fail
    Evidence: .sisyphus/evidence/task-5-prompt-validation-tests.txt

  Scenario: Foreground service setup present
    Tool: Bash (grep)
    Preconditions: Source compiled
    Steps:
      1. grep -r 'getForegroundService' app/src/main/java/ --include='*.kt' | wc -l
      2. Assert count >= 1
      3. grep -r 'startForeground' app/src/main/java/ --include='*.kt' | wc -l
      4. Assert count >= 1
    Expected Result: PendingIntent uses getForegroundService, Service calls startForeground
    Failure Indicators: Still using getService(), no startForeground call
    Evidence: .sisyphus/evidence/task-5-foreground-service.txt
  ```

  **Commit**: YES
  - Message: `feat: add output truncation, prompt validation, foreground service, battery warning`
  - Files: `TermuxRepository.kt`, `MainViewModel.kt`, `TermuxResultService.kt`, `AgentConsoleApplication.kt`, new tests, updated UI
  - Pre-commit: `./gradlew testDebugUnitTest`

---

- [ ] 6. Room Execution History Data Layer

  **What to do**:
  - **TDD: Write tests FIRST:**
  - Write `ExecutionHistoryDaoTest.kt` in `app/src/test/` using Room in-memory database + **Robolectric** (`@RunWith(RobolectricTestRunner::class)`):
    - Room DAO tests require Android context which Robolectric provides without emulator
    - Test insert + getAll returns correct order (newest first)
    - Test deleteOlderThan removes old entries
    - Test entity fields are correctly stored/retrieved
  - Write `HistoryRepositoryTest.kt` if a repository wrapper is used:
    - Test save-on-result lifecycle (pure Kotlin, no Robolectric needed)
  - **Then implement:**
  - Add Room dependencies to version catalog: `room-runtime`, `room-ktx`, `room-compiler` (KSP), `room-testing`
  - Add Robolectric test dependency: `testImplementation("org.robolectric:robolectric:4.12.2")`
  - Create `data/ExecutionHistory.kt` — Room `@Entity`:
    - `id: Long` (auto-generate)
    - `agent: String` (agent display name)
    - `workingDir: String`
    - `prompt: String` (truncated to first 500 chars for storage)
    - `stdout: String` (truncated to first 10KB for storage)
    - `stderr: String` (truncated to first 5KB for storage)
    - `exitCode: Int`
    - `status: String` ("Finished", "Finished with errors", "Failed")
    - `timestamp: Long` (epoch millis)
  - Create `data/ExecutionHistoryDao.kt` — `@Dao`:
    - `@Insert fun insert(entry: ExecutionHistory)`
    - `@Query("SELECT * FROM execution_history ORDER BY timestamp DESC") fun getAll(): Flow<List<ExecutionHistory>>`
    - `@Query("DELETE FROM execution_history WHERE timestamp < :cutoff") fun deleteOlderThan(cutoff: Long)`
  - Create `data/AppDatabase.kt` — `@Database(entities = [ExecutionHistory::class], version = 1)`:
    - Abstract `fun executionHistoryDao(): ExecutionHistoryDao`
  - Provide database via Hilt `di/AppModule.kt`:
    - `Room.databaseBuilder(context, AppDatabase::class.java, "agent_console.db").build()`
  - Wire into `MainViewModel`:
    - On successful `publishResult()`, also insert into Room via DAO
  - Run all tests: `./gradlew testDebugUnitTest`

  **Must NOT do**:
  - Do not add migration strategy yet (version 1, no migrations needed)
  - Do not add search/filter to DAO (simple list for now)
  - Do not add export/share functionality
  - Do not store full stdout/stderr — truncate for storage efficiency

  **Recommended Agent Profile**:
  - **Category**: `deep`
  - **Skills**: [`architecture`, `test-writer`]
    - `architecture`: Database design, DAO patterns, Hilt integration
    - `test-writer`: TDD with Room in-memory database testing
  - **Skills Evaluated but Omitted**:
    - `refactor`: Not refactoring existing code, building new

  **Parallelization**:
  - **Can Run In Parallel**: YES (with Task 5)
  - **Parallel Group**: Wave 4
  - **Blocks**: Tasks 7, 8
  - **Blocked By**: Task 3 (needs Hilt DI + KSP for Room annotation processing)

  **References**:

  **Pattern References**:
  - `di/AppModule.kt` (from Task 3) — Add Room database provider to existing Hilt module
  - `MainViewModel.kt` (from Task 3) — Inject DAO, call insert on publishResult()

  **External References**:
  - Room setup: https://developer.android.com/training/data-storage/room
  - Room with Hilt: https://developer.android.com/training/dependency-injection/hilt-jetpack#room
  - Room testing: https://developer.android.com/training/data-storage/room/testing-db

  **WHY Each Reference Matters**:
  - Room is the standard Android persistence library for structured data
  - In-memory Room databases allow fast unit testing without device/emulator
  - Hilt integration follows the established DI pattern from Task 3

  **Acceptance Criteria**:
  - [ ] `./gradlew assembleDebug` → BUILD SUCCESSFUL
  - [ ] `./gradlew testDebugUnitTest` → all tests pass (including DAO tests)
  - [ ] `grep -r '@Entity' app/src/main/java/ --include='*.kt'` → 1 match
  - [ ] `grep -r '@Dao' app/src/main/java/ --include='*.kt'` → 1 match
  - [ ] `grep -r '@Database' app/src/main/java/ --include='*.kt'` → 1 match

  **QA Scenarios:**

  ```
  Scenario: Room entity and DAO compile and test correctly
    Tool: Bash
    Preconditions: Build succeeds, Hilt + KSP configured
    Steps:
      1. Run: ./gradlew testDebugUnitTest --tests "*ExecutionHistoryDao*" 2>&1
      2. Assert BUILD SUCCESSFUL
      3. Assert test report shows pass for insert/getAll/deleteOlderThan tests
    Expected Result: DAO operations work with in-memory database
    Failure Indicators: Schema errors, missing type converters, query syntax errors
    Evidence: .sisyphus/evidence/task-6-dao-tests.txt

  Scenario: History is saved on execution result
    Tool: Bash (tests)
    Preconditions: ViewModel + Repository + Room wired
    Steps:
      1. Run: ./gradlew testDebugUnitTest --tests "*MainViewModel*" 2>&1
      2. Verify tests include: publishResult triggers Room insert
    Expected Result: Execution results are persisted to Room
    Failure Indicators: Insert not called after publishResult
    Evidence: .sisyphus/evidence/task-6-history-integration.txt

  Scenario: Database components exist in codebase
    Tool: Bash (grep)
    Steps:
      1. grep -r '@Entity' app/src/main/java/ --include='*.kt' | wc -l → >= 1
      2. grep -r '@Dao' app/src/main/java/ --include='*.kt' | wc -l → >= 1  
      3. grep -r '@Database' app/src/main/java/ --include='*.kt' | wc -l → >= 1
      4. grep -r 'agent_console.db' app/src/main/java/ --include='*.kt' | wc -l → >= 1
    Expected Result: All Room components present
    Failure Indicators: Missing annotations
    Evidence: .sisyphus/evidence/task-6-room-components.txt
  ```

  **Commit**: YES
  - Message: `feat: add Room execution history data layer`
  - Files: `data/ExecutionHistory.kt`, `data/ExecutionHistoryDao.kt`, `data/AppDatabase.kt`, `di/AppModule.kt` (updated), `MainViewModel.kt` (updated), tests
  - Pre-commit: `./gradlew testDebugUnitTest`

---

### WAVE 5 — UI Features

- [ ] 7. Dark Mode + Navigation + SAF Directory Picker + History Screen

  **What to do**:
  - **TDD where applicable (navigation routes, ViewModel logic). UI composables tested via QA scenarios.**
  - **Dark Mode:**
    - Create `ui/theme/Theme.kt` with `AgentConsoleTheme` composable
    - Define `LightColorScheme` and `DarkColorScheme` using Material3 `lightColorScheme()` / `darkColorScheme()`
    - Use `isSystemInDarkTheme()` to auto-switch (follow system setting)
    - Wrap entire app in `AgentConsoleTheme` (replace bare `MaterialTheme`)
    - Update `app/src/main/res/values/themes.xml` to use `Theme.Material3.DayNight.NoActionBar`
  - **Navigation:**
    - Add `navigation-compose` dependency to version catalog
    - Create `ui/navigation/NavGraph.kt` with `NavHost`:
      - Route `"main"` → current `AgentConsoleApp()` screen
      - Route `"history"` → new `HistoryScreen()` composable
    - Add navigation button in main screen (e.g., TopAppBar icon or bottom nav item) to go to history
    - Update `MainActivity.kt` to use `NavGraph` instead of direct `AgentConsoleApp()` call
  - **History Screen:**
    - Create `HistoryViewModel.kt` with `@HiltViewModel` — exposes `Flow<List<ExecutionHistory>>` from DAO
    - Create `ui/history/HistoryScreen.kt` composable:
      - `LazyColumn` showing execution history entries
      - Each item shows: agent name, timestamp, status (with color indicator), first line of prompt
      - Tap item to expand and show full stdout/stderr
      - Empty state: "No execution history yet"
    - Write `HistoryViewModelTest.kt` — test that ViewModel exposes DAO data correctly
  - **SAF Directory Picker:**
    - Add `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree())` in main screen
    - Add a folder-icon button next to the working directory text field
    - On result: convert URI to file path, update `workingDir` state + SharedPreferences
    - Keep the text field editable (user can still type manually)
    - Handle permission: persist URI access with `contentResolver.takePersistableUriPermission()`

  **Must NOT do**:
  - Do not add theme toggle (follow system only — toggle is a future enhancement)
  - Do not add bottom navigation bar (simple TopAppBar navigation icon is sufficient for 2 screens)
  - Do not add search/filter to history screen
  - Do not add delete/clear history UI (DAO has `deleteOlderThan` for internal cleanup only)
  - Do not add Compose UI tests

  **Recommended Agent Profile**:
  - **Category**: `visual-engineering`
  - **Skills**: [`frontend-ui-ux`]
    - `frontend-ui-ux`: Compose UI design, theming, navigation, Material3 components
  - **Skills Evaluated but Omitted**:
    - `architecture`: Architecture is already in place from Task 3

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 5 (solo)
  - **Blocks**: Task 8
  - **Blocked By**: Tasks 5, 6 (needs robustness + Room data layer)

  **References**:

  **Pattern References**:
  - `app/src/main/java/com/example/agentconsole/MainActivity.kt` — Current UI to split into NavGraph. `AgentConsoleApp()`, `StatusCard()`, `AgentDropdown()`, `OutputCard()` composables move to main screen route
  - `app/src/main/java/com/example/agentconsole/MainViewModel.kt` (from Task 3) — Existing ViewModel pattern to follow for HistoryViewModel
  - `data/ExecutionHistoryDao.kt` (from Task 6) — `getAll(): Flow<List<ExecutionHistory>>` to collect in HistoryViewModel

  **External References**:
  - Compose Navigation: https://developer.android.com/develop/ui/compose/navigation
  - Material3 theming: https://developer.android.com/develop/ui/compose/designsystems/material3
  - SAF (Storage Access Framework): https://developer.android.com/guide/topics/providers/document-provider
  - OpenDocumentTree: https://developer.android.com/reference/android/content/Intent#ACTION_OPEN_DOCUMENT_TREE

  **WHY Each Reference Matters**:
  - Navigation setup enables multi-screen app (main + history)
  - Material3 DayNight theme gives automatic dark mode with zero manual color mapping
  - SAF is the modern Android way to pick directories safely

  **Acceptance Criteria**:
  - [ ] `./gradlew assembleDebug` → BUILD SUCCESSFUL
  - [ ] `./gradlew testDebugUnitTest` → all tests pass
  - [ ] `grep -r 'NavHost' app/src/main/java/ --include='*.kt'` → at least 1 match
  - [ ] `grep -r 'darkColorScheme' app/src/main/java/ --include='*.kt'` → at least 1 match
  - [ ] `grep -r 'ACTION_OPEN_DOCUMENT_TREE\|OpenDocumentTree' app/src/main/java/ --include='*.kt'` → at least 1 match
  - [ ] `grep -r 'HistoryScreen' app/src/main/java/ --include='*.kt'` → at least 1 match

  **QA Scenarios:**

  ```
  Scenario: App builds with all new UI features
    Tool: Bash
    Preconditions: All Wave 4 tasks complete
    Steps:
      1. Run: ./gradlew clean assembleDebug 2>&1
      2. Assert output contains "BUILD SUCCESSFUL"
      3. Assert APK exists: test -f app/build/outputs/apk/debug/app-debug.apk
    Expected Result: Clean build succeeds with navigation, theming, history screen, SAF picker
    Failure Indicators: Compose compilation errors, missing imports, theme resolution failures
    Evidence: .sisyphus/evidence/task-7-build.txt

  Scenario: Theme, navigation, and SAF components present in source
    Tool: Bash (grep)
    Steps:
      1. grep -r 'AgentConsoleTheme' app/src/main/java/ --include='*.kt' | wc -l → >= 2
      2. grep -r 'NavHost' app/src/main/java/ --include='*.kt' | wc -l → >= 1
      3. grep -r 'darkColorScheme' app/src/main/java/ --include='*.kt' | wc -l → >= 1
      4. grep -r 'HistoryScreen' app/src/main/java/ --include='*.kt' | wc -l → >= 1
      5. grep -r 'OpenDocumentTree' app/src/main/java/ --include='*.kt' | wc -l → >= 1
    Expected Result: All UI feature components exist
    Failure Indicators: Missing composables or theme definitions
    Evidence: .sisyphus/evidence/task-7-features-present.txt

  Scenario: All tests still pass after UI additions
    Tool: Bash
    Steps:
      1. Run: ./gradlew testDebugUnitTest 2>&1
      2. Assert BUILD SUCCESSFUL
      3. Assert no test failures
    Expected Result: No regressions from UI changes
    Failure Indicators: Any test failure
    Evidence: .sisyphus/evidence/task-7-tests.txt
  ```

  **Commit**: YES
  - Message: `feat: add dark mode, navigation, SAF directory picker, history screen`
  - Files: `ui/theme/Theme.kt`, `ui/navigation/NavGraph.kt`, `ui/history/HistoryScreen.kt`, `HistoryViewModel.kt`, updated `MainActivity.kt`, `res/values/themes.xml`
  - Pre-commit: `./gradlew assembleDebug testDebugUnitTest`

---

### WAVE 6 — Final Build Verification + README

- [ ] 8. Full Build Verification + Final README Update

  **What to do**:
  - Run the complete verification suite:
    - `./gradlew clean assembleDebug` — debug build
    - `./gradlew assembleRelease` — release build (verifies proguard + minification)
    - `./gradlew testDebugUnitTest` — all tests
    - `./gradlew lintDebug` — lint checks
  - Fix any issues found by the full build
  - Update `README.md` comprehensively:
    - Remove "Very early WIP Project, expect nothing" from title
    - Add CI badge at top
    - Update "File Structure" section to reflect new architecture (ViewModel, Repository, Room, etc.)
    - Update "Supported Agents" if any changes
    - Update "Quick Start" with build-from-source: `git clone ... && ./gradlew assembleDebug`
    - Replace "What to Add Next" section with current status:
      - ✅ Execution history screen
      - ✅ Theme / dark mode support
      - ✅ Repo picker with SAF
      - ⬜ Streaming output via Termux sessions (future)
    - Add "Architecture" section briefly describing: ViewModel + Hilt + Room + Compose Navigation
    - Add "Contributing" link to CONTRIBUTING.md
    - Verify LICENSE file is mentioned

  **Must NOT do**:
  - Do not add detailed API documentation
  - Do not add screenshots (no device to capture from)
  - Do not change app functionality

  **Recommended Agent Profile**:
  - **Category**: `quick`
  - **Skills**: [`doc-writer`]
    - `doc-writer`: README documentation, markdown formatting
  - **Skills Evaluated but Omitted**:
    - `code-review`: This is documentation, not code review

  **Parallelization**:
  - **Can Run In Parallel**: NO
  - **Parallel Group**: Wave 6 (solo)
  - **Blocks**: F1-F4 (final verification)
  - **Blocked By**: Task 7

  **References**:

  **Pattern References**:
  - `README.md` — Current README to update. Keep the concise style but expand architecture section

  **Acceptance Criteria**:
  - [ ] `./gradlew clean assembleDebug assembleRelease testDebugUnitTest lintDebug 2>&1 | grep -c 'BUILD SUCCESSFUL'` → at least 1
  - [ ] `test -f app/build/outputs/apk/release/app-release-unsigned.apk && echo exists` (or similar release APK path)
  - [ ] `grep -c 'badge' README.md` → at least 1
  - [ ] `grep -c 'Architecture' README.md` → at least 1
  - [ ] README no longer contains "Very early WIP"

  **QA Scenarios:**

  ```
  Scenario: Full build suite passes
    Tool: Bash
    Steps:
      1. Run: ./gradlew clean assembleDebug 2>&1 | tail -3
      2. Assert BUILD SUCCESSFUL
      3. Run: ./gradlew assembleRelease 2>&1 | tail -3
      4. Assert BUILD SUCCESSFUL (proguard + minification works)
      5. Run: ./gradlew testDebugUnitTest 2>&1 | tail -3
      6. Assert BUILD SUCCESSFUL
      7. Run: ./gradlew lintDebug 2>&1 | tail -3
      8. Assert BUILD SUCCESSFUL (or only warnings, no errors)
    Expected Result: All 4 Gradle tasks pass
    Failure Indicators: Any BUILD FAILED
    Evidence: .sisyphus/evidence/task-8-full-build.txt

  Scenario: README is updated and accurate
    Tool: Bash (grep)
    Steps:
      1. grep -c 'Very early WIP' README.md → 0 (removed)
      2. grep -c 'badge' README.md → >= 1
      3. grep -c 'Architecture\|ViewModel\|Hilt' README.md → >= 1
      4. grep -c 'gradlew assembleDebug\|Build from source' README.md → >= 1
    Expected Result: README reflects current project state
    Failure Indicators: Stale content, missing sections
    Evidence: .sisyphus/evidence/task-8-readme.txt
  ```

  **Commit**: YES
  - Message: `docs: update README with architecture, build instructions, CI badge`
  - Files: `README.md`
  - Pre-commit: `./gradlew clean assembleDebug assembleRelease testDebugUnitTest lintDebug`

## Final Verification Wave

> 4 review agents run in PARALLEL. ALL must APPROVE. Rejection → fix → re-run.

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, run command). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in `.sisyphus/evidence/`. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high` + `code-review` skill
  Run `./gradlew assembleDebug testDebugUnitTest lint`. Review all changed files for: `@Suppress`, empty catches, `println` in prod, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic names (data/result/item/temp). Verify no global mutable singletons remain (except Agent enum).
  Output: `Build [PASS/FAIL] | Lint [PASS/FAIL] | Tests [N pass/N fail] | Files [N clean/N issues] | VERDICT`

- [ ] F3. **Real QA** — `unspecified-high`
  Start from clean state. Execute EVERY QA scenario from EVERY task — follow exact steps, capture evidence. Test cross-task integration (features working together, not isolation). Test edge cases: empty prompt, blank workdir, long prompt. Save to `.sisyphus/evidence/final-qa/`.
  Output: `Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff (`git log`/`diff`). Verify 1:1 — everything in spec was built (no missing), nothing beyond spec was built (no creep). Check "Must NOT do" compliance. Detect cross-task contamination. Flag unaccounted changes.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | Unaccounted [CLEAN/N files] | VERDICT`

---

## Commit Strategy

| Task | Commit Message | Files | Pre-commit |
|------|---------------|-------|------------|
| 1 | `chore: merge review branch, fix default branch, add proguard rules` | git operations, `app/proguard-rules.pro` | n/a (git ops) |
| 2 | `build: add gradle wrapper, root build, resources, version catalog, LICENSE` | `gradlew`, `gradle/`, `build.gradle.kts`, `app/src/main/res/`, `gradle/libs.versions.toml`, `LICENSE` | `./gradlew assembleDebug` |
| 3 | `refactor: introduce ViewModel, Hilt DI, TermuxRepository, remove global singletons` | `AgentConsoleApplication.kt`, `MainViewModel.kt`, `TermuxRepository.kt`, `di/AppModule.kt`, updated `MainActivity.kt`, `TermuxResultService.kt`, deleted `ExecutionStore.kt`, `TermuxRunner.kt`, updated tests | `./gradlew testDebugUnitTest` |
| 4 | `ci: add GitHub Actions workflow, community files, topics` | `.github/`, `CONTRIBUTING.md`, README update | n/a |
| 5 | `feat: add output truncation, prompt validation, foreground service, battery warning` | `TermuxRepository.kt`, `MainViewModel.kt`, new tests | `./gradlew testDebugUnitTest` |
| 6 | `feat: add Room execution history data layer` | `ExecutionHistory.kt`, `ExecutionHistoryDao.kt`, `AppDatabase.kt`, `di/AppModule.kt`, tests | `./gradlew testDebugUnitTest` |
| 7 | `feat: add dark mode, navigation, SAF directory picker, history screen` | theme files, `NavGraph.kt`, `HistoryScreen.kt`, `HistoryViewModel.kt`, updated `MainActivity.kt` | `./gradlew assembleDebug` |
| 8 | `docs: update README with architecture, build instructions, CI badge` | `README.md` | `./gradlew clean assembleDebug assembleRelease testDebugUnitTest lint` |

---

## Success Criteria

### Verification Commands
```bash
# Build succeeds (debug + release)
./gradlew clean assembleDebug assembleRelease

# All tests pass
./gradlew testDebugUnitTest

# Lint passes
./gradlew lintDebug

# Default branch is main
gh api repos/SynthNoirLabs/AgentConsole --jq '.default_branch'
# Expected: main

# No global mutable singletons
grep -r "^object.*{" app/src/main/java/ --include="*.kt" | grep -v "Agent\|companion"
# Expected: no matches (or only Agent enum)

# Hilt setup present
grep -r "@HiltAndroidApp" app/src/main/java/ --include="*.kt"
# Expected: 1 match

# CI workflow present
test -f .github/workflows/android.yml && echo "exists"
# Expected: exists

# Room database present
grep -r "@Database" app/src/main/java/ --include="*.kt"
# Expected: 1 match

# Navigation present
grep -r "NavHost" app/src/main/java/ --include="*.kt"
# Expected: 1+ matches
```

### Final Checklist
- [ ] All "Must Have" present
- [ ] All "Must NOT Have" absent
- [ ] All tests pass
- [ ] Release APK generates
- [ ] README reflects actual architecture
