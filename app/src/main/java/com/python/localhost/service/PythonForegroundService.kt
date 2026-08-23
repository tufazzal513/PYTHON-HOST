package com.python.localhost.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.python.localhost.MainActivity
import com.python.localhost.R

/**
 * Keeps the app process alive (with a persistent notification) while a long-running
 * Python project (server, bot, automation) executes. The actual execution is owned
 * by ProcessManager; this service simply preserves the process and offers Stop/Open.
 */
class PythonForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "pymobile_foreground"
        const val ACTION_STOP = "com.python.localhost.action.STOP"
        const val ACTION_RESTART = "com.python.localhost.action.RESTART"
        const val ACTION_OPEN = "com.python.localhost.action.OPEN"
        const val EXTRA_PROJECT_ID = "project_id"
        const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val projectId = intent?.getStringExtra(EXTRA_PROJECT_ID) ?: "unknown"
        startForeground(NOTIFICATION_ID, buildNotification(projectId))
        return START_STICKY
    }

    private fun buildNotification(projectId: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_OPEN
            putExtra(EXTRA_PROJECT_ID, projectId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPi = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, PythonServiceReceiver::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_PROJECT_ID, projectId)
        }
        val stopPi = PendingIntent.getBroadcast(this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val restartIntent = Intent(this, PythonServiceReceiver::class.java).apply {
            action = ACTION_RESTART
            putExtra(EXTRA_PROJECT_ID, projectId)
        }
        val restartPi = PendingIntent.getBroadcast(this, 2, restartIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PyMobile IDE")
            .setContentText("Running: $projectId")
            .setSmallIcon(R.drawable.ic_notify)
            .setContentIntent(openPi)
            .addAction(android.R.drawable.ic_popup_sync, "Restart", restartPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Python Runtime", NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
