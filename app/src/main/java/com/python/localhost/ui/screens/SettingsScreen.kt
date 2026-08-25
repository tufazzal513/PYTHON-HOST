package com.python.localhost.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.ui.components.IdeCard
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.SectionTitle
import com.python.localhost.ui.provideVm
import com.python.localhost.ui.viewmodel.SettingsVM
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.Slider

@Composable
fun SettingsScreen(nav: NavHostController, container: AppContainer) {
    val vm: SettingsVM = provideVm(container) { SettingsVM(it) }
    val settings by vm.settings.collectAsState()
    val ctx = LocalContext.current
    var token by remember { mutableStateOf(vm.getToken() ?: "") }

    Scaffold(
        topBar = { IdeTopBar(title = "Settings", onBack = { nav.popBackStack() }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IdeCard {
                Column {
                    SectionTitle("Editor")
                    Text("Font size: ${settings.fontSize}sp", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = settings.fontSize.toFloat(),
                        onValueChange = { v -> vm.update { it.copy(fontSize = v.toInt().coerceIn(10, 22)) } },
                        valueRange = 10f..22f,
                    )
                    SettingSwitch("Word wrap", settings.wordWrap) { checked -> vm.update { it.copy(wordWrap = checked) } }
                    SettingSwitch("Show line numbers", settings.showLineNumbers) { checked -> vm.update { it.copy(showLineNumbers = checked) } }
                    SettingSwitch("Auto save", settings.autoSave) { checked -> vm.update { it.copy(autoSave = checked) } }
                }
            }

            IdeCard {
                Column {
                    SectionTitle("Dependency installation")
                    OutlinedTextField(
                        value = settings.pipIndexUrl,
                        onValueChange = { v -> vm.update { it.copy(pipIndexUrl = v) } },
                        label = { Text("pip index URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = settings.pipExtraIndexUrl,
                        onValueChange = { v -> vm.update { it.copy(pipExtraIndexUrl = v) } },
                        label = { Text("pip extra-index URL (Android wheels)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Text(
                        "Only packages with Android-compatible wheels install. Native packages are limited.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            IdeCard {
                Column {
                    SectionTitle("GitHub authentication")
                    OutlinedTextField(
                        value = token,
                        onValueChange = {
                            token = it
                            vm.setToken(if (it.isBlank()) null else it)
                        },
                        label = { Text("GitHub token (push/pull)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Stored encrypted on device (Android Keystore). Never written to logs or project files.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            IdeCard {
                Column {
                    SectionTitle("Battery optimisation")
                    Text(
                        "Long-running bots/servers may be stopped by Android. Grant unrestricted battery usage for reliability.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    androidx.compose.material3.Button(onClick = {
                        try {
                            val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
                            if (!pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
                                ctx.startActivity(
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                        .setData(Uri.parse("package:${ctx.packageName}")),
                                )
                            }
                        } catch (_: Exception) { }
                    }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text("Open battery settings")
                    }
                }
            }

            IdeCard {
                Column {
                    SectionTitle("About")
                    Text(
                        "PyMobile IDE runs an embedded CPython interpreter (Chaquopy). Python runs on-device; " +
                            "no cloud server is required. v1 bundles Python 3.11. Public GitHub repos clone without auth.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}
