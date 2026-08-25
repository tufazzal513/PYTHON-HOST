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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.python.localhost.di.AppContainer
import com.python.localhost.python.PyOutputListener
import com.python.localhost.ui.components.IdeCard
import com.python.localhost.ui.components.IdeTopBar
import com.python.localhost.ui.components.PrimaryButton
import com.python.localhost.ui.components.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class DiagResult(val name: String, val ok: Boolean, val detail: String)

/**
 * In-app self-test: verifies that the embedded Python interpreter works, that a
 * script can actually run and produce output, and that pip can download and
 * install a package from the Chaquopy Android wheel repository. The report can
 * be copied and shared for remote debugging.
 */
@Composable
fun DiagnosticsScreen(nav: NavHostController, container: AppContainer) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val results = remember { mutableStateListOf<DiagResult>() }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    var logcatText by remember { mutableStateOf("") }
    var crashText by remember { mutableStateOf("") }

    fun report(): String = buildString {
        appendLine("PyMobile IDE self-test report")
        appendLine("device: Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})")
        appendLine("abi: ${android.os.Build.SUPPORTED_ABIS.joinToString(",")}")
        appendLine("python-runtime: ${try { container.pythonRuntime.getVersion() } catch (e: Exception) { "ERROR: ${e.message}" }}")
        appendLine("---")
        results.forEach {
            appendLine("[${if (it.ok) "PASS" else "FAIL"}] ${it.name}")
            if (it.detail.isNotBlank()) appendLine(it.detail)
        }
        appendLine("---")
        if (crashText.isNotBlank()) {
            appendLine("CRASH LOG (last):")
            appendLine(crashText.takeLast(4000))
        }
        if (logcatText.isNotBlank()) {
            appendLine("LOGCAT (last 250 lines):")
            appendLine(logcatText.lines().takeLast(250).joinToString("\n"))
        }
    }

    var copied by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { IdeTopBar(title = "Diagnostics", subtitle = "Verify Python, run & pip on this device", onBack = { nav.popBackStack() }) },
        bottomBar = {
            if (finished) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                    PrimaryButton(
                        text = if (copied) "Copied ✓ — paste it in chat" else "Copy full report (tests + logs)",
                        modifier = Modifier.padding(10.dp),
                    ) {
                        clipboard.setText(AnnotatedString(report()))
                        copied = true
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PrimaryButton(
                text = if (running) "Running self-test…" else if (finished) "Re-run self-test" else "Run self-test",
                enabled = !running,
            ) {
                running = true
                finished = false
                results.clear()
                scope.launch(Dispatchers.IO) {
                    try {
                        // 1. Interpreter
                        val started = try {
                            container.pythonRuntime.isStarted()
                        } catch (e: Exception) {
                            results.add(DiagResult("Python interpreter started", false, "exception: ${e.message}"))
                            false
                        }
                        if (started) {
                            val ver = try {
                                container.pythonRuntime.getVersion()
                            } catch (e: Exception) {
                                "unknown (${e.message})"
                            }
                            results.add(DiagResult("Python interpreter started", true, "version: $ver"))
                        }

                        // 2. Run a real script and capture output
                        val dir = File(ctx.cacheDir, "selftest").apply { mkdirs() }
                        val script = File(dir, "selftest.py")
                        script.writeText(
                            "import sys, json\n" +
                                "print('SELFTEST_OK', '%d.%d' % sys.version_info[:2], json.dumps({'wheels': 'working'}))\n"
                        )
                        val out = StringBuilder()
                        val rc = try {
                            container.pythonRuntime.runScript(
                                script.absolutePath, emptyList(), emptyMap(),
                                listOf(dir.absolutePath),
                                PyOutputListener { out.append(it) },
                                dir.absolutePath,
                            )
                        } catch (e: Exception) {
                            out.append("\nEXCEPTION: ").append(e.message)
                            -1
                        }
                        results.add(
                            DiagResult(
                                "Run script (print + import json)",
                                rc == 0 && out.contains("SELFTEST_OK"),
                                "exit code: $rc\noutput: ${out.toString().trim().take(500)}",
                            )
                        )

                        // 3. pip: install a tiny pure-Python package from the Android wheel repo
                        val pipOut = StringBuilder()
                        val target = File(dir, "site")
                        val pipRc = try {
                            container.pythonRuntime.pipInstall(
                                target.absolutePath, listOf("six"), null, null,
                                PyOutputListener { pipOut.append(it) },
                            )
                        } catch (e: Exception) {
                            pipOut.append("\nEXCEPTION: ").append(e.message)
                            -1
                        }
                        val installed = target.isDirectory &&
                            (target.listFiles()?.isNotEmpty() == true || pipOut.contains("Successfully installed"))
                        results.add(
                            DiagResult(
                                "pip install from Chaquopy Android repo",
                                pipRc == 0 && installed,
                                "exit code: $pipRc\noutput tail: ${pipOut.toString().trim().takeLast(700)}",
                            )
                        )
                    } catch (e: Throwable) {
                        results.add(DiagResult("Self-test crashed", false, e.message ?: e.toString()))
                    } finally {
                        // A-to-Z logs: own-process logcat + persisted crash log
                        logcatText = try {
                            val proc = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "400"))
                            proc.inputStream.readBytes().toString(Charsets.UTF_8)
                        } catch (e: Exception) {
                            "(logcat unavailable: ${e.message})"
                        }
                        crashText = try {
                            val f = File(ctx.filesDir, "crash.log")
                            if (f.exists()) f.readText() else ""
                        } catch (e: Exception) {
                            "(crash log unavailable: ${e.message})"
                        }
                        running = false
                        finished = true
                    }
                }
            }

            if (results.isEmpty() && !running) {
                Text(
                    "This checks, on THIS device:\n" +
                        "1. Python interpreter initialised\n" +
                        "2. A script actually runs and prints\n" +
                        "3. pip can install packages (network + SSL + Android wheels)\n\n" +
                        "If anything fails, tap 'Copy report' and send it — the exact error will be in it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            results.forEach { r ->
                IdeCard {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            (if (r.ok) "✓ " else "✗ ") + r.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (r.ok) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        r.detail,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (finished) {
                if (crashText.isNotBlank()) {
                    SectionTitle("Crash log (persisted)")
                    IdeCard {
                        Text(
                            crashText.takeLast(3000),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Clear crash log",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                File(ctx.filesDir, "crash.log").delete()
                                crashText = ""
                            }.padding(4.dp),
                        )
                    }
                }
                if (logcatText.isNotBlank()) {
                    SectionTitle("Device log (logcat, this app only)")
                    IdeCard {
                        Text(
                            logcatText.lines().takeLast(250).joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "Use the Copy button at the bottom of the screen, then paste it in chat.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
