package com.python.localhost.di

import android.app.Application
import com.python.localhost.core.AppDirs
import com.python.localhost.core.JsonStore
import com.python.localhost.core.SettingsStore
import com.python.localhost.git.GitManager
import com.python.localhost.github.GitHubImporter
import com.python.localhost.logs.LogManager
import com.python.localhost.process.ProcessManager
import com.python.localhost.project.ProjectDetector
import com.python.localhost.project.ProjectManager
import com.python.localhost.python.DependencyManager
import com.python.localhost.python.PythonRuntime
import com.python.localhost.python.RuntimeManager
import com.python.localhost.storage.StorageManager

/**
 * Composition root. Holds the singletons used across the app. Created once in
 * PyMobileApplication and accessed via `application.container`.
 */
class AppContainer(application: Application) {
    val appDirs = AppDirs(application)
    val json = JsonStore()
    val pythonRuntime = PythonRuntime(application)
    val runtimeManager = RuntimeManager(application, pythonRuntime)
    val projectManager = ProjectManager(appDirs, json)
    val projectDetector = ProjectDetector()
    val dependencyManager = DependencyManager(pythonRuntime, appDirs)
    val logManager = LogManager(appDirs, json)
    val processManager = ProcessManager(pythonRuntime, logManager)
    val gitManager = GitManager()
    val githubImporter = GitHubImporter(gitManager)
    val storageManager = StorageManager(application)
    val settingsStore = SettingsStore(application, appDirs, json)
}
