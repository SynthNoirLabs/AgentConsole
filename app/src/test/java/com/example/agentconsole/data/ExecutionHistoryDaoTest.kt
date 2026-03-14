package com.example.agentconsole.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExecutionHistoryDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ExecutionHistoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.executionHistoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert and getAll returns newest entries first`() = runTest {
        val older = ExecutionHistory.fromExecution(
            agent = "Claude Code",
            workingDir = "~/repo",
            prompt = "older prompt",
            stdout = "older stdout",
            stderr = "",
            exitCode = 0,
            status = "Finished",
            timestamp = 1_000L
        )
        val newer = ExecutionHistory.fromExecution(
            agent = "OpenCode",
            workingDir = "~/repo",
            prompt = "newer prompt",
            stdout = "newer stdout",
            stderr = "",
            exitCode = 0,
            status = "Finished",
            timestamp = 2_000L
        )

        dao.insert(older)
        dao.insert(newer)

        val entries = dao.getAll().first()
        assertEquals(2, entries.size)
        assertEquals("newer prompt", entries[0].prompt)
        assertEquals("older prompt", entries[1].prompt)
    }

    @Test
    fun `deleteOlderThan removes entries older than cutoff`() = runTest {
        dao.insert(
            ExecutionHistory.fromExecution(
                agent = "Claude Code",
                workingDir = "~/repo",
                prompt = "old",
                stdout = "",
                stderr = "",
                exitCode = 0,
                status = "Finished",
                timestamp = 1_000L
            )
        )
        dao.insert(
            ExecutionHistory.fromExecution(
                agent = "Claude Code",
                workingDir = "~/repo",
                prompt = "new",
                stdout = "",
                stderr = "",
                exitCode = 0,
                status = "Finished",
                timestamp = 2_000L
            )
        )

        dao.deleteOlderThan(1_500L)

        val entries = dao.getAll().first()
        assertEquals(1, entries.size)
        assertEquals("new", entries[0].prompt)
        assertTrue(entries[0].timestamp >= 1_500L)
    }

    @Test
    fun `entity fields persist and truncation limits are applied`() = runTest {
        val longPrompt = "p".repeat(ExecutionHistory.MAX_PROMPT_CHARS + 100)
        val longStdout = "o".repeat(ExecutionHistory.MAX_STDOUT_CHARS + 100)
        val longStderr = "e".repeat(ExecutionHistory.MAX_STDERR_CHARS + 100)

        dao.insert(
            ExecutionHistory.fromExecution(
                agent = "Gemini CLI",
                workingDir = "/data/projects/repo",
                prompt = longPrompt,
                stdout = longStdout,
                stderr = longStderr,
                exitCode = 1,
                status = "Finished with errors",
                timestamp = 3_000L
            )
        )

        val stored = dao.getAll().first().single()
        assertEquals("Gemini CLI", stored.agent)
        assertEquals("/data/projects/repo", stored.workingDir)
        assertEquals(ExecutionHistory.MAX_PROMPT_CHARS, stored.prompt.length)
        assertEquals(ExecutionHistory.MAX_STDOUT_CHARS, stored.stdout.length)
        assertEquals(ExecutionHistory.MAX_STDERR_CHARS, stored.stderr.length)
        assertEquals(1, stored.exitCode)
        assertEquals("Finished with errors", stored.status)
        assertEquals(3_000L, stored.timestamp)
        assertTrue(stored.id > 0)
    }
}
