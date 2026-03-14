package com.example.agentconsole

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE

class TermuxResultService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val executionId = intent?.getIntExtra(EXTRA_EXECUTION_ID, -1) ?: -1
        Log.d(TAG, "Received result for execution #$executionId")

        val resultBundle = intent?.getBundleExtra(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE)
        if (resultBundle == null) {
            Log.w(TAG, "No result bundle for execution #$executionId")
            ResultBus.fail("No result bundle returned from Termux.")
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val stdout = resultBundle.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT, "") ?: ""
        val stderr = resultBundle.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_STDERR, "") ?: ""
        val exitCode = resultBundle.getInt(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE, -1)
        val internalErr = resultBundle.getInt(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERR, -1)
        val internalErrMsg = resultBundle.getString(TERMUX_SERVICE.EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG, "") ?: ""

        Log.d(TAG, "Execution #$executionId: exitCode=$exitCode, " +
            "stdout=${stdout.length} chars, stderr=${stderr.length} chars")

        ResultBus.publishResult(
            executionId = executionId,
            stdout = stdout,
            stderr = stderr,
            exitCode = exitCode,
            internalErrorCode = internalErr,
            internalErrorMessage = internalErrMsg
        )

        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Agent Execution",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shown while an agent command result is being processed."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Agent Console")
                .setContentText("Processing agent result\u2026")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Agent Console")
                .setContentText("Processing agent result\u2026")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .build()
        }
    }

    companion object {
        private const val TAG = "TermuxResultService"
        const val EXTRA_EXECUTION_ID = "execution_id"
        const val NOTIFICATION_CHANNEL_ID = "agent_result_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
