package com.python.localhost.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.data.RunHistoryEntry
import com.python.localhost.di.AppContainer
import com.python.localhost.ui.components.IdeCard
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.PrimaryButton
import com.python.localhost.ui.components.SectionTitle
import com.python.localhost.ui.components.SecondaryButton
import com.python.localhost.ui.components.StatusChip
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.DashboardVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import java.util.UUID

@Composable
fun ProjectDashboardScreen(nav: NavHostController, container: AppContainer, projectId: String) {
    val vm: DashboardVM = provideVm(container) { DashboardVM(it, projectId) }
    val ctx = LocalContext.current
    val runState by vm.runState.collectAsState()
    val output by vm.output.collectAsState()
    val gitStatus = remember { container.gitManager.status(vm.dir) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var fg by remember { mutableStateOf(vm.config.runInForeground) }
    var installing by remember { mutableStateOf(false) }
    var installLog by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            IdeTopBar(
                title = vm.meta.name,
                subtitle = "Python ${vm.meta.pythonVersion}",
                onBack = { nav.popBackStack() },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.RUNNING) }) { Text("Running") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IdeCard {
                InfoRow("Path", vm.meta.path)
                InfoRow("Python", vm.meta.pythonVersion)
                InfoRow("Entry", vm.config.entryPoint)
                InfoRow("Git", gitStatus.branch ?: "not a repo")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Status", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(70.dp))
                    val color = when (runState?.state) {
                        com.python.localhost.python.RunState.RUNNING -> MaterialTheme.colorScheme.secondary
                        com.python.localhost.python.RunState.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    StatusChip(runState?.state?.name ?: "Ready", color)
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Foreground (keeps running when minimised)", modifier = Modifier.weight(1f))
                Switch(checked = fg, onCheckedChange = { fg = it })
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("▶ Run Project", modifier = Modifier.weight(1f)) {
                    vm.run(fg, ctx)
                    container.logManager.addRunHistory(
                        RunHistoryEntry(UUID.randomUUID().toString(), projectId, System.currentTimeMillis(), null, "RUNNING", null, null),
                    )
                }
                SecondaryButton("■ Stop", modifier = Modifier.weight(1f)) { vm.stop() }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("🔄 Restart", modifier = Modifier.weight(1f)) { vm.stop(); vm.run(fg, ctx) }
                SecondaryButton("📦 Install Deps", modifier = Modifier.weight(1f)) {
                    scope.launch(Dispatchers.IO) {
                        installing = true
                        installLog = ""
                        val r = vm.install { line -> installLog += line }
                        installing = false
                        withContext(Dispatchers.Main) { scope.launch { snackbar.showSnackbar(r.message) } }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Files", modifier = Modifier.weight(1f)) { nav.navigate(Routes.editor(projectId)) }
                SecondaryButton("Terminal", modifier = Modifier.weight(1f)) { nav.navigate(Routes.terminal(projectId)) }
                SecondaryButton("Config", modifier = Modifier.weight(1f)) { nav.navigate(Routes.runConfig(projectId)) }
                SecondaryButton("Entry", modifier = Modifier.weight(1f)) { nav.navigate(Routes.entryPicker(projectId)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton("Git", modifier = Modifier.weight(1f)) { nav.navigate(Routes.git(projectId)) }
                SecondaryButton("Logs", modifier = Modifier.weight(1f)) { nav.navigate(Routes.logs(projectId)) }
            }

            if (vm.detection.notes.isNotEmpty()) {
                IdeCard {
                    SectionTitle("Notes")
                    vm.detection.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
            if (vm.detection.frameworks.isNotEmpty()) {
                IdeCard {
                    SectionTitle("Detected frameworks")
                    Text(vm.detection.frameworks.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                }
            }

            val installed = remember { vm.installedPackages() }
            if (installed.isNotEmpty()) {
                IdeCard {
                    SectionTitle("Installed packages (project env)")
                    installed.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                }
            }

            SectionTitle("Live output")
            if (installing) {
                Surface(
                    Modifier.fillMaxWidth().heightIn(max = 200.dp).padding(bottom = 8.dp)
                        .verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(installLog, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }
            }
            Surface(
                Modifier.fillMaxWidth().heightIn(max = 260.dp)
                    .verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    if (output.isBlank()) "No output yet. Press Run Project." else output.takeLast(6000),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(70.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}
