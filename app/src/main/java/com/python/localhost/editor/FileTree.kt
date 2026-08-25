package com.python.localhost.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

data class FileNode(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val children: List<FileNode>,
)

fun buildTree(dir: File, maxDepth: Int = 12): FileNode =
    FileNode(dir.name, dir.absolutePath, true, buildChildren(dir, 0, maxDepth))

private fun buildChildren(dir: File, depth: Int, maxDepth: Int): List<FileNode> {
    if (depth >= maxDepth) return emptyList()
    val files = dir.listFiles() ?: return emptyList()
    return files
        .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        .map {
            if (it.isDirectory) {
                FileNode(it.name, it.absolutePath, true, buildChildren(it, depth + 1, maxDepth))
            } else {
                FileNode(it.name, it.absolutePath, false, emptyList())
            }
        }
}

@Composable
fun FileTree(
    node: FileNode,
    currentPath: String?,
    onOpenFile: (File) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember(node.path) { mutableStateOf(true) }
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDir) expanded = !expanded else onOpenFile(File(node.path))
                }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (node.isDir) (if (expanded) "▾ " else "▸ ") else "  ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                node.name,
                color = if (node.path == currentPath) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (expanded && node.isDir) {
            node.children.forEach { child ->
                Box(Modifier.padding(start = 12.dp)) {
                    FileTree(child, currentPath, onOpenFile)
                }
            }
        }
    }
}
