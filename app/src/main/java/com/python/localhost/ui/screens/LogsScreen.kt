package com.python.localhost.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Default.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.SectionTitle
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.LogsVM
import androidx.compose.runtime.collectAsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsScreen(nav: NavHostController, container: AppContainer, projectId: String) {
    val vm: LogsVM = provideVm(container) { LogsVM(it, projectId) }
    val category by vm.category.collectAsState()
    val content = remember(category) { vm.content() }
    val history = remember { vm.history() }

    Scaffold(
        topBar = {
            IdeTopBar(
                title = "Logs — ${vm.meta.name}",
                onBack = { nav.popBackStack() },
                actions = {
                    IconButton(onClick = { vm.clear() }) { Icon(Icons.Default.Delete, "Clear") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(vm.categories) { c ->
                    FilterChip(
                        selected = c == category,
                        onClick = { vm.select(c) },
                        label = { Text(c.name.lowercase()) },
                    )
                }
            }
            if (history.isNotEmpty()) {
                SectionTitle("Run history")
                history.take(20).forEach { h ->
                    val start = SimpleDateFormat("dd MMM HH:mm", Locale.US).format(Date(h.startedAt))
                    Text("• $start  ${h.status}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SectionTitle("${category.name.lowercase()} log")
            Surface(
                Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp)
                    .verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    if (content.isBlank()) "No log entries." else content,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
