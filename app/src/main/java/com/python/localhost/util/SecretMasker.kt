package com.python.localhost.util

/**
 * Masks secrets in text destined for logs / terminal output so credentials are
 * never leaked. Applied to all captured process output and log lines.
 */
object SecretMasker {
    private val SECRET_KEYS = setOf(
        "token", "api_key", "apikey", "api-key", "secret", "password", "passwd",
        "pwd", "key", "auth", "authorization", "access_token", "private_key",
        "client_secret", "bot_token", "db_password", "dbpass",
    )

    // Heuristics for common secret value shapes.
    private val SECRET_VALUE_HINTS = Regex(
        "(?i)(Bearer\\s+[A-Za-z0-9._-]+|ghp_[A-Za-z0-9]{16,}|github_pat_[A-Za-z0-9_]{20,}|" +
            "xox[baprs]-[A-Za-z0-9-]{10,}|AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9]{16,}|" +
            "eyJ[A-Za-z0-9_.-]{10,}\\.[A-Za-z0-9_.-]+)",
    )

    private val KEY_VALUE = Regex(
        "(?im)^\\s*(${SECRET_KEYS.joinToString("|")})\\s*=\\s*(\\S+)",
    )

    fun mask(text: String): String {
        var out = SECRET_VALUE_HINTS.replace(text) { m -> "***${m.value.takeLast(4)}" }
        out = KEY_VALUE.replace(out) { m -> "${m.groupValues[1]}=***" }
        return out
    }

    fun isSecretKey(key: String): Boolean =
        SECRET_KEYS.any { k -> key.equals(k, ignoreCase = true) || key.contains(k, ignoreCase = true) }
}
