package com.python.localhost.data

/**
 * Persistent metadata for a project. Stored as JSON at <projectDir>/project.json.
 */
data class ProjectMeta(
    val id: String,
    val name: String,
    val path: String,            // absolute path to project root
    val pythonVersion: String,   // requested Python version, e.g. "3.11"
    val template: String = "basic",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val gitUrl: String? = null,
)

/**
 * How a project should be run. Stored as JSON at <projectDir>/.pymobile/runconfig.json.
 */
data class RunConfig(
    val entryPoint: String = "main.py",   // relative to project root
    val arguments: String = "",
    val workingDir: String = "",          // relative; empty => project root
    val environment: Map<String, String> = emptyMap(),
    val pythonVersion: String = "3.11",
    val runInForeground: Boolean = false,
    val port: Int = 0,
)

/**
 * Global app settings. Stored as JSON at <appDir>/settings/settings.json.
 */
data class AppSettings(
    val fontSize: Int = 13,
    val wordWrap: Boolean = false,
    val darkTheme: Boolean = true,
    val tabSize: Int = 4,
    val showLineNumbers: Boolean = true,
    val autoSave: Boolean = true,
    val pipIndexUrl: String = "https://pypi.org/simple",
    val pipExtraIndexUrl: String = "https://chaquo.com/pypi-13.1",
    val confirmDestructive: Boolean = true,
)

/**
 * Result of automatically inspecting an imported/created project.
 */
data class DetectionResult(
    val entryCandidates: List<String>,
    val detectedEntry: String?,
    val hasRequirementsTxt: Boolean,
    val hasPyprojectToml: Boolean,
    val hasSetupPy: Boolean,
    val hasEnv: Boolean,
    val frameworks: List<String>,
    val hasSqlite: Boolean,
    val hasReadme: Boolean,
    val isGitRepo: Boolean,
    val notes: List<String> = emptyList(),
)

/**
 * A single entry in a project's run history.
 */
data class RunHistoryEntry(
    val id: String,
    val projectId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val status: String,        // RUNNING | COMPLETED | FAILED | STOPPED
    val exitCode: Int?,
    val durationMs: Long?,
)

/**
 * Snapshot of a project's git status.
 */
data class GitStatusInfo(
    val branch: String?,
    val isClean: Boolean,
    val added: List<String>,
    val modified: List<String>,
    val deleted: List<String>,
    val untracked: List<String>,
    val aheadCount: Int,
    val behindCount: Int,
    val remoteUrl: String?,
    val error: String? = null,
)
