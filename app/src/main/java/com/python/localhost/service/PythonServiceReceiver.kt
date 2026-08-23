package com.python.localhost.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.python.localhost.PyMobileApplication

/**
 * Handles the Stop action from the foreground notification.
 */
class PythonServiceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == PythonForegroundService.ACTION_STOP) {
            val projectId = intent.getStringExtra(PythonForegroundService.EXTRA_PROJECT_ID)
            val app = context.applicationContext as PyMobileApplication
            if (projectId != null) app.container.processManager.stopRun(projectId)
            context.stopService(Intent(context, PythonForegroundService::class.java))
        } else if (intent.action == PythonForegroundService.ACTION_RESTART) {
            val projectId = intent.getStringExtra(PythonForegroundService.EXTRA_PROJECT_ID)
            val app = context.applicationContext as PyMobileApplication
            if (projectId != null) app.container.processManager.restartLast(projectId)
        }
    }
}
