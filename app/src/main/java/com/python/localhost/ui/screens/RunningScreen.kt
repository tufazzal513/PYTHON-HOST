package com.python.localhost.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.python.RunStateInfo
import com.python.localhost.ui.components.EmptyState
import com.python.localhost.ui.components.IdeCard
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.StatusChip
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.RunningVM
import com.python.localhost.util.ResourceMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.compose.runtime.collectAsState

@Composable
fun RunningScreen(nav: NavHostController, container: AppContainer) {
    val vm: RunningVM = provideVm(container) { RunningVM(it) }
    val running by vm.running.collectAsState()
    var mem by remember { mutableStateOf(0L) }
    var cpu by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            mem = ResourceMonitor.memoryUsedMb()
            cpu = withContext(Dispatchers.Default) { ResourceMonitor.cpuPercentSample() }
            delay(2500)
        }
    }

    Scaffold(
        topBar = { IdeTopBar(title = "Running", subtitle = "Active Python processes") },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(running, key = { it.projectId }) { info ->
                RunningCard(info, vm, nav, mem, cpu, onStop = { vm.stop(info.projectId) })
            }
            if (running.isEmpty()) {
                item { EmptyState("No running projects. Start one from a project dashboard.") }
            }
        }
    }
}

@Composable
private fun RunningCard(
    info: RunStateInfo,
    vm: RunningVM,
    nav: NavHostController,
    mem: Long,
    cpu: Float?,
    onStop: () -> Unit,
) {
    IdeCard {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(info.projectName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                val color = when (info.state) {
                    com.python.localhost.python.RunState.RUNNING -> MaterialTheme.colorScheme.secondary
                    com.python.localhost.python.RunState.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                StatusChip(info.state.name, color)
            }
            Spacer(Modifier.height(6.dp))
            val uptime = System.currentTimeMillis() - info.startedAt
            Text(
                "Uptime ${formatDuration(uptime)}  •  Memory ${mem} MB  •  CPU ${cpu?.let { "%.0f%%".format(it) } ?: "n/a (app-level)"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val out = vm.output(info.projectId)
            if (out.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    out.takeLast(240),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { nav.navigate(Routes.dashboard(info.projectId)) }) { Text("Open") }
                Button(onClick = { nav.navigate(Routes.logs(info.projectId)) }) { Text("Logs") }
                Button(onClick = onStop) { Text("Stop") }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (h > 0) "%dh %02dm".format(h, m) else "%dm %02ds".format(m, s)
}
