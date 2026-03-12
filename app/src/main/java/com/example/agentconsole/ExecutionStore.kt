package com.example.agentconsole

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExecutionUiState(
    val status: String = "Idle",
    val activeAgent: String = "",
    val workingDir: String = "",
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int? = null,
    val internalErrorCode: Int? = null,
    val internalErrorMessage: String = "",
    val lastExecutionId: Int? = null,
    val isRunning: Boolean = false
)

object ExecutionStore {
    private val _state = MutableStateFlow(ExecutionUiState())
    val state: StateFlow<ExecutionUiState> = _state.asStateFlow()

    fun markRunning(executionId: Int, agent: String, workingDir: String) {
        _state.value = ExecutionUiState(
            status = "Running",
            activeAgent = agent,
            workingDir = workingDir,
            lastExecutionId = executionId,
            isRunning = true
        )
    }

    fun publishResult(
        stdout: String,
        stderr: String,
        exitCode: Int,
        internalErrorCode: Int,
        internalErrorMessage: String
    ) {
        _state.value = _state.value.copy(
            status = if (exitCode == 0 && internalErrorCode == -1) "Finished" else "Finished with errors",
            stdout = stdout,
            stderr = stderr,
            exitCode = exitCode,
            internalErrorCode = internalErrorCode,
            internalErrorMessage = internalErrorMessage,
            isRunning = false
        )
    }

    fun fail(message: String) {
        _state.value = _state.value.copy(
            status = "Failed",
            stderr = message,
            isRunning = false
        )
    }
}
