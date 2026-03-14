# Agent Console

[![Android CI](https://github.com/SynthNoirLabs/AgentConsole/actions/workflows/android.yml/badge.svg)](https://github.com/SynthNoirLabs/AgentConsole/actions/workflows/android.yml)

Android app starter for running AI coding agents from a native Jetpack Compose UI via Termux.

```
Jetpack Compose app
    |
Termux RUN_COMMAND
    |
~/bin/agent_runner.sh
    |
Claude / Gemini / Codex / OpenCode CLI
```

## Supported Agents

| Agent | CLI |
|-------|-----|
| Claude Code | `claude` |
| Gemini CLI | `gemini` |
| Codex CLI | `codex` |
| OpenCode | `opencode` |

## Requirements

- Android 8.0+ (API 26)
- [Termux](https://f-droid.org/packages/com.termux/) installed
- At least one CLI agent installed inside Termux

## Build from Source

```bash
# Clone
git clone https://github.com/SynthNoirLabs/AgentConsole.git
cd AgentConsole

# Build debug APK (requires JDK 17)
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Lint check
./gradlew lintDebug
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`

See [CONTRIBUTING.md](CONTRIBUTING.md) for full setup instructions.

## Quick Start

### 1. Open in Android Studio

Create a new **Empty Activity** project or clone this repo directly:

- Package name: `com.example.agentconsole`
- Minimum SDK: 26+

### 2. Termux Setup

```bash
# Enable external app access
mkdir -p ~/.termux
printf 'allow-external-apps=true\n' >> ~/.termux/termux.properties
termux-reload-settings

# Install tools
pkg update && pkg install git nodejs python

# Install your preferred agent(s)
npm install -g @anthropic-ai/claude-code
npm install -g @google/gemini-cli
npm install -g opencode
pip install openai

# Install the helper script
mkdir -p ~/bin
cp scripts/agent_runner.sh ~/bin/agent_runner.sh
chmod +x ~/bin/agent_runner.sh
```

### 3. Android Permissions

1. Open **Android Settings > Apps > Agent Console > Permissions**
2. Grant **Run commands in Termux environment**

### 4. Run

- Select an agent from the dropdown
- Set the working directory to your repo path
- Enter a prompt
- Tap **Run**

stdout/stderr will appear in the app once the command completes.

## Architecture

Agent Console follows **MVVM** with unidirectional data flow:

- **Hilt** — dependency injection via `@HiltAndroidApp` / `@HiltViewModel`
- **ViewModel + StateFlow** — `MainViewModel` owns all UI state; no logic in Composables
- **Room** — local persistence for execution history (`AppDatabase`, DAOs)
- **Compose Navigation** — single-activity, multi-screen nav via `NavGraph.kt`
- **ResultBus** — `SharedFlow` bridge that forwards Termux broadcast results from `TermuxResultService` into the ViewModel

## File Structure

```
app/
  src/main/
    AndroidManifest.xml
    java/com/example/agentconsole/
      AgentConsoleApplication.kt   # @HiltAndroidApp entry point
      MainActivity.kt              # Single Compose activity
      MainViewModel.kt             # StateFlow-based ViewModel
      TermuxRunner.kt              # Sends RUN_COMMAND intents to Termux
      TermuxResultService.kt       # Receives broadcast results from Termux
      TermuxRepository.kt          # Injectable repository layer
      ResultBus.kt                 # Service → ViewModel SharedFlow bridge
      Agent.kt                     # Agent enum (Claude, Gemini, Codex, OpenCode)
      di/
        AppModule.kt               # Hilt module – binds repository & DB
      data/
        ExecutionHistory.kt        # Room entity
        ExecutionHistoryDao.kt     # DAO interface
        AppDatabase.kt             # Room database
      ui/
        theme/
          Theme.kt                 # Material3 light/dark theming
        navigation/
          NavGraph.kt              # Compose Navigation graph
        history/
          HistoryScreen.kt         # Execution history screen
          HistoryViewModel.kt      # ViewModel for history screen
scripts/
  agent_runner.sh                  # Termux helper that dispatches to the right CLI
settings.gradle.kts
app/build.gradle.kts
```

## Status

| Feature | State |
|---------|-------|
| Execution history screen | ✅ Done |
| Theme / dark mode support | ✅ Done |
| Directory picker with SAF | ✅ Done |
| Output truncation and validation | ✅ Done |
| CI/CD pipeline | ✅ Done |
| Streaming output via Termux sessions | ⬜ Future |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup instructions, code style, and PR guidelines.

## License

MIT
