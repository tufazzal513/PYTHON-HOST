package com.python.localhost.process

import android.content.Context
import androidx.core.content.ContextCompat
import com.python.localhost.data.ProjectMeta
import com.python.localhost.data.RunConfig
import com.python.localhost.di.AppContainer
import com.python.localhost.python.RunStateInfo
import com.python.localhost.service.PythonForegroundService
import com.python.localhost.util.EnvParser
import com.python.localhost.util.FileUtils
import java.io.File

/**
 * Builds a RunRequest from a project's metadata + run configuration and starts it via
 * ProcessManager. If foreground execution is requested, also launches the foreground
 * service (with its persistent notification) so long-running projects survive minimise.
 */
object RunHelper {
    fun buildAndStart(
        context: Context,
        container: AppContainer,
        meta: ProjectMeta,
        config: RunConfig,
        foreground: Boolean,
    ): RunStateInfo {
        val dir = File(meta.path)
        val entryFile = File(dir, config.entryPoint)
        val scriptPath = if (entryFile.exists()) entryFile.absolutePath else File(dir, "main.py").absolutePath
        val env = buildEnv(dir, config)
        val siteDirs = listOf(dir.absolutePath, container.appDirs.packagesDir(meta.id).absolutePath)
        val workdirFile = if (config.workingDir.isBlank()) dir else File(dir, config.workingDir)
        val workdir = if (workdirFile.exists()) workdirFile.absolutePath else dir.absolutePath
        val argv = config.arguments.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        val req = RunRequest(
            projectId = meta.id,
            projectName = meta.name,
            scriptPath = scriptPath,
            argv = argv,
            env = env,
            siteDirs = siteDirs,
            foreground = foreground,
            workingDir = workdir,
        )
        val status = container.processManager.startRun(req)
        if (foreground) {
            try {
                val intent = android.content.Intent(context, PythonForegroundService::class.java)
                    .putExtra(PythonForegroundService.EXTRA_PROJECT_ID, meta.id)
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                container.logManager.append(meta.id, com.python.localhost.logs.LogCategory.ERROR,
                    "Could not start foreground service: ${e.message}")
            }
        }
        return status
    }

    private fun buildEnv(dir: File, config: RunConfig): Map<String, String> {
        val env = HashMap(config.environment)
        val dotenv = File(dir, ".env")
        if (dotenv.exists()) env.putAll(EnvParser.parse(dotenv))
        return env
    }

    fun relPath(base: File, file: File): String = FileUtils.relativePath(base, file)
}
