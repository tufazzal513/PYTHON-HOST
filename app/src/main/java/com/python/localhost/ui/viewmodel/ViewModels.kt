package com.python.localhost.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.python.localhost.data.AppSettings
import com.python.localhost.data.GitStatusInfo
import com.python.localhost.data.ProjectMeta
import com.python.localhost.data.RunConfig
import com.python.localhost.di.AppContainer
import com.python.localhost.git.Credentials
import com.python.localhost.git.GitResult
import com.python.localhost.logs.LogCategory
import com.python.localhost.python.DependencyManager
import com.python.localhost.python.InstallResult
import com.python.localhost.process.RunHelper
import com.python.localhost.process.RunState
import com.python.localhost.process.RunStateInfo
import com.python.localhost.project.ProjectManager
import com.python.localhost.terminal.LineKind
import com.python.localhost.terminal.TerminalLine
import com.python.localhost.terminal.TerminalSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class ProjectsVM(private val container: AppContainer) : ViewModel() {
    private val _projects = MutableStateFlow<List<ProjectMeta>>(emptyList())
    val projects: StateFlow<List<ProjectMeta>> = _projects.asStateFlow()

    fun load() {
        _projects.value = container.projectManager.listProjects()
    }
}

class RunningVM(private val container: AppContainer) : ViewModel() {
    val running: StateFlow<List<RunStateInfo>> = container.processManager.states.map { map ->
        map.values.filter { it.state == RunState.RUNNING || it.state == RunState.STARTING }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val all: StateFlow<Map<String, RunStateInfo>> = container.processManager.states

    fun stop(id: String) = container.processManager.stopRun(id)
    fun output(id: String): String = container.processManager.getOutput(id)
}

class SettingsVM(private val container: AppContainer) : ViewModel() {
    private val _settings = MutableStateFlow(container.settingsStore.getSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(block: (AppSettings) -> AppSettings) {
        val s = block(_settings.value)
        container.settingsStore.saveSettings(s)
        _settings.value = s
    }

    fun setToken(token: String?) = container.settingsStore.setGitHubToken(token)
    fun getToken(): String? = container.settingsStore.getGitHubToken()
}

class CreateProjectVM(private val container: AppContainer) : ViewModel() {
    val versions: List<String> = container.runtimeManager.availableRuntimes().map { it.version }
    val templates = listOf("basic", "flask", "fastapi", "telegram", "automation")

    fun create(name: String, version: String, template: String): ProjectMeta? {
        if (name.isBlank()) return null
        return container.projectManager.createProject(name.trim(), version, template)
    }
}

sealed class GitHubImportState {
    object Idle : GitHubImportState()
    object Importing : GitHubImportState()
    data class Success(val id: String, val msg: String) : GitHubImportState()
    data class Error(val msg: String) : GitHubImportState()
}

class GitHubImportVM(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<GitHubImportState>(GitHubImportState.Idle)
    val state: StateFlow<GitHubImportState> = _state.asStateFlow()
    val versions: List<String> = container.runtimeManager.availableRuntimes().map { it.version }

    fun import(url: String, name: String, version: String) {
        val err = container.githubImporter.validate(url)
        if (err != null) { _state.value = GitHubImportState.Error(err); return }
        _state.value = GitHubImportState.Importing
        viewModelScope.launch(Dispatchers.IO) {
            val target = File(container.appDirs.projects, UUID.randomUUID().toString().take(8))
            val res = container.githubImporter.import(url, target)
            if (res.success) {
                val meta = container.projectManager.createFromCloned(
                    target, name.ifBlank { target.name }, version, url,
                )
                _state.value = GitHubImportState.Success(meta.id, "Imported ${meta.name}")
            } else {
                target.deleteRecursively()
                _state.value = GitHubImportState.Error(res.message)
            }
        }
    }
}

class DashboardVM(private val container: AppContainer, private val projectId: String) : ViewModel() {
    val meta: ProjectMeta = container.projectManager.getProject(projectId)
        ?: throw IllegalArgumentException("Project not found")
    val dir = File(meta.path)
    val config = container.projectManager.getRunConfig(dir)
    val detection = container.projectDetector.detect(dir)

    val runState: StateFlow<RunStateInfo?> = container.processManager.states.map { it[projectId] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val output: StateFlow<String> = container.processManager.outputs.map { it[projectId] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun run(foreground: Boolean, context: android.content.Context) {
        RunHelper.buildAndStart(context, container, meta, config, foreground)
    }

    fun stop() = container.processManager.stopRun(projectId)

    fun saveConfig(newConfig: RunConfig) = container.projectManager.saveRunConfig(dir, newConfig)

    fun install(onOutput: (String) -> Unit): InstallResult {
        val s = container.settingsStore.getSettings()
        container.logManager.append(projectId, LogCategory.DEPENDENCY, "Install started")
        val r = container.dependencyManager.install(projectId, dir, s.pipIndexUrl, s.pipExtraIndexUrl, onOutput)
        container.logManager.append(projectId, LogCategory.DEPENDENCY, r.message)
        return r
    }

    fun installedPackages(): Set<String> = container.dependencyManager.installedPackages(projectId)
}

class TerminalVM(private val container: AppContainer, private val projectId: String) : ViewModel() {
    val meta = container.projectManager.getProject(projectId)
        ?: throw IllegalArgumentException("Project not found")
    val dir = File(meta.path)

    private val _lines = MutableStateFlow<List<TerminalLine>>(
        listOf(TerminalLine("PyMobile Console ready. Type !help for commands.", LineKind.SYSTEM)),
    )
    val lines: StateFlow<List<TerminalLine>> = _lines.asStateFlow()

    private val session = TerminalSession(
        runtime = container.pythonRuntime,
        siteDirs = listOf(dir.absolutePath, container.appDirs.packagesDir(projectId).absolutePath),
        pipTargetDir = container.appDirs.packagesDir(projectId).absolutePath,
        onLine = { line -> _lines.value = _lines.value + line },
        onClear = { _lines.value = emptyList() },
    )

    fun submit(cmd: String) = session.submit(cmd)
}

class LogsVM(private val container: AppContainer, private val projectId: String) : ViewModel() {
    val categories = LogCategory.values().toList()
    private val _category = MutableStateFlow(LogCategory.OUTPUT)
    val category: StateFlow<LogCategory> = _category.asStateFlow()

    fun select(c: LogCategory) { _category.value = c }
    fun content(): String = container.logManager.read(projectId, _category.value)
    fun clear() = container.logManager.clear(projectId, _category.value)
    fun history() = container.logManager.getRunHistory(projectId)
}

class GitVM(private val container: AppContainer, private val projectId: String) : ViewModel() {
    val meta = container.projectManager.getProject(projectId)
        ?: throw IllegalArgumentException("Project not found")
    val dir = File(meta.path)

    private val _status = MutableStateFlow<GitStatusInfo?>(null)
    val status: StateFlow<GitStatusInfo?> = _status.asStateFlow()
    private val _branches = MutableStateFlow<List<String>>(emptyList())
    val branches: StateFlow<List<String>> = _branches.asStateFlow()
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()
    private val _result = MutableStateFlow<String?>(null)
    val result: StateFlow<String?> = _result.asStateFlow()

    fun refresh() {
        _status.value = container.gitManager.status(dir)
        _branches.value = container.gitManager.branches(dir)
    }

    fun setMessage(m: String) { _message.value = m }

    fun commit() {
        val msg = _message.value
        if (msg.isBlank()) { _result.value = "Commit message is required."; return }
        val r = container.gitManager.addAllAndCommit(dir, msg)
        _result.value = r.message
        if (r.success) _message.value = ""
        refresh()
    }

    fun push(token: String?) {
        val t = token ?: container.settingsStore.getGitHubToken()
        if (t.isNullOrBlank()) { _result.value = "GitHub token required (set in Settings)."; return }
        val r = container.gitManager.push(dir, Credentials("token", t))
        _result.value = r.message
        refresh()
    }

    fun pull(token: String?) {
        val t = token ?: container.settingsStore.getGitHubToken()
        val r = if (t.isNullOrBlank()) container.gitManager.pull(dir)
        else container.gitManager.pull(dir, Credentials("token", t))
        _result.value = r.message
        refresh()
    }

    fun createBranch(name: String) {
        if (name.isBlank()) return
        _result.value = container.gitManager.createBranch(dir, name).message
        refresh()
    }

    fun checkout(name: String) {
        _result.value = container.gitManager.checkout(dir, name).message
        refresh()
    }

    fun getStoredToken(): String? = container.settingsStore.getGitHubToken()
}

class EntryPickerVM(private val container: AppContainer, private val projectId: String) : ViewModel() {
    val meta = container.projectManager.getProject(projectId)
        ?: throw IllegalArgumentException("Project not found")
    val dir = File(meta.path)
    val candidates = container.projectDetector.detect(dir).entryCandidates

    fun setEntry(point: String) {
        val config = container.projectManager.getRunConfig(dir)
        container.projectManager.saveRunConfig(dir, config.copy(entryPoint = point))
    }
}
