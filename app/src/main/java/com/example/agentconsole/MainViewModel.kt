package com.example.agentconsole

import android.content.Context
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agentconsole.data.ExecutionHistory
import com.example.agentconsole.data.ExecutionHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isRunning: Boolean = false,
    val historyError: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: TermuxRepository,
    private val executionHistoryDao: ExecutionHistoryDao,
    private val resultBus: ResultBus,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreInitialState())
    val uiState: StateFlow<ExecutionUiState> = _uiState.asStateFlow()

    private var lastPrompt: String
        get() = savedStateHandle.get<String>(KEY_LAST_PROMPT).orEmpty()
        set(value) { savedStateHandle[KEY_LAST_PROMPT] = value }

    init {
        viewModelScope.launch {
            resultBus.events.collect { event ->
                when (event) {
                    is ResultBus.Event.Running -> markRunning(
                        event.executionId,
                        event.agent,
                        event.workingDir
                    )

                    is ResultBus.Event.Result -> publishResult(
                        executionId = event.executionId,
                        stdout = event.stdout,
                        stderr = event.stderr,
                        exitCode = event.exitCode,
                        internalErrorCode = event.internalErrorCode,
                        internalErrorMessage = event.internalErrorMessage
                    )

                    is ResultBus.Event.Failed -> fail(event.message)
                }
            }
        }
    }

    fun validateWorkingDir(workingDir: String): String? = repository.validateWorkingDir(workingDir)

    fun validatePrompt(prompt: String): String? = repository.validatePrompt(prompt)

    fun isTermuxInstalled(): Boolean = repository.isTermuxInstalled(appContext)

    fun openTermux(context: Context) = repository.openTermux(context)

    fun run(agent: Agent, prompt: String, workingDir: String) {
        // Persist only inputs that pass validation so a restored process never
        // resurfaces an invalid prompt. The repository re-validates and emits
        // Failed via the bus; this is intentional defense-in-depth.
        if (repository.validatePrompt(prompt) == null &&
            repository.validateWorkingDir(workingDir) == null
        ) {
            lastPrompt = prompt
        }
        repository.runAgent(
            context = appContext,
            agent = agent,
            prompt = prompt,
            workingDir = workingDir
        )
    }

    fun dismissHistoryError() {
        _uiState.value = _uiState.value.copy(historyError = null)
    }

    @VisibleForTesting
    internal fun markRunning(executionId: Int, agent: String, workingDir: String) {
        Log.d(TAG, "markRunning: executionId=$executionId, agent=$agent, workdir=$workingDir")
        savedStateHandle[KEY_LAST_EXECUTION_ID] = executionId
        savedStateHandle[KEY_ACTIVE_AGENT] = agent
        savedStateHandle[KEY_ACTIVE_WORKDIR] = workingDir
        _uiState.value = ExecutionUiState(
            status = "Running",
            activeAgent = agent,
            workingDir = workingDir,
            lastExecutionId = executionId,
            isRunning = true
        )
    }

    @VisibleForTesting
    internal fun publishResult(
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
        val status = if (exitCode == 0 && internalErrorCode == -1) "Finished" else "Finished with errors"
        // Live UI uses a generous byte-bounded truncation (50 KB) so the user
        // sees as much output as is practical for the current run; history
        // applies a smaller char-bounded truncation in ExecutionHistory.fromExecution
        // because the row is stored long-term. Both are marked when trimmed.
        val truncatedStdout = TermuxRepository.truncateOutput(stdout, "stdout")
        val truncatedStderr = TermuxRepository.truncateOutput(stderr, "stderr")
        clearInFlightState()
        _uiState.value = current.copy(
            status = status,
            stdout = truncatedStdout,
            stderr = truncatedStderr,
            exitCode = exitCode,
            internalErrorCode = internalErrorCode,
            internalErrorMessage = internalErrorMessage,
            isRunning = false
        )

        viewModelScope.launch {
            try {
                executionHistoryDao.insert(
                    ExecutionHistory.fromExecution(
                        agent = current.activeAgent,
                        workingDir = current.workingDir,
                        prompt = lastPrompt,
                        stdout = stdout,
                        stderr = stderr,
                        exitCode = exitCode,
                        status = status,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save execution history for execution #$executionId", e)
                _uiState.value = _uiState.value.copy(
                    historyError = "Could not save execution to history: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }

    @VisibleForTesting
    internal fun fail(message: String) {
        Log.w(TAG, "fail: $message")
        clearInFlightState()
        _uiState.value = _uiState.value.copy(
            status = "Failed",
            stderr = message,
            isRunning = false
        )
    }

    private fun clearInFlightState() {
        savedStateHandle.remove<Int>(KEY_LAST_EXECUTION_ID)
        savedStateHandle.remove<String>(KEY_ACTIVE_AGENT)
        savedStateHandle.remove<String>(KEY_ACTIVE_WORKDIR)
    }

    private fun restoreInitialState(): ExecutionUiState {
        val executionId = savedStateHandle.get<Int>(KEY_LAST_EXECUTION_ID) ?: return ExecutionUiState()
        val agent = savedStateHandle.get<String>(KEY_ACTIVE_AGENT).orEmpty()
        val workingDir = savedStateHandle.get<String>(KEY_ACTIVE_WORKDIR).orEmpty()
        Log.d(TAG, "Restoring in-flight execution #$executionId after process recreation")
        return ExecutionUiState(
            status = "Running",
            activeAgent = agent,
            workingDir = workingDir,
            lastExecutionId = executionId,
            isRunning = true
        )
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val KEY_LAST_EXECUTION_ID = "last_execution_id"
        private const val KEY_ACTIVE_AGENT = "active_agent"
        private const val KEY_ACTIVE_WORKDIR = "active_workdir"
        private const val KEY_LAST_PROMPT = "last_prompt"
    }
}
