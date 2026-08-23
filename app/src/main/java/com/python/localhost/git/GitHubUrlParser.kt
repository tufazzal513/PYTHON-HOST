package com.python.localhost.git

import java.util.regex.Pattern

/**
 * Parses GitHub repository URLs (HTTPS and SSH) into owner/repo and a canonical
 * clone URL. Used to validate user input before cloning.
 */
object GitHubUrlParser {
    private val HTTPS = Pattern.compile(
        """https?://github\.com/([\w.-]+)/([\w.-]+?)(?:\.git)?(?:/.*)?"""
    )
    private val SSH = Pattern.compile(
        """git@github\.com:([\w.-]+)/([\w.-]+?)(?:\.git)?"""
    )

    data class Parsed(val owner: String, val repo: String, val cloneUrl: String)

    fun parse(input: String): Parsed? {
        val trimmed = input.trim()
        for (p in listOf(HTTPS, SSH)) {
            val m = p.matcher(trimmed)
            if (m.find()) {
                val owner = m.group(1)
                var repo = m.group(2).removeSuffix(".git")
                if (owner.isBlank() || repo.isBlank()) continue
                return Parsed(owner, repo, "https://github.com/$owner/$repo.git")
            }
        }
        return null
    }

    fun isValid(input: String): Boolean = parse(input) != null
}
