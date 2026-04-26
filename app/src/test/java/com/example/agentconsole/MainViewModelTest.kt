package com.example.agentconsole

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.example.agentconsole.data.ExecutionHistory
import com.example.agentconsole.data.ExecutionHistoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel
    private lateinit var fakeExecutionHistoryDao: FakeExecutionHistoryDao
    private lateinit var resultBus: ResultBus
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeExecutionHistoryDao = FakeExecutionHistoryDao()
        resultBus = ResultBus()
        savedStateHandle = SavedStateHandle()
        viewModel = MainViewModel(
            appContext = ApplicationProvider.getApplicationContext<Context>(),
            repository = TermuxRepository(resultBus),
            executionHistoryDao = fakeExecutionHistoryDao,
            resultBus = resultBus,
            savedStateHandle = savedStateHandle
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
        savedStateHandle["last_prompt"] = "summarize"
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

        assertTrue(state.isRunning)
        assertEquals("Running", state.status)
        assertEquals("", state.stdout)
        assertEquals(0, fakeExecutionHistoryDao.entries.size)
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
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        assertFalse(state.isRunning)
        assertEquals("Finished with errors", state.status)
        assertEquals("error occurred", state.stderr)
    }

    @Test
    fun `publishResult persists execution history`() = runTest {
        savedStateHandle["last_prompt"] = "persist me"
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

    @Test
    fun `historyError surfaces when DAO insert fails and dismiss clears it`() = runTest {
        fakeExecutionHistoryDao.shouldFailOnInsert = true
        savedStateHandle["last_prompt"] = "boom"
        viewModel.markRunning(7, "Claude Code", "~/repo")
        viewModel.publishResult(
            executionId = 7,
            stdout = "ok",
            stderr = "",
            exitCode = 0,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.first().historyError)

        viewModel.dismissHistoryError()
        assertNull(viewModel.uiState.first().historyError)
    }

    @Test
    fun `bus markRunning event drives running state`() = runTest {
        resultBus.markRunning(11, "Claude Code", "~/repo")
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        assertTrue(state.isRunning)
        assertEquals(11, state.lastExecutionId)
        assertEquals("Claude Code", state.activeAgent)
    }

    @Test
    fun `bus result event drives finished state`() = runTest {
        savedStateHandle["last_prompt"] = "via bus"
        resultBus.markRunning(12, "Claude Code", "~/repo")
        resultBus.publishResult(
            executionId = 12,
            stdout = "out",
            stderr = "",
            exitCode = 0,
            internalErrorCode = -1,
            internalErrorMessage = ""
        )
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        assertFalse(state.isRunning)
        assertEquals("Finished", state.status)
        assertEquals("out", state.stdout)
        assertEquals(1, fakeExecutionHistoryDao.entries.size)
        assertEquals("via bus", fakeExecutionHistoryDao.entries.first().prompt)
    }

    @Test
    fun `bus failed event drives failed state`() = runTest {
        resultBus.fail("nope")
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        assertFalse(state.isRunning)
        assertEquals("Failed", state.status)
        assertEquals("nope", state.stderr)
    }

    @Test
    fun `run rejects invalid workdir via bus`() = runTest {
        viewModel.run(Agent.CLAUDE, "ok prompt", "relative/path")
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        assertEquals("Failed", state.status)
        assertTrue(state.stderr.contains("absolute path"))
    }

    @Test
    fun `run rejects empty prompt via bus`() = runTest {
        viewModel.run(Agent.CLAUDE, "", "/data/projects/repo")
        advanceUntilIdle()
        val state = viewModel.uiState.first()

        assertEquals("Failed", state.status)
        assertTrue(state.stderr.contains("Prompt"))
    }

    @Test
    fun `restored SavedStateHandle resumes running state on cold start`() = runTest {
        val restored = SavedStateHandle(
            mapOf(
                "last_execution_id" to 77,
                "active_agent" to "Codex CLI",
                "active_workdir" to "~/proj",
                "last_prompt" to "old prompt"
            )
        )
        val recoveredVm = MainViewModel(
            appContext = ApplicationProvider.getApplicationContext<Context>(),
            repository = TermuxRepository(resultBus),
            executionHistoryDao = fakeExecutionHistoryDao,
            resultBus = resultBus,
            savedStateHandle = restored
        )
        val state = recoveredVm.uiState.first()

        assertTrue(state.isRunning)
        assertEquals(77, state.lastExecutionId)
        assertEquals("Codex CLI", state.activeAgent)
        assertEquals("~/proj", state.workingDir)
    }

    private class FakeExecutionHistoryDao : ExecutionHistoryDao {
        val entries = mutableListOf<ExecutionHistory>()
        var shouldFailOnInsert = false

        override suspend fun insert(entry: ExecutionHistory) {
            if (shouldFailOnInsert) throw RuntimeException("disk full")
            entries += entry.copy(id = (entries.size + 1).toLong())
        }

        override fun getRecent(limit: Int): Flow<List<ExecutionHistory>> {
            return flowOf(entries.take(limit))
        }

        override fun getAll(): Flow<List<ExecutionHistory>> {
            return flowOf(entries.toList())
        }

        override suspend fun deleteOlderThan(cutoff: Long) {
            entries.removeAll { it.timestamp < cutoff }
        }
    }
}
