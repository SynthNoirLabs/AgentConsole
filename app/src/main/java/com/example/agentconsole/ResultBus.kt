package com.example.agentconsole

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

object ResultBus {
    sealed interface Event {
        data class Running(val executionId: Int, val agent: String, val workingDir: String) : Event

        data class Result(
            val executionId: Int,
            val stdout: String,
            val stderr: String,
            val exitCode: Int,
            val internalErrorCode: Int,
            val internalErrorMessage: String
        ) : Event

        data class Failed(val message: String) : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun markRunning(executionId: Int, agent: String, workingDir: String) {
        _events.tryEmit(Event.Running(executionId, agent, workingDir))
    }

    fun publishResult(
        executionId: Int,
        stdout: String,
        stderr: String,
        exitCode: Int,
        internalErrorCode: Int,
        internalErrorMessage: String
    ) {
        _events.tryEmit(
            Event.Result(
                executionId = executionId,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                internalErrorCode = internalErrorCode,
                internalErrorMessage = internalErrorMessage
            )
        )
    }

    fun fail(message: String) {
        _events.tryEmit(Event.Failed(message))
    }
}

