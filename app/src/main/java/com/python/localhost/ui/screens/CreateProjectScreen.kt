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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.ui.components.DropdownField
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.PrimaryButton
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.CreateProjectVM

@Composable
fun CreateProjectScreen(nav: NavHostController, container: AppContainer) {
    val vm: CreateProjectVM = provideVm(container) { CreateProjectVM(it) }
    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf(vm.versions.firstOrNull() ?: "3.11") }
    var template by remember { mutableStateOf("basic") }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { IdeTopBar(title = "New Project", onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Project name") },
                isError = error != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
            DropdownField("Python version", vm.versions, version) { version = it }
            DropdownField("Template", vm.templates, template) { template = it }
            PrimaryButton("Create Project") {
                val meta = vm.create(name, version, template)
                if (meta == null) {
                    error = "Project name is required."
                } else {
                    nav.navigate(Routes.dashboard(meta.id)) { popUpTo(Routes.CREATE_PROJECT) { inclusive = true } }
                }
            }
        }
    }
}
