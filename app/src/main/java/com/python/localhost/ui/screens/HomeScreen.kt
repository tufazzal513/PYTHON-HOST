package com.python.localhost.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Default.Add
import androidx.compose.material.icons.Default.CloudDownload
import androidx.compose.material.icons.Default.Folder
import androidx.compose.material.icons.Default.PlayArrow
import androidx.compose.material.icons.Default.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.process.RunState
import com.python.localhost.ui.components.IdeCard
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.navigation.Routes
import androidx.compose.runtime.collectAsState

@Composable
fun HomeScreen(nav: NavHostController, container: AppContainer) {
    val states by container.processManager.states.collectAsState()
    val runningCount = states.values.count { it.state == RunState.RUNNING || it.state == RunState.STARTING }
    val projects = remember { container.projectManager.listProjects() }

    Scaffold(
        topBar = { IdeTopBar(title = "PyMobile IDE", subtitle = "Native Android Python IDE") },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Home", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
            }
            item { HomeCard("Projects", "${projects.size} project(s)", Icons.Default.Folder) { nav.navigate(Routes.PROJECTS) } }
            item { HomeCard("Running", "$runningCount running", Icons.Default.PlayArrow) { nav.navigate(Routes.RUNNING) } }
            item { HomeCard("Create Project", "New Python project from a template", Icons.Default.Add) { nav.navigate(Routes.CREATE_PROJECT) } }
            item { HomeCard("Import from GitHub", "Clone a public repository", Icons.Default.CloudDownload) { nav.navigate(Routes.GITHUB_IMPORT) } }
            item { HomeCard("Settings", "App & runtime settings", Icons.Default.Settings) { nav.navigate(Routes.SETTINGS) } }
        }
    }
}

@Composable
private fun HomeCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    IdeCard(Modifier.clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
