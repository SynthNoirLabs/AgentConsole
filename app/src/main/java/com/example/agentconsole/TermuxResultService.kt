package com.example.agentconsole

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_SERVICE

class TermuxResultService : Service() {

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

    companion object {
        private const val TAG = "TermuxResultService"
        const val EXTRA_EXECUTION_ID = "execution_id"
    }
}
