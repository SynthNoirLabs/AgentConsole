package com.example.agentconsole

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.agentconsole.data.ExecutionHistory
import com.example.agentconsole.data.ExecutionHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel
    private lateinit var fakeExecutionHistoryDao: FakeExecutionHistoryDao

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeExecutionHistoryDao = FakeExecutionHistoryDao()
        viewModel = MainViewModel(
            appContext = ApplicationProvider.getApplicationContext<Context>(),
            repository = TermuxRepository(),
            executionHistoryDao = fakeExecutionHistoryDao
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
        viewModel.run(Agent.CLAUDE, "summarize", "relative/path")
        viewModel.markRunning(42, "Claude Code", "~/repo")
        viewModel.publishResult(
            executionId = 42,
            stdout = "hello",
            stderr = "",
            exitCode = 0,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        assertFalse(state.isRunning)
        assertEquals("Finished", state.status)
        assertEquals("hello", state.stdout)
    }

    @Test
    fun `publishResult discards stale execution ID`() = runTest {
        viewModel.run(Agent.CLAUDE, "summarize", "relative/path")
        viewModel.markRunning(100, "Claude Code", "~/repo")
        viewModel.publishResult(
            executionId = 99, // stale
            stdout = "stale output",
            stderr = "",
            exitCode = 0,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        // Should still be running — stale result was discarded
        assertTrue(state.isRunning)
        assertEquals("Running", state.status)
        assertEquals("", state.stdout)
        assertEquals(0, fakeExecutionHistoryDao.entries.size)
    }

    @Test
    fun `publishResult marks errors correctly`() = runTest {
        viewModel.run(Agent.GEMINI, "diagnose", "relative/path")
        viewModel.markRunning(10, "Gemini CLI", "~/repo")
        viewModel.publishResult(
            executionId = 10,
            stdout = "",
            stderr = "error occurred",
            exitCode = 1,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        assertFalse(state.isRunning)
        assertEquals("Finished with errors", state.status)
        assertEquals("error occurred", state.stderr)
    }

    @Test
    fun `publishResult persists execution history`() = runTest {
        viewModel.run(Agent.OPENCODE, "persist me", "relative/path")
        viewModel.markRunning(22, "OpenCode", "~/repo")

        viewModel.publishResult(
            executionId = 22,
            stdout = "stdout",
            stderr = "stderr",
            exitCode = 0,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )

        advanceUntilIdle()

        assertEquals(1, fakeExecutionHistoryDao.entries.size)
        val inserted = fakeExecutionHistoryDao.entries.first()
        assertEquals("OpenCode", inserted.agent)
        assertEquals("~/repo", inserted.workingDir)
        assertEquals("persist me", inserted.prompt)
        assertEquals("stdout", inserted.stdout)
        assertEquals("stderr", inserted.stderr)
        assertEquals(0, inserted.exitCode)
        assertEquals("Finished", inserted.status)
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

    private class FakeExecutionHistoryDao : ExecutionHistoryDao {
        val entries = mutableListOf<ExecutionHistory>()

        override suspend fun insert(entry: ExecutionHistory) {
            entries += entry.copy(id = (entries.size + 1).toLong())
        }

        override fun getAll(): Flow<List<ExecutionHistory>> {
            return flowOf(entries.toList())
        }

        override suspend fun deleteOlderThan(cutoff: Long) {
            entries.removeAll { it.timestamp < cutoff }
        }
    }
}
