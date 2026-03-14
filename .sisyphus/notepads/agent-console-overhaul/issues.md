
## Task 2 - Build Foundation (2026-03-13)
- Local system `java` runtime was unavailable via `/usr/bin/java`; Gradle wrapper commands required explicit `JAVA_HOME` from mise-managed JDK.
- `kotlin-lsp` installation succeeded, but LSP initialization for `.kts` files still timed out in this environment.
- No XML LSP server is configured in the current OpenCode setup, so XML diagnostics could not be produced via `lsp_diagnostics`.

## Task 3c - MainViewModel + ExecutionStore deletion (2026-03-13)
- `lsp_diagnostics` still times out on Kotlin files (`initialize` timeout), so LSP clean checks could not be completed.
- `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` both fail in this shell with "Unable to locate a Java Runtime" from macOS stub Java.
