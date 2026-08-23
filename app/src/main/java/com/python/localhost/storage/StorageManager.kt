package com.python.localhost.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Helpers around Android's Storage Access Framework (SAF). Importing/exporting uses
 * user-selected documents/trees so the app never needs broad storage permissions.
 */
class StorageManager(private val context: Context) {

    fun pickDirectoryIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }

    fun pickZipIntent(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }

    fun createZipIntent(fileName: String): Intent =
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

    /** Recursively copy a picked document tree into a local directory. */
    fun copyTreeToLocal(treeUri: Uri, dest: File): Long {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IllegalArgumentException("Invalid tree URI")
        return copyDocument(root, dest)
    }

    private fun copyDocument(doc: DocumentFile, dest: File): Long {
        var count = 0L
        if (doc.isDirectory) {
            dest.mkdirs()
            doc.listFiles().forEach { child ->
                count += copyDocument(child, File(dest, child.name ?: "file_${count}"))
            }
        } else if (doc.isFile) {
            dest.parentFile?.mkdirs()
            context.contentResolver.openInputStream(doc.uri)?.use { input ->
                dest.outputStream().use { out -> count += input.copyTo(out) }
            }
        }
        return count
    }

    fun writeToDocument(docUri: Uri, data: ByteArray) {
        context.contentResolver.openOutputStream(docUri)?.use { it.write(data) }
    }

    fun readFromDocument(docUri: Uri): ByteArray =
        context.contentResolver.openInputStream(docUri)?.readBytes() ?: ByteArray(0)
}
