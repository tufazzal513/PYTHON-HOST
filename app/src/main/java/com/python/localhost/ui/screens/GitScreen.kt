package com.python.localhost.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Default.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.ui.components.DropdownField
import com.python.localhost.ui.components.ErrorText
import com.python.localhost.ui.components.IdeCard
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.PrimaryButton
import com.python.localhost.ui.components.SecondaryButton
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.GitVM
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

@Composable
fun GitScreen(nav: NavHostController, container: AppContainer, projectId: String) {
    val vm: GitVM = provideVm(container) { GitVM(it, projectId) }
    val status by vm.status.collectAsState()
    val branches by vm.branches.collectAsState()
    val message by vm.message.collectAsState()
    val result by vm.result.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var newBranch by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = {
            IdeTopBar(
                title = "Git — ${vm.meta.name}",
                onBack = { nav.popBackStack() },
                actions = { IconButton(onClick = { vm.refresh() }) { Icon(Icons.Default.Refresh, "Refresh") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val st = status
            if (st != null && st.error == null) {
                IdeCard {
                    InfoRow("Branch", st.branch ?: "-")
                    InfoRow("Clean", if (st.isClean) "yes" else "no")
                    if (st.added.isNotEmpty()) InfoRow("Added", st.added.joinToString(", "))
                    if (st.modified.isNotEmpty()) InfoRow("Modified", st.modified.joinToString(", "))
                    if (st.deleted.isNotEmpty()) InfoRow("Deleted", st.deleted.joinToString(", "))
                    if (st.untracked.isNotEmpty()) InfoRow("Untracked", st.untracked.joinToString(", "))
                }
            } else {
                ErrorText(st?.error ?: "This project is not a git repository (clone a GitHub repo or initialise one to use Git features).")
            }

            OutlinedTextField(
                value = message,
                onValueChange = { vm.setMessage(it) },
                label = { Text("Commit message") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("Commit", modifier = Modifier.weight(1f)) { vm.commit() }
                SecondaryButton("Push", modifier = Modifier.weight(1f)) { vm.push(null) }
                SecondaryButton("Pull", modifier = Modifier.weight(1f)) { vm.pull(null) }
            }

            DropdownField(
                "Branch",
                branches.ifEmpty { listOf("(none)") },
                branches.firstOrNull() ?: "(none)",
            ) { if (it != "(none)") vm.checkout(it) }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newBranch,
                    onValueChange = { newBranch = it },
                    label = { Text("New branch") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton("Create") { vm.createBranch(newBranch) }
            }

            result?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
    }
}
