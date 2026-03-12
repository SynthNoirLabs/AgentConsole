package com.example.agentconsole

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE
import java.util.concurrent.atomic.AtomicInteger

object TermuxRunner {

    private const val TAG = "TermuxRunner"
    private val nextExecutionId = AtomicInteger(1000)

    /** Characters that are not allowed in working directory paths. */
    private val DANGEROUS_PATH_CHARS = Regex("[;|&`$\\\\\"'<>(){}!#]")

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TermuxConstants.TERMUX_PACKAGE_NAME, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openTermux(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(TermuxConstants.TERMUX_PACKAGE_NAME)
        if (launchIntent != null) {
            context.startActivity(launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /**
     * Validates the working directory path.
     * Returns null if valid, or an error message if invalid.
     */
    fun validateWorkingDir(workingDir: String): String? {
        if (workingDir.isBlank()) return null // blank means default ~/
        if (DANGEROUS_PATH_CHARS.containsMatchIn(workingDir)) {
            return "Working directory contains invalid characters."
        }
        if (workingDir.contains("..")) {
            return "Working directory must not contain '..' path traversal."
        }
        if (!workingDir.startsWith("/") && !workingDir.startsWith("~/")) {
            return "Working directory must be an absolute path (start with / or ~/)."
        }
        return null
    }

    fun run(
        context: Context,
        agent: Agent,
        prompt: String,
        workingDir: String
    ) {
        if (prompt.isBlank()) {
            Log.w(TAG, "Run rejected: empty prompt")
            ExecutionStore.fail("Prompt is empty.")
            return
        }

        val workdirError = validateWorkingDir(workingDir)
        if (workdirError != null) {
            Log.w(TAG, "Run rejected: $workdirError (input=$workingDir)")
            ExecutionStore.fail(workdirError)
            return
        }

        if (!isTermuxInstalled(context)) {
            Log.w(TAG, "Run rejected: Termux not installed")
            ExecutionStore.fail("Termux is not installed.")
            return
        }

        val executionId = nextExecutionId.incrementAndGet()
        val safeWorkdir = if (workingDir.isBlank()) "~/" else workingDir

        Log.d(TAG, "Starting execution #$executionId: agent=${agent.cliName}, workdir=$safeWorkdir")

        val resultIntent = Intent(context, TermuxResultService::class.java).apply {
            putExtra(TermuxResultService.EXTRA_EXECUTION_ID, executionId)
        }

        val pendingIntentFlags = PendingIntent.FLAG_ONE_SHOT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        val pendingIntent = PendingIntent.getService(
            context,
            executionId,
            resultIntent,
            pendingIntentFlags
        )

        val intent = Intent().apply {
            setClassName(
                TermuxConstants.TERMUX_PACKAGE_NAME,
                TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE_NAME
            )
            action = RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND

            putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_PATH, "~/bin/agent_runner.sh")
            putExtra(RUN_COMMAND_SERVICE.EXTRA_ARGUMENTS, arrayOf(agent.cliName, prompt))
            putExtra(RUN_COMMAND_SERVICE.EXTRA_WORKDIR, safeWorkdir)
            putExtra(RUN_COMMAND_SERVICE.EXTRA_BACKGROUND, true)
            putExtra(RUN_COMMAND_SERVICE.EXTRA_COMMAND_LABEL, "${agent.displayName} run")
            putExtra(
                RUN_COMMAND_SERVICE.EXTRA_COMMAND_DESCRIPTION,
                "Runs ${agent.displayName} in background mode for the selected repo."
            )
            putExtra(RUN_COMMAND_SERVICE.EXTRA_PENDING_INTENT, pendingIntent)
        }

        ExecutionStore.markRunning(executionId, agent.displayName, safeWorkdir)

        try {
            context.startService(intent)
            Log.d(TAG, "Termux service started for execution #$executionId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting Termux for execution #$executionId", e)
            ExecutionStore.fail(
                "Missing Termux permission. In Android Settings, grant this app 'Run commands in Termux environment', then enable allow-external-apps=true inside Termux."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting Termux for execution #$executionId", e)
            ExecutionStore.fail("Could not start Termux command: ${e.message}")
        }
    }
}
