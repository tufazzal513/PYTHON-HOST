package com.python.localhost.python

import com.python.localhost.core.AppDirs
import com.python.localhost.project.RequirementsParser
import java.io.File

data class InstallResult(
    val packages: List<String>,
    val message: String,
    val exitCode: Int,
)

/**
 * Detects and installs project dependencies from requirements.txt / pyproject.toml /
 * setup.py using the embedded pip. Installations are isolated per project under the
 * app's `packages/<projectId>` directory, which is added to sys.path at run time.
 */
class DependencyManager(
    private val runtime: PythonRuntime,
    private val appDirs: AppDirs,
) {
    fun requirementsFile(projectDir: File) = File(projectDir, "requirements.txt")
    fun pyprojectFile(projectDir: File) = File(projectDir, "pyproject.toml")
    fun setupPyFile(projectDir: File) = File(projectDir, "setup.py")

    /** Requirement specifiers from requirements.txt (best-effort, ignores comments/flags). */
    fun parseRequirements(projectDir: File): List<String> {
        val req = requirementsFile(projectDir)
        if (!req.exists()) return emptyList()
        return req.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("-") }
    }

    /** Combined requirements from requirements.txt + pyproject.toml + setup.py. */
    fun collectRequirements(projectDir: File): List<String> = RequirementsParser.collect(projectDir)

    fun hasDependencyConfig(projectDir: File): Boolean =
        requirementsFile(projectDir).exists() ||
            pyprojectFile(projectDir).exists() ||
            setupPyFile(projectDir).exists()

    fun sitePackagesDir(projectId: String): File =
        appDirs.packagesDir(projectId).apply { mkdirs() }

    /** Real installation driven by embedded pip. Reports the actual outcome. */
    fun install(
        projectId: String,
        projectDir: File,
        indexUrl: String,
        extraIndexUrl: String,
        onOutput: (String) -> Unit,
    ): InstallResult {
        val pkgs = RequirementsParser.collect(projectDir)
        if (pkgs.isEmpty()) {
            return InstallResult(emptyList(), "No installable dependencies found in requirements.txt / pyproject.toml / setup.py.", 0)
        }
        val target = sitePackagesDir(projectId).absolutePath
        val listener = PyOutputListener(onOutput)
        val rc = runtime.pipInstall(target, pkgs, indexUrl, extraIndexUrl, listener)
        return InstallResult(
            pkgs,
            if (rc == 0) "Installed ${pkgs.size} package(s) into the project environment."
            else "pip finished with exit code $rc. Some packages may not be Android-compatible " +
                "(native wheels are limited on Android).",
            rc,
        )
    }

    /** Best-effort detection of already-installed top-level packages in the project env. */
    fun installedPackages(projectId: String): Set<String> {
        val dir = sitePackagesDir(projectId)
        if (!dir.exists()) return emptySet()
        return dir.listFiles()
            ?.filter { it.isDirectory || it.extension == "dist-info" || it.extension == "egg-info" }
            ?.mapNotNull { f ->
                val n = f.name
                when {
                    n.endsWith(".dist-info") -> n.substringBefore(".dist-info")
                    n.endsWith(".egg-info") -> n.substringBefore(".egg-info")
                    else -> null
                }
            }
            ?.toSet() ?: emptySet()
    }
}
