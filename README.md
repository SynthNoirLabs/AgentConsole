# Very early WIP Project, expect nothing - Agent Console

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

## File Structure

```
app/
  src/main/
    AndroidManifest.xml
    java/com/example/agentconsole/
      Agent.kt              # Agent enum (Claude, Gemini, Codex, OpenCode)
      ExecutionStore.kt     # Reactive state holder (StateFlow)
      MainActivity.kt       # Compose UI
      TermuxResultService.kt # Receives results from Termux
      TermuxRunner.kt       # Sends RUN_COMMAND intents to Termux
scripts/
  agent_runner.sh           # Termux helper that dispatches to the right CLI
settings.gradle.kts
app/build.gradle.kts
```

## What to Add Next

- Repo picker with SAF / DocumentFile
- Execution history screen
- Streaming output via Termux sessions
- Theme / dark mode support

## License

MIT
