package com.example.agentconsole

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class ResultBus @Inject constructor() {

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
        emit(Event.Running(executionId, agent, workingDir))
    }

    fun publishResult(
        executionId: Int,
        stdout: String,
        stderr: String,
        exitCode: Int,
        internalErrorCode: Int,
        internalErrorMessage: String
    ) {
        emit(
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
        emit(Event.Failed(message))
    }

    private fun emit(event: Event) {
        if (!_events.tryEmit(event)) {
            Log.w(TAG, "Dropped ResultBus event with no active collector: ${event::class.simpleName}")
        }
    }

    companion object {
        private const val TAG = "ResultBus"
    }
}
