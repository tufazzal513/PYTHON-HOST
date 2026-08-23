package com.python.localhost.core

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Tiny JSON persistence helper backed by Gson. Used for all app metadata
 * (project metadata, run configuration, settings). No external database required.
 */
class JsonStore(private val gson: Gson = GsonBuilder().setPrettyPrinting().create()) {

    fun <T> read(file: File, clazz: Class<T>): T? {
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), clazz)
        } catch (e: Exception) {
            null
        }
    }

    fun write(file: File, value: Any) {
        file.parentFile?.mkdirs()
        file.writeText(gson.toJson(value))
    }

    fun readString(file: File): String? = if (file.exists()) file.readText() else null

    fun writeString(file: File, text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text)
    }
}
