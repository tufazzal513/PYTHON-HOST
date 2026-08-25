package com.python.localhost.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.git.GitHubUrlParser
import com.python.localhost.ui.components.DropdownField
import com.python.localhost.ui.components.EmptyState
import com.python.localhost.ui.components.ErrorText
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.LoadingBox
import com.python.localhost.ui.components.PrimaryButton
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.GitHubImportState
import com.python.localhost.ui.viewmodel.GitHubImportVM
import androidx.compose.runtime.collectAsState

@Composable
fun GitHubImportScreen(nav: NavHostController, container: AppContainer) {
    val vm: GitHubImportVM = provideVm(container) { GitHubImportVM(it) }
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf(vm.versions.firstOrNull() ?: "3.11") }
    val state by vm.state.collectAsState()

    LaunchedEffect(state) {
        if (state is GitHubImportState.Success) {
            nav.navigate(Routes.dashboard((state as GitHubImportState.Success).id)) {
                popUpTo(Routes.GITHUB_IMPORT) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = { IdeTopBar(title = "Import from GitHub", onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    if (name.isBlank()) name = GitHubUrlParser.parse(it)?.repo ?: ""
                },
                label = { Text("Repository URL") },
                placeholder = { Text("https://github.com/user/repo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Project name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownField("Python version", vm.versions, version) { version = it }

            when (val s = state) {
                is GitHubImportState.Importing -> LoadingBox()
                is GitHubImportState.Error -> ErrorText(s.msg)
                is GitHubImportState.Success -> EmptyState(s.msg)
                else -> { }
            }

            PrimaryButton(
                "Import",
                enabled = url.isNotBlank() && state !is GitHubImportState.Importing,
            ) { vm.import(url, name, version) }
        }
    }
}
