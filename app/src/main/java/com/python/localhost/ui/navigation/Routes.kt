package com.python.localhost.ui.navigation

object Routes {
    const val HOME = "home"
    const val PROJECTS = "projects"
    const val RUNNING = "running"
    const val SETTINGS = "settings"
    const val CREATE_PROJECT = "create_project"
    const val GITHUB_IMPORT = "github_import"
    const val RUN_CONFIG = "run_config/{projectId}"
    const val SERVER = "server/{projectId}?url={url}"
    const val DASHBOARD = "dashboard/{projectId}"
    const val EDITOR = "editor/{projectId}?file={file}"
    const val TERMINAL = "terminal/{projectId}"
    const val LOGS = "logs/{projectId}"
    const val GIT = "git/{projectId}"
    const val ENTRY_PICKER = "entry_picker/{projectId}"

    fun dashboard(projectId: String) = "dashboard/$projectId"
    fun editor(projectId: String, file: String? = null) = "editor/$projectId?file=${file ?: ""}"
    fun terminal(projectId: String) = "terminal/$projectId"
    fun logs(projectId: String) = "logs/$projectId"
    fun git(projectId: String) = "git/$projectId"
    fun entryPicker(projectId: String) = "entry_picker/$projectId"
    fun runConfig(projectId: String) = "run_config/$projectId"
    fun server(projectId: String, url: String): String {
        val enc = java.net.URLEncoder.encode(url, "UTF-8")
        return "server/$projectId?url=$enc"
    }
}
