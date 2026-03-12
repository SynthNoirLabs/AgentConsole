package com.example.agentconsole

import android.util.Log
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
    private const val TAG = "ExecutionStore"

    private val _state = MutableStateFlow(ExecutionUiState())
    val state: StateFlow<ExecutionUiState> = _state.asStateFlow()

    fun markRunning(executionId: Int, agent: String, workingDir: String) {
        Log.d(TAG, "markRunning: executionId=$executionId, agent=$agent, workdir=$workingDir")
        _state.value = ExecutionUiState(
            status = "Running",
            activeAgent = agent,
            workingDir = workingDir,
            lastExecutionId = executionId,
            isRunning = true
        )
    }

    /**
     * Publishes the result for the given [executionId].
     * If the execution ID does not match the current running execution, the result is discarded
     * to prevent stale results from overwriting newer state.
     */
    fun publishResult(
        executionId: Int,
        stdout: String,
        stderr: String,
        exitCode: Int,
        internalErrorCode: Int,
        internalErrorMessage: String
    ) {
        val current = _state.value
        if (current.lastExecutionId != null && current.lastExecutionId != executionId) {
            Log.w(TAG, "Discarding stale result for execution #$executionId " +
                "(current=#${current.lastExecutionId})")
            return
        }

        Log.d(TAG, "publishResult: executionId=$executionId, exitCode=$exitCode")
        _state.value = current.copy(
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
        Log.e(TAG, "fail: $message")
        _state.value = _state.value.copy(
            status = "Failed",
            stderr = message,
            isRunning = false
        )
    }
}
