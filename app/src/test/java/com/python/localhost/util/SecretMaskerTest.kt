package com.python.localhost.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretMaskerTest {

    @Test
    fun masksKeyValueSecrets() {
        val out = SecretMasker.mask("token=ghp_ABCD1234secret")
        assertTrue(out.contains("***"))
        assertFalse(out.contains("ghp_ABCD1234secret"))
    }

    @Test
    fun masksBearerTokens() {
        val out = SecretMasker.mask("Authorization: Bearer abcdef1234567890")
        assertTrue(out.contains("***"))
    }

    @Test
    fun masksGitHubPat() {
        val out = SecretMasker.mask("github_pat_1234567890abcdef")
        assertTrue(out.contains("***"))
    }

    @Test
    fun keepsOrdinaryText() {
        val out = SecretMasker.mask("print('hello world')")
        assertEquals("print('hello world')", out)
    }
}
