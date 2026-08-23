package com.python.localhost.project

import com.python.localhost.core.AppDirs
import com.python.localhost.core.JsonStore
import com.python.localhost.data.ProjectMeta
import com.python.localhost.data.RunConfig
import com.python.localhost.util.ZipUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Manages the full lifecycle of projects: creation, import (folder/zip), export (zip),
 * backup/restore, rename, delete, and reading/writing per-project run configuration.
 * All projects live under AppDirs.projects/<id>.
 */
class ProjectManager(private val appDirs: AppDirs, private val json: JsonStore) {

    fun listProjects(): List<ProjectMeta> {
        val dir = appDirs.projects
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.filter { it.isDirectory }
            ?.mapNotNull { readMeta(it) }
            ?.sortedByDescending { it.updatedAt } ?: emptyList()
    }

    fun readMeta(dir: File): ProjectMeta? =
        json.read(File(dir, "project.json"), ProjectMeta::class.java)

    fun saveMeta(meta: ProjectMeta) =
        json.write(
            File(File(appDirs.projects, meta.id), "project.json"),
            meta.copy(updatedAt = System.currentTimeMillis()),
        )

    fun getProject(id: String): ProjectMeta? {
        val dir = appDirs.projectDir(id)
        return if (dir.exists()) readMeta(dir) else null
    }

    fun createProject(name: String, pythonVersion: String, template: String): ProjectMeta {
        val id = UUID.randomUUID().toString().take(8)
        val dir = appDirs.projectDir(id)
        dir.mkdirs()
        val meta = ProjectMeta(
            id = id, name = name, path = dir.absolutePath,
            pythonVersion = pythonVersion, template = template,
        )
        json.write(File(dir, "project.json"), meta)
        ProjectTemplates.apply(template, dir, name)
        saveMeta(meta)
        return meta
    }

    fun deleteProject(id: String) {
        appDirs.projectDir(id).deleteRecursively()
    }

    fun renameProject(id: String, newName: String): ProjectMeta? {
        val meta = getProject(id) ?: return null
        val updated = meta.copy(name = newName)
        saveMeta(updated)
        return updated
    }

    /** Finalise a project copied from a picked SAF tree (already on local disk). */
    fun finalizeImported(name: String, pythonVersion: String, copiedDir: File): ProjectMeta {
        val id = UUID.randomUUID().toString().take(8)
        val dir = appDirs.projectDir(id)
        dir.mkdirs()
        copiedDir.copyRecursively(dir, overwrite = true)
        copiedDir.deleteRecursively()
        val meta = ProjectMeta(id = id, name = name, path = dir.absolutePath, pythonVersion = pythonVersion)
        json.write(File(dir, "project.json"), meta)
        saveMeta(meta)
        return meta
    }

    fun importZip(bytes: ByteArray, name: String, pythonVersion: String): ProjectMeta {
        val id = UUID.randomUUID().toString().take(8)
        val dir = appDirs.projectDir(id)
        dir.mkdirs()
        try {
            ZipUtils.extractZip(bytes.inputStream(), dir)
        } catch (e: SecurityException) {
            dir.deleteRecursively()
            throw e
        }
        val meta = ProjectMeta(id = id, name = name, path = dir.absolutePath, pythonVersion = pythonVersion)
        json.write(File(dir, "project.json"), meta)
        saveMeta(meta)
        return meta
    }

    fun exportZip(id: String): ByteArray {
        val dir = appDirs.projectDir(id)
        val out = ByteArrayOutputStream()
        ZipUtils.zipDirectory(dir, out)
        return out.toByteArray()
    }

    fun backupProject(id: String): File {
        val meta = getProject(id) ?: throw IllegalArgumentException("Project not found")
        val backupFile = File(appDirs.backups, "${meta.name}-${id}-${System.currentTimeMillis()}.zip")
        backupFile.outputStream().use { ZipUtils.zipDirectory(appDirs.projectDir(id), it) }
        return backupFile
    }

    /** Register a directory that was populated by a git clone as a project. */
    fun createFromCloned(cloneDir: File, name: String, version: String, gitUrl: String?): ProjectMeta {
        val id = cloneDir.name
        val meta = ProjectMeta(
            id = id, name = name, path = cloneDir.absolutePath,
            pythonVersion = version, gitUrl = gitUrl,
        )
        json.write(File(cloneDir, "project.json"), meta)
        saveMeta(meta)
        return meta
    }

    fun getRunConfig(dir: File): RunConfig =
        json.read(runConfigFile(dir), RunConfig::class.java) ?: RunConfig()

    fun saveRunConfig(dir: File, config: RunConfig) =
        json.write(runConfigFile(dir), config)

    private fun runConfigFile(dir: File) =
        File(File(dir, ".pymobile"), "runconfig.json").apply { parentFile?.mkdirs() }
}
