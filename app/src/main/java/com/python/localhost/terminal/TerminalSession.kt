package com.python.localhost.terminal

import com.python.localhost.python.PythonRuntime
import com.python.localhost.python.PyOutputListener
import com.python.localhost.util.SecretMasker

enum class LineKind { INPUT, OUTPUT, ERROR, SYSTEM, META }
data class TerminalLine(val text: String, val kind: LineKind)

/**
 * A Python REPL "terminal". Real Python code is executed by the embedded interpreter;
 * a few `!` meta-commands provide project-oriented shortcuts (help, clear, pip).
 *
 * This is intentionally NOT a Linux shell — the embedded interpreter has no OS shell.
 * That limitation is communicated clearly in the UI and help text.
 */
class TerminalSession(
    private val runtime: PythonRuntime,
    private val siteDirs: List<String>,
    private val pipTargetDir: String,
    private val onLine: (TerminalLine) -> Unit,
    private val onClear: () -> Unit,
) {
    fun submit(command: String) {
        val trimmed = command.trim()
        if (trimmed.isEmpty()) return
        if (trimmed.startsWith("!")) {
            handleMeta(trimmed)
            return
        }
        onLine(TerminalLine(trimmed, LineKind.INPUT))
        val listener = PyOutputListener { text -> onLine(TerminalLine(SecretMasker.mask(text), LineKind.OUTPUT)) }
        runtime.replEval(command, siteDirs, listener)
    }

    private fun handleMeta(cmd: String) {
        val parts = cmd.removePrefix("!").split("\\s+".toRegex(), limit = 2)
        val name = parts[0]
        val arg = parts.getOrNull(1) ?: ""
        when (name) {
            "help" -> onLine(TerminalLine(META_HELP, LineKind.META))
            "clear" -> onClear()
            "pip" -> runPip(arg)
            else -> onLine(TerminalLine("Unknown command: !$name (try !help)", LineKind.ERROR))
        }
    }

    private fun runPip(arg: String) {
        onLine(TerminalLine("!pip $arg", LineKind.INPUT))
        val listener = PyOutputListener { text -> onLine(TerminalLine(SecretMasker.mask(text), LineKind.OUTPUT)) }
        runtime.pipInstall(
            pipTargetDir,
            arg.split("\\s+".toRegex()).filter { it.isNotEmpty() },
            null, null, listener,
        )
    }

    companion object {
        const val META_HELP = "PyMobile Console — a Python REPL. Type Python code and press enter.\n" +
            "Meta commands:\n" +
            "  !help          show this help\n" +
            "  !clear         clear the screen\n" +
            "  !pip <args>    run pip (e.g. !pip install requests)\n" +
            "Note: this is a Python console, not a full Linux shell."
    }
}
