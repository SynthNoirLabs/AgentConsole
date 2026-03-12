package com.example.agentconsole

enum class Agent(val cliName: String, val displayName: String) {
    CLAUDE("claude", "Claude Code"),
    GEMINI("gemini", "Gemini CLI"),
    CODEX("codex", "Codex CLI"),
    OPENCODE("opencode", "OpenCode")
}
