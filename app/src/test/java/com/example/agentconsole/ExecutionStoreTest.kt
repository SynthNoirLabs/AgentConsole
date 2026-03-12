package com.example.agentconsole

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExecutionStoreTest {

    @Before
    fun resetState() {
        // Reset to idle by publishing a fail then re-checking
        ExecutionStore.fail("")
    }

    @Test
    fun `markRunning sets running state`() = runTest {
        ExecutionStore.markRunning(1, "Claude Code", "~/repo")
        val state = ExecutionStore.state.first()

        assertTrue(state.isRunning)
        assertEquals("Running", state.status)
        assertEquals("Claude Code", state.activeAgent)
        assertEquals("~/repo", state.workingDir)
        assertEquals(1, state.lastExecutionId)
    }

    @Test
    fun `publishResult updates state for matching execution ID`() = runTest {
        ExecutionStore.markRunning(42, "Claude Code", "~/repo")
        ExecutionStore.publishResult(
            executionId = 42,
            stdout = "hello",
            stderr = "",
            exitCode = 0,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        val state = ExecutionStore.state.first()

        assertFalse(state.isRunning)
        assertEquals("Finished", state.status)
        assertEquals("hello", state.stdout)
    }

    @Test
    fun `publishResult discards stale execution ID`() = runTest {
        ExecutionStore.markRunning(100, "Claude Code", "~/repo")
        ExecutionStore.publishResult(
            executionId = 99, // stale
            stdout = "stale output",
            stderr = "",
            exitCode = 0,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        val state = ExecutionStore.state.first()

        // Should still be running — stale result was discarded
        assertTrue(state.isRunning)
        assertEquals("Running", state.status)
        assertEquals("", state.stdout)
    }

    @Test
    fun `publishResult marks errors correctly`() = runTest {
        ExecutionStore.markRunning(10, "Gemini CLI", "~/repo")
        ExecutionStore.publishResult(
            executionId = 10,
            stdout = "",
            stderr = "error occurred",
            exitCode = 1,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        val state = ExecutionStore.state.first()

        assertFalse(state.isRunning)
        assertEquals("Finished with errors", state.status)
        assertEquals("error occurred", state.stderr)
    }

    @Test
    fun `fail sets failed state`() = runTest {
        ExecutionStore.markRunning(5, "Claude Code", "~/repo")
        ExecutionStore.fail("something broke")
        val state = ExecutionStore.state.first()

        assertFalse(state.isRunning)
        assertEquals("Failed", state.status)
        assertEquals("something broke", state.stderr)
    }
}
