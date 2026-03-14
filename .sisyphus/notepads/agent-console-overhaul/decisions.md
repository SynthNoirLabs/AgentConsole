
## Task 2 - Build Foundation (2026-03-13)
- Added `gradle.properties` with `android.useAndroidX=true` to satisfy AGP AndroidX checks during dependency resolution.
- Kept the required theme parent `Theme.Material3.DayNight.NoActionBar` and added `com.google.android.material:material` to support XML style resolution.
- Preserved unrelated untracked Hilt prototype files by temporarily stashing them only for verification runs, then restoring them unchanged.

## Task 3c - MainViewModel + ExecutionStore deletion (2026-03-13)
- Implemented a `ResultBus` event object for Repository/Service -> ViewModel communication so `TermuxRepository` no longer depends directly on `ExecutionStore`.
- Kept a temporary compatibility `ExecutionStore` object in `ResultBus.kt` because `MainActivity` and `TermuxRunner` are explicitly deferred to task 3d.
