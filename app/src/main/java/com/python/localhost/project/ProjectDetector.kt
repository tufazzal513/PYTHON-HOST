package com.python.localhost.project

import com.python.localhost.data.DetectionResult
import com.python.localhost.util.FileUtils
import java.io.File

/**
 * Inspects a project's files to automatically detect entry points, dependency
 * configuration, frameworks, databases, git, and README.
 */
class ProjectDetector {

    private val KNOWN_ENTRY = setOf("main.py", "app.py", "bot.py", "run.py", "server.py", "__main__.py")

    fun detect(dir: File): DetectionResult {
        val files = FileUtils.listFilesRecursive(dir)
        val pyFiles = files.filter { it.extension == "py" }
        val entryCandidates = pyFiles.map { FileUtils.relativePath(dir, it) }
            .filter { KNOWN_ENTRY.contains(it.substringAfterLast("/")) || KNOWN_ENTRY.contains(it) }
            .ifEmpty { pyFiles.map { FileUtils.relativePath(dir, it) }.take(20) }

        val hasReq = files.any { it.name == "requirements.txt" }
        val hasPyproject = files.any { it.name == "pyproject.toml" }
        val hasSetup = files.any { it.name == "setup.py" }
        val hasEnv = files.any { it.name == ".env" }
        val hasReadme = files.any { it.name.startsWith("readme", ignoreCase = true) }
        val hasSqlite = files.any {
            it.extension == "db" || it.extension == "sqlite" || it.extension == "sqlite3"
        }
        val isGit = File(dir, ".git").exists()

        val frameworks = mutableListOf<String>()
        val reqFile = files.firstOrNull { it.name == "requirements.txt" }
        val content = buildString {
            reqFile?.let { append(it.readText()); append("\n") }
            pyFiles.take(10).forEach { append(it.readText()); append("\n") }
        }
        if (Regex("""\bflask\b""", RegexOption.IGNORE_CASE).containsMatchIn(content)) frameworks += "Flask"
        if (Regex("""\bfastapi\b""", RegexOption.IGNORE_CASE).containsMatchIn(content)) frameworks += "FastAPI"
        if (Regex("""python-telegram-bot|telebot|aiogram""", RegexOption.IGNORE_CASE).containsMatchIn(content)) frameworks += "Telegram Bot"
        if (Regex("""discord\.py|\bdiscord\b""", RegexOption.IGNORE_CASE).containsMatchIn(content)) frameworks += "Discord Bot"

        val detected = entryCandidates.firstOrNull()
        val notes = mutableListOf<String>()
        if (entryCandidates.size > 1) notes += "Multiple entry points detected — choose one in Run Configuration."
        if (hasSqlite) notes += "SQLite database detected — treated as a project file."

        return DetectionResult(
            entryCandidates = entryCandidates,
            detectedEntry = detected,
            hasRequirementsTxt = hasReq,
            hasPyprojectToml = hasPyproject,
            hasSetupPy = hasSetup,
            hasEnv = hasEnv,
            frameworks = frameworks,
            hasSqlite = hasSqlite,
            hasReadme = hasReadme,
            isGitRepo = isGit,
            notes = notes,
        )
    }
}
