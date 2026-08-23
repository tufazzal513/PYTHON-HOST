package com.python.localhost.github

import com.python.localhost.git.GitHubUrlParser
import com.python.localhost.git.GitManager
import com.python.localhost.git.GitResult
import java.io.File

/**
 * Imports public GitHub repositories by cloning via JGit. Public repositories do not
 * require authentication. Validation rejects anything that is not a github.com URL.
 */
class GitHubImporter(private val git: GitManager) {

    /** Returns an error message, or null if the URL is a valid GitHub repo URL. */
    fun validate(url: String): String? {
        if (url.isBlank()) return "Please enter a repository URL."
        val parsed = GitHubUrlParser.parse(url)
            ?: return "That doesn't look like a GitHub repository URL."
        if (!parsed.cloneUrl.startsWith("https://github.com/")) {
            return "Only github.com public repositories are supported."
        }
        return null
    }

    fun import(url: String, target: File): GitResult {
        val parsed = GitHubUrlParser.parse(url)
            ?: return GitResult(false, "Invalid GitHub URL.")
        return git.clone(parsed.cloneUrl, target)
    }
}
