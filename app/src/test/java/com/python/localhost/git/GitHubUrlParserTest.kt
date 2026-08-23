package com.python.localhost.git

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubUrlParserTest {

    @Test
    fun parsesHttps() {
        val p = GitHubUrlParser.parse("https://github.com/owner/repo")!!
        assertEquals("owner", p.owner)
        assertEquals("repo", p.repo)
        assertEquals("https://github.com/owner/repo.git", p.cloneUrl)
    }

    @Test
    fun parsesHttpsWithGitSuffixAndPath() {
        val p = GitHubUrlParser.parse("https://github.com/owner/repo.git")!!
        assertEquals("repo", p.repo)
        val p2 = GitHubUrlParser.parse("https://github.com/a/b/tree/main")!!
        assertEquals("b", p2.repo)
    }

    @Test
    fun parsesSsh() {
        val p = GitHubUrlParser.parse("git@github.com:owner/repo.git")!!
        assertEquals("owner", p.owner)
        assertEquals("repo", p.repo)
    }

    @Test
    fun rejectsNonGitHub() {
        assertNull(GitHubUrlParser.parse("https://gitlab.com/x/y"))
        assertNull(GitHubUrlParser.parse("not a url"))
    }
}
