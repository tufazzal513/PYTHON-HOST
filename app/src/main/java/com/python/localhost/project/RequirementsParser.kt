package com.python.localhost.project

import java.io.File

/**
 * Best-effort parser for Python dependency declarations across the three common formats:
 * requirements.txt, pyproject.toml ([project] dependencies), and setup.py (install_requires).
 * Only the simple, common cases are handled — enough to drive embedded pip.
 */
object RequirementsParser {

    fun fromRequirementsTxt(dir: File): List<String> {
        val f = File(dir, "requirements.txt")
        if (!f.exists()) return emptyList()
        return f.readLines().map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("-") }
    }

    fun fromPyprojectToml(dir: File): List<String> {
        val f = File(dir, "pyproject.toml")
        if (!f.exists()) return emptyList()
        return extractListBlock(f.readText(), "dependencies")
    }

    fun fromSetupPy(dir: File): List<String> {
        val f = File(dir, "setup.py")
        if (!f.exists()) return emptyList()
        return extractListBlock(f.readText(), "install_requires")
    }

    /** Combine and de-duplicate requirements from all supported sources. */
    fun collect(dir: File): List<String> =
        (fromRequirementsTxt(dir) + fromPyprojectToml(dir) + fromSetupPy(dir)).distinct()

    private fun extractListBlock(text: String, key: String): List<String> {
        val start = text.indexOf(key)
        if (start < 0) return emptyList()
        val eq = text.indexOf('=', start)
        if (eq < 0) return emptyList()
        val open = text.indexOf('[', eq)
        if (open < 0) return emptyList()
        var depth = 0
        var i = open
        var close = -1
        while (i < text.length) {
            when (text[i]) {
                '[' -> depth++
                ']' -> { depth--; if (depth == 0) { close = i; break } }
            }
            i++
        }
        if (close < 0) return emptyList()
        return text.substring(open + 1, close)
            .split(',')
            .map { it.trim() }
            .map { it.trim('"', '\'', ' ', '\n', '\r', '\t') }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
    }
}
