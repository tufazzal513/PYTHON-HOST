package com.python.localhost.logs

import com.python.localhost.core.AppDirs
import com.python.localhost.core.JsonStore
import com.python.localhost.data.RunHistoryEntry
import com.python.localhost.util.SecretMasker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LogCategory { RUN, OUTPUT, ERROR, DEPENDENCY, GIT, SYSTEM }

/**
 * Per-project, per-category logs with simple size-based rotation. Kept small so logs
 * never grow without bound. Secrets are masked by callers before appending.
 */
class LogManager(private val appDirs: AppDirs, private val json: JsonStore) {
    private val maxBytes = 500_000L

    private fun file(projectId: String, category: LogCategory): File {
        val dir = appDirs.logDir(projectId)
        return File(dir, "${category.name.lowercase()}.log")
    }

    fun append(projectId: String, category: LogCategory, message: String) {
        val f = file(projectId, category)
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        synchronized(this) {
            f.appendText("[${ts}] ${SecretMasker.mask(message)}\n")
            rotate(f)
        }
    }

    fun read(projectId: String, category: LogCategory): String =
        if (file(projectId, category).exists()) file(projectId, category).readText() else ""

    fun clear(projectId: String, category: LogCategory) = file(projectId, category).writeText("")

    fun clearAll(projectId: String) = LogCategory.values().forEach { clear(projectId, it) }

    private fun rotate(f: File) {
        if (f.length() > maxBytes) {
            val lines = f.readLines()
            val keep = lines.takeLast((lines.size * 0.7).toInt())
            f.writeText(keep.joinToString("\n") + "\n")
        }
    }

    // ---- Run history ----
    private fun historyFile(projectId: String): File =
        File(appDirs.logDir(projectId), "run_history.json")

    fun addRunHistory(entry: RunHistoryEntry) {
        val list = getRunHistory(entry.projectId).toMutableList()
        list.add(0, entry)
        json.write(historyFile(entry.projectId), list.take(100))
    }

    @Suppress("UNCHECKED_CAST")
    fun getRunHistory(projectId: String): List<RunHistoryEntry> {
        val f = historyFile(projectId)
        if (!f.exists()) return emptyList()
        return try {
            (json.read(f, Array<RunHistoryEntry>::class.java)?.toList() ?: emptyList())
        } catch (e: Exception) {
            emptyList()
        }
    }
}
