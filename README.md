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

- Package name: `com.synthnoirlabs.agentconsole`
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

## Screenshots

> Screenshots are not yet captured. Pull requests adding `docs/screenshots/main.png` and `docs/screenshots/history.png` are welcome.

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| Run button does nothing, status stays "Idle" | Termux is not installed | Install Termux from F-Droid (the Play Store build is outdated and incompatible) |
| Status shows "Failed: Run commands in Termux permission missing" | App was never granted `RUN_COMMAND` | Settings → Apps → Agent Console → Permissions → enable **Run commands in Termux environment** |
| Status shows "Failed" but Termux is installed and permission granted | `allow-external-apps` is `false` in Termux | Inside Termux: `mkdir -p ~/.termux && printf 'allow-external-apps=true\n' >> ~/.termux/termux.properties && termux-reload-settings` |
| Output is empty, exit code 127 | `~/bin/agent_runner.sh` is missing or not executable | Re-run the helper script install steps in the Termux setup section |
| Output is empty, exit code 127 mentioning the agent CLI | The selected agent's CLI is not installed in Termux | `npm install -g @anthropic-ai/claude-code` (or the corresponding install for your agent) |
| App killed mid-run, no result delivered | Battery optimization is on | Settings → Apps → Agent Console → Battery → Unrestricted |
| Output truncated with `[...truncated — N bytes]` | Output exceeded 50 KB | Pipe the agent's output to a file inside Termux instead, or wait for the streaming feature |

## Releases

Tagged commits matching `v*` (for example `v1.0.0`) trigger `.github/workflows/release.yml`, which builds a signed APK + AAB and publishes them as a GitHub Release. The workflow requires four repository secrets:

- `RELEASE_KEYSTORE_BASE64` — `base64 -w0 release.keystore`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Local release builds work the same way: export the four `RELEASE_*` env vars (with `RELEASE_KEYSTORE_PATH` instead of the base64) and run `./gradlew assembleRelease`. Without those env vars, the release build is unsigned (debug builds always work without configuration).

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
    java/com/synthnoirlabs/agentconsole/
      AgentConsoleApplication.kt   # @HiltAndroidApp entry point
      MainActivity.kt              # Single Compose activity
      MainViewModel.kt             # StateFlow-based ViewModel
      HistoryViewModel.kt          # ViewModel for history screen
      TermuxRepository.kt          # Sends RUN_COMMAND intents + validation
      TermuxResultService.kt       # Receives broadcast results from Termux
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
