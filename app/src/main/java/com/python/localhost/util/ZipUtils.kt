package com.python.localhost.util

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * ZIP utilities with built-in path-traversal (Zip-Slip) protection and size limits.
 */
object ZipUtils {
    private const val BUFFER = 8192
    private const val MAX_ENTRIES = 100_000
    private const val MAX_UNCOMPRESSED = 2L * 1024 * 1024 * 1024 // 2 GB

    /**
     * Extract a zip safely into [destDir], rejecting path traversal and zip bombs.
     * @throws SecurityException if an entry tries to escape the target directory.
     * @throws IOException on read/write errors or oversized archives.
     */
    @Throws(SecurityException::class, IOException::class)
    fun extractZip(input: InputStream, destDir: File) {
        destDir.mkdirs()
        val destCanonical = destDir.canonicalPath
        var entries = 0
        var total = 0L
        ZipInputStream(BufferedInputStream(input)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (++entries > MAX_ENTRIES) throw IOException("Too many entries in archive")
                val outFile = safeFile(destDir, entry.name, destCanonical)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buf = ByteArray(BUFFER)
                        var len: Int
                        while (zis.read(buf).also { len = it } > 0) {
                            total += len
                            if (total > MAX_UNCOMPRESSED) throw IOException("Archive too large")
                            fos.write(buf, 0, len)
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun safeFile(destDir: File, name: String, destCanonical: String): File {
        val normalized = name.replace('\\', '/')
        if (normalized.startsWith("/") ||
            normalized == ".." ||
            normalized.startsWith("../") ||
            normalized.contains("/../") ||
            normalized.contains("..\\")
        ) {
            throw SecurityException("Illegal zip entry: $name")
        }
        val out = File(destDir, normalized).canonicalFile
        val outCanonical = out.canonicalPath
        if (outCanonical != destCanonical &&
            !outCanonical.startsWith("$destCanonical${File.separator}")
        ) {
            throw SecurityException("Zip entry escapes target directory: $name")
        }
        return out
    }

    /** Create a zip from a directory, preserving its relative structure. */
    fun zipDirectory(sourceDir: File, output: OutputStream) {
        ZipOutputStream(BufferedOutputStream(output)).use { zos ->
            sourceDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val rel = sourceDir.toURI().relativize(file.toURI()).path
                    zos.putNextEntry(ZipEntry(rel))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
        }
    }
}
