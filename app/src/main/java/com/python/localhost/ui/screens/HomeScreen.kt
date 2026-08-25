package com.python.localhost.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.python.RunState
import com.python.localhost.ui.navigation.Routes

/**
 * Android-Studio "Welcome" style home: quick-start actions on the left,
 * recent projects on the right, status footer.
 */
@Composable
fun HomeScreen(nav: NavHostController, container: AppContainer) {
    val states by container.processManager.states.collectAsState()
    val runningCount = states.values.count {
        it.state == RunState.RUNNING || it.state == RunState.STARTING
    }
    val projects = container.projectManager.listProjects()

    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            // Title bar
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(
                    Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "PyMobile IDE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (runningCount > 0) "● $runningCount running" else "idle",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (runningCount > 0) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(Modifier.weight(1f)) {
                // Left: quick-start actions (AS welcome list)
                Column(
                    Modifier.weight(0.42f).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "Quick Start",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WelcomeAction("New Project", "Create from a template", Icons.Filled.Add) {
                        nav.navigate(Routes.CREATE_PROJECT)
                    }
                    WelcomeAction("Import from GitHub", "Clone a repository", Icons.Filled.CloudDownload) {
                        nav.navigate(Routes.GITHUB_IMPORT)
                    }
                    WelcomeAction("Projects", "${projects.size} on device", Icons.Filled.Folder) {
                        nav.navigate(Routes.PROJECTS)
                    }
                    WelcomeAction("Running", "$runningCount active", Icons.Filled.PlayArrow) {
                        nav.navigate(Routes.RUNNING)
                    }
                    WelcomeAction("Settings", "Editor, pip, tokens", Icons.Filled.Settings) {
                        nav.navigate(Routes.SETTINGS)
                    }
                    WelcomeAction("Self-test", "Check Python, run & pip", Icons.Filled.BugReport) {
                        nav.navigate(Routes.DIAGNOSTICS)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Python ${container.runtimeManager.embeddedVersion()} • runs fully on-device",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Right: recent projects
                Column(Modifier.weight(0.58f).fillMaxSize().padding(16.dp)) {
                    Text(
                        "Recent Projects",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (projects.isEmpty()) {
                        Text(
                            "No projects yet.\nUse New Project or import from GitHub.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(projects, key = { it.id }) { p ->
                                val st = states[p.id]
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        nav.navigate(Routes.dashboard(p.id))
                                    },
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                p.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                            Text(
                                                p.path.substringAfterLast("/").let { "$it • py ${p.pythonVersion}" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                            )
                                        }
                                        if (st != null && (st.state == RunState.RUNNING || st.state == RunState.STARTING)) {
                                            Text(
                                                "RUNNING",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                            )
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeAction(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
