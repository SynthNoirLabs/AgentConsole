
## Task 2 - Build Foundation (2026-03-13)
- OpenCode `lsp_diagnostics` is not currently reliable for Kotlin Gradle scripts in this environment (`kotlin-lsp` initialize timeout), blocking clean LSP verification for `.kts` files.
- Existing untracked Hilt files (`AgentConsoleApplication.kt`, `di/AppModule.kt`) are outside this task scope but can break builds unless either Hilt dependencies are wired or files are excluded/stashed.

## Task 3c - MainViewModel + ExecutionStore deletion (2026-03-13)
- Kotlin LSP diagnostics remain unavailable (`initialize` timeout), so required LSP cleanliness cannot be machine-verified for changed files.
- Gradle build/test verification is blocked in this environment due missing configured JDK (`/usr/bin/java` reports no runtime).
