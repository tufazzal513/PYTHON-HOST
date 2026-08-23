package com.python.localhost.core

import android.content.Context
import java.io.File

/**
 * Centralised, app-managed storage directories. Everything lives under the app's
 * private external files directory, so no broad storage permission is required.
 *
 * PyMobileIDE/
 *   projects/   backups/   logs/   runtimes/   packages/   cache/   settings/
 */
class AppDirs(context: Context) {
    val root: File = File(context.getExternalFilesDir(null), "PyMobileIDE").apply { mkdirs() }
    val projects: File = File(root, "projects").apply { mkdirs() }
    val backups: File = File(root, "backups").apply { mkdirs() }
    val logs: File = File(root, "logs").apply { mkdirs() }
    val runtimes: File = File(root, "runtimes").apply { mkdirs() }
    val packages: File = File(root, "packages").apply { mkdirs() }
    val cache: File = File(root, "cache").apply { mkdirs() }
    val settings: File = File(root, "settings").apply { mkdirs() }

    fun projectDir(id: String): File = File(projects, id)
    fun logDir(projectId: String): File = File(logs, projectId).apply { mkdirs() }
    fun packagesDir(projectId: String): File = File(packages, projectId).apply { mkdirs() }
}
