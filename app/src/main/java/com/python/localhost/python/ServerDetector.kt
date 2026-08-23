package com.python.localhost.python

import java.util.regex.Pattern

data class ServerInfo(
    val host: String,
    val port: Int,
    val lanUrl: String?,
    val localUrl: String,
)

/**
 * Heuristically detects a local web server URL from process output (Flask/FastAPI/
 * uvicorn/static httpservers commonly print "Running on http://...").
 */
object ServerDetector {
    private val PATTERNS = listOf(
        Pattern.compile("(?i)Running on (https?://[0-9a-zA-Z.\\*]+:(\\d+))"),
        Pattern.compile("(?i)http://0\\.0\\.0\\.0:(\\d+)"),
        Pattern.compile("(?i)http://127\\.0\\.0\\.1:(\\d+)"),
        Pattern.compile("(?i)port[= ]+(\\d{2,5})"),
        Pattern.compile("(?i)listening on .*?(\\d{2,5})"),
    )

    fun detect(text: String): ServerInfo? {
        for (p in PATTERNS) {
            val m = p.matcher(text)
            if (!m.find()) continue
            val portStr = if (m.groupCount() >= 2) m.group(2) else m.group(1)
            val port = portStr.toIntOrNull() ?: continue
            if (port <= 0 || port > 65535) continue
            val full = if (m.groupCount() >= 2) m.group(1) else "http://127.0.0.1:$port"
            val host = full.substringAfter("://").substringBefore(":").ifBlank { "127.0.0.1" }
            val local = if (host.contains("*") || host == "0.0.0.0") "127.0.0.1" else host
            val lan = if (host == "0.0.0.0" || host.contains("*")) "<your-device-lan-ip>" else null
            return ServerInfo(host, port, lan, "http://$local:$port")
        }
        return null
    }
}
