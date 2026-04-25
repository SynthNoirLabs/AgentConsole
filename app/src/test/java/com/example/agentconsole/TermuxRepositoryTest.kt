package com.example.agentconsole

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxRepositoryTest {

    private val repository = TermuxRepository(ResultBus())

    @Test
    fun `blank workdir is valid (defaults to home)`() {
        assertNull(repository.validateWorkingDir(""))
        assertNull(repository.validateWorkingDir("   "))
    }

    @Test
    fun `absolute path is valid`() {
        assertNull(repository.validateWorkingDir("/data/projects/myrepo"))
        assertNull(repository.validateWorkingDir("/sdcard/Download/repo"))
    }

    @Test
    fun `home-relative path is valid`() {
        assertNull(repository.validateWorkingDir("~/projects/myrepo"))
        assertNull(repository.validateWorkingDir("~/"))
    }

    @Test
    fun `relative path without tilde or slash is rejected`() {
        assertNotNull(repository.validateWorkingDir("projects/myrepo"))
        assertNotNull(repository.validateWorkingDir("myrepo"))
    }

    @Test
    fun `path traversal is rejected`() {
        assertNotNull(repository.validateWorkingDir("/data/../etc/passwd"))
        assertNotNull(repository.validateWorkingDir("~/projects/../../secrets"))
    }

    @Test
    fun `shell metacharacters are rejected`() {
        assertNotNull(repository.validateWorkingDir("/data; rm -rf /"))
        assertNotNull(repository.validateWorkingDir("/data && echo pwned"))
        assertNotNull(repository.validateWorkingDir("/data | cat /etc/passwd"))
        assertNotNull(repository.validateWorkingDir("/data\$(whoami)"))
        assertNotNull(repository.validateWorkingDir("/data`whoami`"))
    }

    @Test
    fun `paths with normal characters are accepted`() {
        assertNull(repository.validateWorkingDir("/home/user/my-project_v2"))
        assertNull(repository.validateWorkingDir("~/code/my.dotted.dir"))
        assertNull(repository.validateWorkingDir("/sdcard/Android/data/com.app/files"))
    }

    @Test
    fun `validatePrompt rejects blank prompts`() {
        assertNotNull(repository.validatePrompt(""))
        assertNotNull(repository.validatePrompt("   "))
        assertNotNull(repository.validatePrompt("\n\t  "))
    }

    @Test
    fun `validatePrompt rejects null bytes`() {
        assertNotNull(repository.validatePrompt("hello\u0000world"))
        assertNotNull(repository.validatePrompt("\u0000"))
    }

    @Test
    fun `validatePrompt rejects oversized prompts`() {
        val oversize = "a".repeat(TermuxRepository.MAX_PROMPT_SIZE + 1)
        assertNotNull(repository.validatePrompt(oversize))
    }

    @Test
    fun `validatePrompt accepts normal prompts`() {
        assertNull(repository.validatePrompt("Summarize this codebase."))
        assertNull(repository.validatePrompt("a".repeat(TermuxRepository.MAX_PROMPT_SIZE)))
    }

    @Test
    fun `truncateOutput leaves small inputs unchanged`() {
        val small = "ok"
        assertEquals(small, TermuxRepository.truncateOutput(small, "stdout"))
    }

    @Test
    fun `truncateOutput marks oversized inputs`() {
        val big = "x".repeat(TermuxRepository.MAX_OUTPUT_SIZE + 100)
        val truncated = TermuxRepository.truncateOutput(big, "stdout")
        assertTrue(truncated.length < big.length)
        assertTrue(truncated.contains("truncated"))
        assertTrue(truncated.contains("stdout"))
    }
}
