# Contributing to Agent Console

Thank you for your interest in contributing! This guide covers how to build, test, and submit changes.

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with API 26+ installed
- Git

## Building from Source

### 1. Clone the repository

```bash
git clone https://github.com/SynthNoirLabs/AgentConsole.git
cd AgentConsole
```

### 2. Open in Android Studio

File → Open → select the `AgentConsole` directory. Android Studio will sync Gradle automatically.

### 3. Build via command line

```bash
# Debug APK
./gradlew assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Running Tests

```bash
# Unit tests
./gradlew testDebugUnitTest

# Lint checks
./gradlew lintDebug

# All checks (recommended before opening a PR)
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Test reports are written to:
- `app/build/reports/tests/testDebugUnitTest/`
- `app/build/reports/lint-results-debug.html`

## Project Structure

```
app/src/main/java/com/example/agentconsole/
  Agent.kt              # Agent enum (Claude, Gemini, Codex, OpenCode)
  ExecutionStore.kt     # Reactive state holder (StateFlow)
  MainActivity.kt       # Compose UI entry point
  TermuxResultService.kt # Receives results via broadcast from Termux
  TermuxRunner.kt       # Sends RUN_COMMAND intents to Termux
scripts/
  agent_runner.sh       # Shell helper dispatched by Termux
```

## Code Style

- **Kotlin** — follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- **Jetpack Compose** — prefer stateless composables, hoist state up
- **No magic strings** — use constants or enums
- Run `./gradlew lintDebug` before pushing; zero new warnings expected

## Submitting a Pull Request

1. Fork the repository and create a feature branch from `main`
2. Make your changes with focused, atomic commits
3. Run `./gradlew assembleDebug testDebugUnitTest lintDebug` — all must pass
4. Open a PR against `main` using the pull request template
5. Describe what you changed and why; link any related issues

## Reporting Bugs / Requesting Features

Use the GitHub issue templates:
- **Bug report** — reproducible defects with steps to reproduce
- **Feature request** — new functionality with a clear use case

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).
