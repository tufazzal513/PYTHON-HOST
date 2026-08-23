package com.python.localhost.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Default.Add
import androidx.compose.material.icons.Default.CloudDownload
import androidx.compose.material.icons.Default.Delete
import androidx.compose.material.icons.Default.Folder
import androidx.compose.material.icons.Default.FolderOpen
import androidx.compose.material.icons.Default.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavHostController
import com.python.localhost.data.ProjectMeta
import com.python.localhost.data.RunConfig
import com.python.localhost.di.AppContainer
import com.python.localhost.process.RunHelper
import com.python.localhost.process.RunState
import com.python.localhost.ui.components.ConfirmDialog
import com.python.localhost.ui.components.EmptyState
import com.python.localhost.ui.components.IdeCard
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.StatusChip
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.ProjectsVM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.compose.runtime.collectAsState

@Composable
fun ProjectsScreen(nav: NavHostController, container: AppContainer) {
    val vm: ProjectsVM = provideVm(container) { ProjectsVM(it) }
    val projects by vm.projects.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val version = container.runtimeManager.embeddedVersion()
    LaunchedEffect(Unit) { vm.load() }
    var pendingDelete by remember { mutableStateOf<ProjectMeta?>(null) }

    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Exception) { }
        scope.launch(Dispatchers.IO) {
            try {
                val temp = File(container.appDirs.cache, "import_${System.currentTimeMillis()}")
                temp.mkdirs()
                container.storageManager.copyTreeToLocal(uri, temp)
                val name = DocumentFile.fromTreeUri(ctx, uri)?.name ?: "imported"
                val meta = container.projectManager.finalizeImported(name, version, temp)
                withContext(Dispatchers.Main) { nav.navigate(Routes.dashboard(meta.id)) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { scope.launch { snackbar.showSnackbar("Import failed: ${e.message}") } }
            }
        }
    }

    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                val bytes = container.storageManager.readFromDocument(uri)
                val name = DocumentFile.fromSingleUri(ctx, uri)?.name?.removeSuffix(".zip") ?: "imported"
                val meta = container.projectManager.importZip(bytes, name, version)
                withContext(Dispatchers.Main) { nav.navigate(Routes.dashboard(meta.id)) }
            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) { scope.launch { snackbar.showSnackbar("ZIP blocked: unsafe path (${e.message})") } }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { scope.launch { snackbar.showSnackbar("Import failed: ${e.message}") } }
            }
        }
    }

    Scaffold(
        topBar = {
            IdeTopBar(
                title = "Projects",
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.CREATE_PROJECT) }) { Icon(Icons.Default.Add, "New") }
                    IconButton(onClick = { nav.navigate(Routes.GITHUB_IMPORT) }) { Icon(Icons.Default.CloudDownload, "GitHub") }
                    IconButton(onClick = { zipLauncher.launch(arrayOf("application/zip")) }) { Icon(Icons.Default.Folder, "Import ZIP") }
                    IconButton(onClick = { treeLauncher.launch(null) }) { Icon(Icons.Default.FolderOpen, "Import Folder") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(projects, key = { it.id }) { meta ->
                ProjectCardItem(meta, container, nav, onDelete = { pendingDelete = meta })
            }
            if (projects.isEmpty()) {
                item { EmptyState("No projects yet. Create one or import a folder / ZIP / GitHub repo.") }
            }
        }
    }

    pendingDelete?.let { meta ->
        ConfirmDialog(
            title = "Delete project?",
            message = "This will permanently delete '${meta.name}' and all its files. This cannot be undone.",
            confirmText = "Delete",
            onConfirm = { container.projectManager.deleteProject(meta.id); pendingDelete = null; vm.load() },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ProjectCardItem(
    meta: ProjectMeta,
    container: AppContainer,
    nav: NavHostController,
    onDelete: () -> Unit,
) {
    val ctx = LocalContext.current
    val states by container.processManager.states.collectAsState()
    val st = states[meta.id]
    IdeCard {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(meta.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                StatusChip(meta.pythonVersion, MaterialTheme.colorScheme.primary)
            }
            Text(meta.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { nav.navigate(Routes.dashboard(meta.id)) }) { Text("Open") }
                Button(onClick = {
                    val config = container.projectManager.getRunConfig(File(meta.path))
                    RunHelper.buildAndStart(ctx, container, meta, config, config.runInForeground)
                    nav.navigate(Routes.dashboard(meta.id)) { popUpTo(Routes.PROJECTS) {} }
                }) { Text("Run") }
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Delete, "Delete") }
            }
            if (st != null) {
                Spacer(Modifier.height(4.dp))
                val color = when (st.state) {
                    RunState.RUNNING -> MaterialTheme.colorScheme.secondary
                    RunState.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                StatusChip(st.state.name, color)
            }
        }
    }
}
