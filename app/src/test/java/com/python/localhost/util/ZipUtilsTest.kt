package com.python.localhost.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.File

class ZipUtilsTest {

    private fun makeZip(entries: Map<String, String>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    @Test
    fun extractsNestedEntriesSafely() {
        val zip = makeZip(mapOf("a.txt" to "hello", "sub/b.txt" to "world"))
        val dir = createTempDir("ztest")
        ZipUtils.extractZip(ByteArrayInputStream(zip), dir)
        assertTrue(File(dir, "a.txt").readText() == "hello")
        assertTrue(File(dir, "sub/b.txt").readText() == "world")
    }

    @Test
    fun rejectsPathTraversal() {
        val zip = makeZip(mapOf("../evil.txt" to "x"))
        val dir = createTempDir("ztest2")
        assertThrows(SecurityException::class.java) {
            ZipUtils.extractZip(ByteArrayInputStream(zip), dir)
        }
    }

    @Test
    fun rejectsAbsoluteTraversal() {
        val zip = makeZip(mapOf("/etc/passwd" to "x"))
        val dir = createTempDir("ztest3")
        assertThrows(SecurityException::class.java) {
            ZipUtils.extractZip(ByteArrayInputStream(zip), dir)
        }
    }
}
