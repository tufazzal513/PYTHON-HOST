package com.python.localhost.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.ui.components.EmptyState
import com.python.localhost.ui.components.IdeCard
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.EntryPickerVM

@Composable
fun EntryPointPickerScreen(nav: NavHostController, container: AppContainer, projectId: String) {
    val vm: EntryPickerVM = provideVm(container) { EntryPickerVM(it, projectId) }
    val candidates = vm.candidates

    Scaffold(
        topBar = { IdeTopBar(title = "Choose entry point", onBack = { nav.popBackStack() }) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(12.dp),
        ) {
            item { Text("Detected Python entry points:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(8.dp)) }
            items(candidates) { candidate ->
                IdeCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                    vm.setEntry(candidate)
                    nav.popBackStack()
                }) {
                    Text(candidate, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (candidates.isEmpty()) {
                item { EmptyState("No Python files were found in this project.") }
            }
        }
    }
}
