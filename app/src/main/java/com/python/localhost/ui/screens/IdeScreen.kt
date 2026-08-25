package com.python.localhost.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.editor.CodeEditor
import com.python.localhost.editor.FileTree
import com.python.localhost.editor.defaultDarkColors
import com.python.localhost.editor.detectLanguage
import com.python.localhost.process.RunHelper
import com.python.localhost.python.RunState
import com.python.localhost.python.RunStateInfo
import com.python.localhost.terminal.LineKind
import com.python.localhost.ui.components.EmptyState
import com.python.localhost.ui.components.StatusChip
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.EditorViewModel
import com.python.localhost.ui.viewmodel.TerminalVM

/**
 * Android-Studio-style project workspace: run toolbar, file tree, editor tabs,
 * bottom tool window with RUN / TERMINAL, and a status bar.
 */
@Composable
fun IdeScreen(nav: NavHostController, container: AppContainer, projectId: String, file: String? = null) {
    val vm: EditorViewModel = provideVm(container) { EditorViewModel(it, projectId) }
    val termVm: TerminalVM = provideVm(container) { TerminalVM(it, projectId) }
    val ctx = LocalContext.current
    val config = remember { container.projectManager.getRunConfig(vm.projectDir) }
    val settings = remember { container.settingsStore.getSettings() }

    val states by container.processManager.states.collectAsState()
    val outputs by container.processManager.outputs.collectAsState()
    val runState: RunStateInfo? = states[projectId]
    val runOutput: String = outputs[projectId] ?: ""

    val termLines by termVm.lines.collectAsState()

    var showTerminal by remember { mutableStateOf(false) }
    var showTree by remember { mutableStateOf(true) }
    var termInput by remember { mutableStateOf("") }

    val currentTab: String? by vm.currentTab
    var tfv by remember(currentTab) {
        val text = vm.content(currentTab ?: "")
        mutableStateOf(TextFieldValue(text, TextRange(text.length)))
    }
    val undoStack = remember { mutableStateListOf<TextFieldValue>() }
    val redoStack = remember { mutableStateListOf<TextFieldValue>() }

    LaunchedEffect(Unit) {
        if (vm.currentTab.value == null) {
            val entry = if (!file.isNullOrBlank() && vm.fileAbsolute(file).exists()) {
                file
            } else if (vm.fileAbsolute(config.entryPoint).exists()) {
                config.entryPoint
            } else if (vm.fileAbsolute("main.py").exists()) {
                "main.py"
            } else {
                firstPythonFile(vm)
            }
            if (entry != null) vm.openFile(entry)
        }
    }
    val tab = currentTab

    fun doRun() {
        val t = tab
        if (t != null && vm.isDirty(t)) vm.save(t)
        showTerminal = false
        RunHelper.buildAndStart(ctx, container, vm.meta, config, false)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ---------- Run toolbar ----------
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                Modifier.fillMaxWidth().height(46.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text(vm.meta.name, fontSize = 15.sp, style = MaterialTheme.typography.titleMedium)
                    Text(config.entryPoint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (runState != null) {
                    StatusChip(
                        runState.state.name,
                        if (runState.state == RunState.FAILED) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                IconButton(onClick = { doRun() }, modifier = Modifier.width(38.dp)) {
                    Icon(Icons.Filled.PlayArrow, "Run", tint = Color(0xFF4CAF50))
                }
                IconButton(onClick = { container.processManager.stopRun(projectId) }, modifier = Modifier.width(38.dp)) {
                    Icon(Icons.Filled.Stop, "Stop", tint = MaterialTheme.colorScheme.error)
                }
                IconButton(onClick = { nav.navigate(Routes.dashboard(projectId)) }, modifier = Modifier.width(38.dp)) {
                    Text("⋯", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))

        // ---------- Editor area ----------
        Row(Modifier.weight(1f)) {
            if (showTree) {
                Column(
                    Modifier.fillMaxHeight().width(180.dp)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(6.dp),
                ) {
                    Text("Project", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FileTree(
                        vm.tree,
                        vm.fileAbsolute(tab ?: "").absolutePath,
                        onOpenFile = { f -> vm.openFile(vm.relPathOf(f)) },
                    )
                }
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showTree = !showTree }, modifier = Modifier.width(34.dp)) {
                            Icon(Icons.Filled.Menu, "Files", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LazyRow(Modifier.weight(1f)) {
                            items(vm.openTabs) { t ->
                                val active = t == tab
                                Box(
                                    Modifier
                                        .background(if (active) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { vm.openFile(t) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            t.substringAfterLast("/") + if (vm.isDirty(t)) " ●" else "",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        IconButton(onClick = { vm.closeTab(t) }, modifier = Modifier.width(22.dp)) {
                                            Icon(Icons.Filled.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(12.dp))
                                        }
                                    }
                                }
                            }
                        }
                        IconButton(onClick = {
                            val prev = tfv
                            if (undoStack.isNotEmpty()) {
                                redoStack.add(prev)
                                tfv = undoStack.removeAt(undoStack.lastIndex)
                                val t = tab
                                if (t != null) vm.setContent(t, tfv.text)
                            }
                        }, modifier = Modifier.width(34.dp)) {
                            Icon(Icons.Filled.Undo, "Undo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            val prev = tfv
                            if (redoStack.isNotEmpty()) {
                                undoStack.add(prev)
                                tfv = redoStack.removeAt(redoStack.lastIndex)
                                val t = tab
                                if (t != null) vm.setContent(t, tfv.text)
                            }
                        }, modifier = Modifier.width(34.dp)) {
                            Icon(Icons.Filled.Redo, "Redo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            val t = tab
                            if (t != null) vm.save(t)
                        }, modifier = Modifier.width(34.dp)) {
                            Icon(Icons.Filled.Save, "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showTerminal = !showTerminal }, modifier = Modifier.width(34.dp)) {
                            Text(">_ ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }

                if (tab != null) {
                    CodeEditor(
                        value = tfv,
                        onValueChange = { new ->
                            undoStack.add(tfv)
                            if (undoStack.size > 100) undoStack.removeAt(0)
                            redoStack.clear()
                            tfv = new
                            vm.setContent(tab, new.text)
                        },
                        language = detectLanguage(tab),
                        fontSize = settings.fontSize,
                        wordWrap = settings.wordWrap,
                        showLineNumbers = settings.showLineNumbers,
                        colors = defaultDarkColors(),
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        EmptyState("Open a file from the project tree")
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))

        // ---------- Bottom tool window: RUN output / TERMINAL ----------
        if (showTerminal) {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().height(210.dp)) {
                    Text(
                        "TERMINAL — Python console (variables persist)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                    val listState = rememberLazyListState()
                    LaunchedEffect(termLines.size) {
                        if (termLines.isNotEmpty()) listState.animateScrollToItem(termLines.size - 1)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp),
                    ) {
                        items(termLines) { line ->
                            Text(
                                line.text,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = when (line.kind) {
                                    LineKind.INPUT -> MaterialTheme.colorScheme.primary
                                    LineKind.ERROR -> MaterialTheme.colorScheme.error
                                    LineKind.SYSTEM, LineKind.META -> MaterialTheme.colorScheme.onSurfaceVariant
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = termInput,
                        onValueChange = { termInput = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        placeholder = { Text("python>", fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            termVm.submit(termInput)
                            termInput = ""
                        }),
                    )
                }
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().height(210.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "RUN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "Rerun",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { doRun() }.padding(4.dp),
                        )
                    }
                    Text(
                        runOutput.ifBlank {
                            if (runState != null) "Running…" else "No output yet. Press ▶ to run the project."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))

        // ---------- Status bar ----------
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                Modifier.fillMaxWidth().height(22.dp).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Python ${container.runtimeManager.embeddedVersion()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (runState != null) runState.state.name else "idle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun firstPythonFile(vm: EditorViewModel): String? {
    fun walk(node: com.python.localhost.editor.FileNode): String? {
        node.children.forEach { c ->
            if (!c.isDir && c.name.endsWith(".py")) {
                return c.path.substringAfter(vm.projectDir.absolutePath + "/")
            }
            if (c.isDir) walk(c)?.let { return it }
        }
        return null
    }
    return walk(vm.tree)
}
