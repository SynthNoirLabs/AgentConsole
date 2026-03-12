package com.example.agentconsole

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.termux.shared.termux.TermuxConstants
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.RUN_COMMAND_SERVICE
import java.util.concurrent.atomic.AtomicInteger

object TermuxRunner {

    private val nextExecutionId = AtomicInteger(1000)

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

    fun run(
        context: Context,
        agent: Agent,
        prompt: String,
        workingDir: String
    ) {
        if (prompt.isBlank()) {
            ExecutionStore.fail("Prompt is empty.")
            return
        }

        if (!isTermuxInstalled(context)) {
            ExecutionStore.fail("Termux is not installed.")
            return
        }

        val executionId = nextExecutionId.incrementAndGet()
        val safeWorkdir = if (workingDir.isBlank()) "~/" else workingDir

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
        } catch (e: SecurityException) {
            ExecutionStore.fail(
                "Missing Termux permission. In Android Settings, grant this app 'Run commands in Termux environment', then enable allow-external-apps=true inside Termux."
            )
        } catch (e: Exception) {
            ExecutionStore.fail("Could not start Termux command: ${e.message}")
        }
    }
}
