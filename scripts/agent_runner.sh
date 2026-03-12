#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

agent="${1:-}"
prompt="${2:-}"

if [ -z "$agent" ]; then
  echo "Usage: agent_runner.sh <agent> <prompt>" >&2
  exit 1
fi

if [ -z "$prompt" ]; then
  echo "Error: prompt must not be empty" >&2
  exit 1
fi

case "$agent" in
  claude)
    exec claude -p -- "$prompt"
    ;;
  gemini)
    exec gemini -p -- "$prompt"
    ;;
  codex)
    exec codex exec -- "$prompt"
    ;;
  opencode)
    exec opencode run -- "$prompt"
    ;;
  *)
    echo "Unknown agent: $agent" >&2
    exit 64
    ;;
esac
