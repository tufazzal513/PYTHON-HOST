package com.python.localhost.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.editor.CodeEditor
import com.python.localhost.editor.FileTree
import com.python.localhost.editor.defaultDarkColors
import com.python.localhost.editor.detectLanguage
import com.python.localhost.process.RunHelper
import com.python.localhost.ui.components.EmptyState
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    nav: NavHostController,
    container: AppContainer,
    projectId: String,
    initialFile: String?,
) {
    val vm: EditorViewModel = provideVm(container) { EditorViewModel(it, projectId) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val settings = remember { container.settingsStore.getSettings() }
    val colors = defaultDarkColors()

    LaunchedEffect(initialFile) {
        if (!initialFile.isNullOrBlank()) vm.openFile(initialFile)
    }

    val currentTab = vm.currentTab.value
    var tfv by remember(currentTab) { mutableStateOf(TextFieldValue(vm.content(currentTab ?: ""))) }
    val undoStack = remember(currentTab) { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember(currentTab) { mutableStateListOf<TextFieldValue>() }

    var showTree by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showGoto by remember { mutableStateOf(false) }

    fun runCurrent() {
        val config = container.projectManager.getRunConfig(vm.projectDir)
        RunHelper.buildAndStart(nav.context, container, vm.meta, config, config.runInForeground)
        scope.launch { snackbar.showSnackbar("Started: ${vm.meta.name}") }
        nav.navigate(Routes.dashboard(projectId)) { popUpTo(Routes.EDITOR) { inclusive = true } }
    }

    Scaffold(
        topBar = {
            IdeTopBar(
                title = currentTab ?: vm.meta.name,
                subtitle = if (currentTab != null && vm.isDirty(currentTab)) "● unsaved" else null,
                onBack = { nav.popBackStack() },
                actions = {
                    IconButton(onClick = { showTree = !showTree }) { Icon(Icons.Default.Menu, "Files") }
                    IconButton(onClick = {
                        val prev = tfv
                        if (undoStack.isNotEmpty()) {
                            redoStack.add(prev)
                            tfv = undoStack.removeAt(undoStack.lastIndex)
                            currentTab?.let { vm.setContent(it, tfv.text) }
                        }
                    }) { Icon(Icons.Default.Undo, "Undo") }
                    IconButton(onClick = {
                        val prev = tfv
                        if (redoStack.isNotEmpty()) {
                            undoStack.add(prev)
                            tfv = redoStack.removeAt(redoStack.lastIndex)
                            currentTab?.let { vm.setContent(it, tfv.text) }
                        }
                    }) { Icon(Icons.Default.Redo, "Redo") }
                    IconButton(onClick = { if (currentTab != null) vm.save(currentTab) }) { Icon(Icons.Default.Save, "Save") }
                    IconButton(onClick = { showSearch = true }) { Icon(Icons.Default.Search, "Search") }
                    IconButton(onClick = { runCurrent() }) { Icon(Icons.Default.PlayArrow, "Run") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            if (showTree) {
                Column(
                    Modifier.fillMaxHeight().width(170.dp).padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    FileTree(
                        vm.tree, vm.fileAbsolute(currentTab ?: "").absolutePath,
                        onOpenFile = { f -> vm.openFile(vm.relPathOf(f)) },
                    )
                }
            }
            Column(Modifier.fillMaxSize().weight(1f)) {
                LazyRow(Modifier.fillMaxWidth().padding(4.dp)) {
                    items(vm.openTabs) { tab ->
                        val name = tab.substringAfterLast("/")
                        Surface(
                            color = if (tab == currentTab) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 4.dp).clickable { vm.openFile(tab) },
                        ) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(name + if (vm.isDirty(tab)) " ●" else "", color = MaterialTheme.colorScheme.onSurface)
                                IconButton(onClick = { vm.closeTab(tab) }, modifier = Modifier.width(20.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.width(14.dp))
                                }
                            }
                        }
                    }
                }
                if (currentTab != null) {
                    val lang = detectLanguage(currentTab!!)
                    CodeEditor(
                        value = tfv,
                        onValueChange = { new ->
                            undoStack.add(tfv)
                            if (undoStack.size > 100) undoStack.removeAt(0)
                            redoStack.clear()
                            tfv = new
                            vm.setContent(currentTab, new.text)
                        },
                        language = lang,
                        fontSize = settings.fontSize,
                        wordWrap = settings.wordWrap,
                        showLineNumbers = settings.showLineNumbers,
                        colors = colors,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState("Open a file from the file tree to start editing.")
                    }
                }
            }
        }
    }

    if (showSearch) {
        SearchDialog(
            onDismiss = { showSearch = false },
            onReplaceAll = { find, replace ->
                val newText = tfv.text.replace(find, replace)
                tfv = TextFieldValue(newText, selection = TextRange(newText.length))
                currentTab?.let { vm.setContent(it, newText) }
                showSearch = false
            },
            onGoto = { showSearch = false; showGoto = true },
        )
    }
    if (showGoto) {
        GotoDialog(
            onDismiss = { showGoto = false },
            onGo = { line ->
                val lines = tfv.text.split("\n")
                var offset = 0
                val target = (line - 1).coerceAtLeast(0)
                for (k in 0 until target.coerceAtMost(lines.size - 1)) offset += lines[k].length + 1
                tfv = tfv.copy(selection = TextRange(offset.coerceAtMost(tfv.text.length)))
                showGoto = false
            },
        )
    }
}

@Composable
private fun SearchDialog(
    onDismiss: () -> Unit,
    onReplaceAll: (String, String) -> Unit,
    onGoto: () -> Unit,
) {
    var find by remember { mutableStateOf("") }
    var replace by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search / Replace") },
        text = {
            Column {
                OutlinedTextField(find, { find = it }, label = { Text("Find") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(replace, { replace = it }, label = { Text("Replace") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                TextButton(onClick = onGoto) { Text("Go to line…") }
            }
        },
        confirmButton = { TextButton(onClick = { onReplaceAll(find, replace) }) { Text("Replace All") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun GotoDialog(onDismiss: () -> Unit, onGo: (Int) -> Unit) {
    var line by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to line") },
        text = {
            OutlinedTextField(line, { line = it }, label = { Text("Line number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { line.toIntOrNull()?.let { onGo(it) } ?: onDismiss() }) { Text("Go") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
