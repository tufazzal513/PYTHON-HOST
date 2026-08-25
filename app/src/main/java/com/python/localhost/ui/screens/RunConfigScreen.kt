package com.python.localhost.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.data.RunConfig
import com.python.localhost.di.AppContainer
import com.python.localhost.ui.components.DropdownField
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.PrimaryButton
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.RunConfigViewModel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch

@Composable
fun RunConfigScreen(nav: NavHostController, container: AppContainer, projectId: String) {
    val vm: RunConfigViewModel = provideVm(container) { RunConfigViewModel(it, projectId) }
    val config by vm.config.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var entry by remember { mutableStateOf(config.entryPoint) }
    var args by remember { mutableStateOf(config.arguments) }
    var workdir by remember { mutableStateOf(config.workingDir) }
    var envText by remember { mutableStateOf(config.environment.entries.joinToString("\n") { "${it.key}=${it.value}" }) }
    var version by remember { mutableStateOf(config.pythonVersion) }
    var fg by remember { mutableStateOf(config.runInForeground) }
    var port by remember { mutableStateOf(config.port.toString()) }

    Scaffold(
        topBar = { IdeTopBar(title = "Run Configuration", onBack = { nav.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(entry, { entry = it }, label = { Text("Entry point (relative to project)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(args, { args = it }, label = { Text("Arguments") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(workdir, { workdir = it }, label = { Text("Working directory (relative, empty = root)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                envText, { envText = it },
                label = { Text("Environment (KEY=VALUE per line)") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
            )
            DropdownField("Python version", vm.versions, version) { version = it }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Run in foreground (keep alive when minimised)", modifier = Modifier.weight(1f))
                Switch(checked = fg, onCheckedChange = { fg = it })
            }
            OutlinedTextField(port, { port = it.filter { c -> c.isDigit() } }, label = { Text("Port (0 = auto)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
            PrimaryButton("Save") {
                vm.update {
                    it.copy(
                        entryPoint = entry.trim(),
                        arguments = args.trim(),
                        workingDir = workdir.trim(),
                        environment = parseEnv(envText),
                        pythonVersion = version,
                        runInForeground = fg,
                        port = port.toIntOrNull() ?: 0,
                    )
                }
                vm.save()
                scope.launch {
                    snackbar.showSnackbar("Saved")
                    nav.popBackStack()
                }
            }
        }
    }
}

private fun parseEnv(text: String): Map<String, String> = text.lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() && it.contains("=") }
    .associate { line ->
        val i = line.indexOf('=')
        line.substring(0, i).trim() to line.substring(i + 1).trim()
    }
