package com.python.localhost.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.lifecycle.ViewModel
import com.python.localhost.data.ProjectMeta
import com.python.localhost.di.AppContainer
import com.python.localhost.editor.FileNode
import com.python.localhost.editor.buildTree
import com.python.localhost.logs.LogCategory
import com.python.localhost.util.FileUtils
import java.io.File

class EditorViewModel(private val container: AppContainer, private val projectId: String) : ViewModel() {
    val meta: ProjectMeta = container.projectManager.getProject(projectId)
        ?: throw IllegalArgumentException("Project not found")
    val projectDir = File(meta.path)
    val tree: FileNode = buildTree(projectDir)

    private val _contents = mutableStateMapOf<String, String>()
    private val _openTabs = mutableStateListOf<String>()
    private val _currentTab = mutableStateOf<String?>(null)
    val currentTab: State<String?> = _currentTab
    private val _dirty = mutableStateSetOf<String>()

    fun openFile(relPath: String) {
        if (!_contents.containsKey(relPath)) {
            val file = File(projectDir, relPath)
            _contents[relPath] = if (file.exists()) file.readText() else ""
        }
        if (!_openTabs.contains(relPath)) _openTabs.add(relPath)
        _currentTab.value = relPath
    }

    fun closeTab(relPath: String) {
        _openTabs.remove(relPath)
        _contents.remove(relPath)
        if (_currentTab.value == relPath) _currentTab.value = _openTabs.lastOrNull()
    }

    fun setContent(relPath: String, text: String) {
        _contents[relPath] = text
        _dirty.add(relPath)
    }

    fun isDirty(relPath: String) = _dirty.contains(relPath)

    fun save(relPath: String) {
        val text = _contents[relPath] ?: return
        File(projectDir, relPath).writeText(text)
        _dirty.remove(relPath)
        container.logManager.append(projectId, LogCategory.SYSTEM, "Saved $relPath")
    }

    fun fileAbsolute(relPath: String) = File(projectDir, relPath)

    fun content(relPath: String): String = _contents[relPath] ?: ""

    fun relPathOf(file: File): String = FileUtils.relativePath(projectDir, file)
}
