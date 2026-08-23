package com.python.localhost.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.python.localhost.data.ProjectMeta
import com.python.localhost.data.RunConfig
import com.python.localhost.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class RunConfigViewModel(private val container: AppContainer, private val projectId: String) : ViewModel() {
    val meta: ProjectMeta = container.projectManager.getProject(projectId)
        ?: throw IllegalArgumentException("Project not found")
    val dir = File(meta.path)
    private val _config = MutableStateFlow(container.projectManager.getRunConfig(dir))
    val config: StateFlow<RunConfig> = _config.asStateFlow()
    val versions: List<String> = container.runtimeManager.availableRuntimes().map { it.version }

    fun update(block: (RunConfig) -> RunConfig) {
        _config.value = block(_config.value)
    }

    fun save() {
        container.projectManager.saveRunConfig(dir, _config.value)
    }
}
