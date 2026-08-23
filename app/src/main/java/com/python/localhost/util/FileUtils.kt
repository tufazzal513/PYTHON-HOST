package com.python.localhost.util

import java.io.File

object FileUtils {

    fun listFilesRecursive(dir: File): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        val result = mutableListOf<File>()
        dir.walkTopDown().forEach { if (it.isFile) result.add(it) }
        return result
    }

    fun relativePath(base: File, file: File): String =
        file.absolutePath.removePrefix(base.absolutePath + File.separator)

    fun humanSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var i = 0
        while (size >= 1024 && i < units.lastIndex) {
            size /= 1024
            i++
        }
        return "%.1f %s".format(size, units[i])
    }

    private val TEXT_EXTS = setOf(
        "py", "pyw", "txt", "json", "xml", "yaml", "yml", "ini", "env", "md", "csv",
        "html", "htm", "css", "js", "ts", "sql", "toml", "cfg", "log", "sh", "gradle",
        "kt", "kts", "properties", "lock", "gitignore", "editorconfig", "rst", "tex",
    )

    fun isTextFile(file: File): Boolean {
        val name = file.name.lowercase()
        if (name == ".env" || name == ".gitignore" || name == "dockerfile" || name.startsWith(".env")) {
            return true
        }
        return TEXT_EXTS.contains(file.extension.lowercase())
    }

    fun isImageFile(file: File): Boolean =
        setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg").contains(file.extension.lowercase())
}
