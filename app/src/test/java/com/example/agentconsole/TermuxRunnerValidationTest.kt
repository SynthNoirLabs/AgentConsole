package com.example.agentconsole

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TermuxRunnerValidationTest {

    @Test
    fun `blank workdir is valid (defaults to home)`() {
        assertNull(TermuxRunner.validateWorkingDir(""))
        assertNull(TermuxRunner.validateWorkingDir("   "))
    }

    @Test
    fun `absolute path is valid`() {
        assertNull(TermuxRunner.validateWorkingDir("/data/projects/myrepo"))
        assertNull(TermuxRunner.validateWorkingDir("/sdcard/Download/repo"))
    }

    @Test
    fun `home-relative path is valid`() {
        assertNull(TermuxRunner.validateWorkingDir("~/projects/myrepo"))
        assertNull(TermuxRunner.validateWorkingDir("~/"))
    }

    @Test
    fun `relative path without tilde or slash is rejected`() {
        assertNotNull(TermuxRunner.validateWorkingDir("projects/myrepo"))
        assertNotNull(TermuxRunner.validateWorkingDir("myrepo"))
    }

    @Test
    fun `path traversal is rejected`() {
        assertNotNull(TermuxRunner.validateWorkingDir("/data/../etc/passwd"))
        assertNotNull(TermuxRunner.validateWorkingDir("~/projects/../../secrets"))
    }

    @Test
    fun `shell metacharacters are rejected`() {
        assertNotNull(TermuxRunner.validateWorkingDir("/data; rm -rf /"))
        assertNotNull(TermuxRunner.validateWorkingDir("/data && echo pwned"))
        assertNotNull(TermuxRunner.validateWorkingDir("/data | cat /etc/passwd"))
        assertNotNull(TermuxRunner.validateWorkingDir("/data\$(whoami)"))
        assertNotNull(TermuxRunner.validateWorkingDir("/data`whoami`"))
    }

    @Test
    fun `paths with normal characters are accepted`() {
        assertNull(TermuxRunner.validateWorkingDir("/home/user/my-project_v2"))
        assertNull(TermuxRunner.validateWorkingDir("~/code/my.dotted.dir"))
        assertNull(TermuxRunner.validateWorkingDir("/sdcard/Android/data/com.app/files"))
    }
}
