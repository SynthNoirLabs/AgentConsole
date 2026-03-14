package com.example.agentconsole

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: TermuxRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExecutionUiState())
    val uiState: StateFlow<ExecutionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ResultBus.events.collect { event ->
                when (event) {
                    is ResultBus.Event.Running -> {
                        markRunning(event.executionId, event.agent, event.workingDir)
                    }

                    is ResultBus.Event.Result -> {
                        publishResult(
                            executionId = event.executionId,
                            stdout = event.stdout,
                            stderr = event.stderr,
                            exitCode = event.exitCode,
                            internalErrorCode = event.internalErrorCode,
                            internalErrorMessage = event.internalErrorMessage
                        )
                    }

                    is ResultBus.Event.Failed -> {
                        fail(event.message)
                    }
                }
            }
        }
    }

    fun run(agent: Agent, prompt: String, workingDir: String) {
        if (prompt.isBlank()) {
            fail("Prompt is empty.")
            return
        }

        val workdirError = repository.validateWorkingDir(workingDir)
        if (workdirError != null) {
            fail(workdirError)
            return
        }

        repository.runAgent(
            context = appContext,
            agent = agent,
            prompt = prompt,
            workingDir = workingDir
        )
    }

    fun checkTermuxInstalled(context: Context): Boolean {
        return repository.isTermuxInstalled(context)
    }

    fun markRunning(executionId: Int, agent: String, workingDir: String) {
        Log.d(TAG, "markRunning: executionId=$executionId, agent=$agent, workdir=$workingDir")
        _uiState.value = ExecutionUiState(
            status = "Running",
            activeAgent = agent,
            workingDir = workingDir,
            lastExecutionId = executionId,
            isRunning = true
        )
    }

    fun publishResult(
        executionId: Int,
        stdout: String,
        stderr: String,
        exitCode: Int,
        internalErrorCode: Int,
        internalErrorMessage: String
    ) {
        val current = _uiState.value
        if (current.lastExecutionId != null && current.lastExecutionId != executionId) {
            Log.w(
                TAG,
                "Discarding stale result for execution #$executionId " +
                    "(current=#${current.lastExecutionId})"
            )
            return
        }

        Log.d(TAG, "publishResult: executionId=$executionId, exitCode=$exitCode")
        _uiState.value = current.copy(
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
        _uiState.value = _uiState.value.copy(
            status = "Failed",
            stderr = message,
            isRunning = false
        )
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}
