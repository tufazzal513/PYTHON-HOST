package com.python.localhost.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.terminal.LineKind
import com.python.localhost.terminal.TerminalLine
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.navigation.Routes
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.TerminalVM
import androidx.compose.runtime.collectAsState

@Composable
fun TerminalScreen(nav: NavHostController, container: AppContainer, projectId: String) {
    val vm: TerminalVM = provideVm(container) { TerminalVM(it, projectId) }
    val lines by vm.lines.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.scrollToItem(lines.lastIndex)
    }

    Scaffold(
        topBar = {
            IdeTopBar(
                title = "Terminal — ${vm.meta.name}",
                subtitle = "Python console",
                onBack = { nav.popBackStack() },
                actions = {
                    IconButton(onClick = { vm.submit("!clear") }) { Icon(Icons.Default.Delete, "Clear") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(8.dp).horizontalScroll(rememberScrollState()),
                state = listState,
            ) {
                items(lines) { line -> TerminalLineView(line) }
            }
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("python>") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { vm.submit(input); input = "" }),
                )
                Spacer(Modifier.height(0.dp).width(8.dp))
                Button(onClick = { vm.submit(input); input = "" }) { Text("Run") }
            }
        }
    }
}

@Composable
private fun TerminalLineView(line: TerminalLine) {
    val color = when (line.kind) {
        LineKind.INPUT -> MaterialTheme.colorScheme.primary
        LineKind.OUTPUT -> MaterialTheme.colorScheme.onSurface
        LineKind.ERROR -> MaterialTheme.colorScheme.error
        LineKind.META -> MaterialTheme.colorScheme.onSurfaceVariant
        LineKind.SYSTEM -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        line.text,
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.bodySmall,
        color = color,
    )
}
