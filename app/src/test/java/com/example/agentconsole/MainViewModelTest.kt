package com.example.agentconsole

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(
            appContext = null as android.content.Context,
            repository = TermuxRepository()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `markRunning sets running state`() = runTest {
        viewModel.markRunning(1, "Claude Code", "~/repo")
        val state = viewModel.uiState.first()

        assertTrue(state.isRunning)
        assertEquals("Running", state.status)
        assertEquals("Claude Code", state.activeAgent)
        assertEquals("~/repo", state.workingDir)
        assertEquals(1, state.lastExecutionId)
    }

    @Test
    fun `publishResult updates state for matching execution ID`() = runTest {
        viewModel.markRunning(42, "Claude Code", "~/repo")
        viewModel.publishResult(
            executionId = 42,
            stdout = "hello",
            stderr = "",
            exitCode = 0,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        val state = viewModel.uiState.first()

        assertFalse(state.isRunning)
        assertEquals("Finished", state.status)
        assertEquals("hello", state.stdout)
    }

    @Test
    fun `publishResult discards stale execution ID`() = runTest {
        viewModel.markRunning(100, "Claude Code", "~/repo")
        viewModel.publishResult(
            executionId = 99, // stale
            stdout = "stale output",
            stderr = "",
            exitCode = 0,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        val state = viewModel.uiState.first()

        // Should still be running — stale result was discarded
        assertTrue(state.isRunning)
        assertEquals("Running", state.status)
        assertEquals("", state.stdout)
    }

    @Test
    fun `publishResult marks errors correctly`() = runTest {
        viewModel.markRunning(10, "Gemini CLI", "~/repo")
        viewModel.publishResult(
            executionId = 10,
            stdout = "",
            stderr = "error occurred",
            exitCode = 1,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        val state = viewModel.uiState.first()

        assertFalse(state.isRunning)
        assertEquals("Finished with errors", state.status)
        assertEquals("error occurred", state.stderr)
    }

    @Test
    fun `fail sets failed state`() = runTest {
        viewModel.markRunning(5, "Claude Code", "~/repo")
        viewModel.fail("something broke")
        val state = viewModel.uiState.first()

        assertFalse(state.isRunning)
        assertEquals("Failed", state.status)
        assertEquals("something broke", state.stderr)
    }
}
