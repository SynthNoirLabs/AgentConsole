package com.example.agentconsole.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "execution_history")
data class ExecutionHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val agent: String,
    val workingDir: String,
    val prompt: String,
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val status: String,
    val timestamp: Long
) {
    companion object {
        const val MAX_PROMPT_CHARS = 500
        const val MAX_STDOUT_CHARS = 10 * 1024
        const val MAX_STDERR_CHARS = 5 * 1024

        fun fromExecution(
            agent: String,
            workingDir: String,
            prompt: String,
            stdout: String,
            stderr: String,
            exitCode: Int,
            status: String,
            timestamp: Long
        ): ExecutionHistory {
            return ExecutionHistory(
                agent = agent,
                workingDir = workingDir,
                prompt = prompt.take(MAX_PROMPT_CHARS),
                stdout = stdout.take(MAX_STDOUT_CHARS),
                stderr = stderr.take(MAX_STDERR_CHARS),
                exitCode = exitCode,
                status = status,
                timestamp = timestamp
            )
        }
    }
}
