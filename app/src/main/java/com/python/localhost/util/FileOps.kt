package com.python.localhost.util

import java.io.File

/** Filesystem helpers for the in-editor file operations (create / rename / delete). */
object FileOps {
    fun createFile(parent: File, name: String): File {
        val f = File(parent, name)
        f.parentFile?.mkdirs()
        if (!f.exists()) f.createNewFile()
        return f
    }

    fun createFolder(parent: File, name: String): File {
        val d = File(parent, name)
        d.mkdirs()
        return d
    }

    fun rename(path: File, newName: String): Boolean {
        if (!path.exists()) return false
        val target = File(path.parentFile, newName)
        return path.renameTo(target)
    }

    fun delete(path: File): Boolean = if (path.exists()) path.deleteRecursively() else false
}
