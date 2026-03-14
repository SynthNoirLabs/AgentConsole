package com.example.agentconsole

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TermuxRepositoryTest {

    private val repository = TermuxRepository()

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
}