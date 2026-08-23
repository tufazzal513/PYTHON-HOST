package com.python.localhost.util

import java.io.File
import java.util.LinkedHashMap

/**
 * Minimal .env parser (KEY=VALUE, supports # comments and quoted values).
 */
object EnvParser {
    fun parse(file: File): Map<String, String> {
        if (!file.exists()) return emptyMap()
        val map = LinkedHashMap<String, String>()
        file.readLines().forEach { line ->
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) return@forEach
            val idx = t.indexOf('=')
            if (idx > 0) {
                val k = t.substring(0, idx).trim()
                var v = t.substring(idx + 1).trim()
                if ((v.startsWith("\"") && v.endsWith("\"")) ||
                    (v.startsWith("'") && v.endsWith("'"))
                ) {
                    v = v.substring(1, v.length - 1)
                }
                map[k] = v
            }
        }
        return map
    }
}
