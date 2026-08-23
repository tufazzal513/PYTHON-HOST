package com.python.localhost.python

import android.content.Context

data class RuntimeInfo(
    val id: String,
    val version: String,
    val embedded: Boolean,
    val note: String,
)

/**
 * Abstraction over available Python runtimes. v1 ships a single embedded CPython
 * (via Chaquopy). The architecture allows additional runtimes (downloadable
 * interpreters or further versions) to be registered later without changing how
 * the rest of the app resolves and runs projects.
 */
class RuntimeManager(
    @Suppress("UNUSED_PARAMETER") private val context: Context,
    @Suppress("UNUSED_PARAMETER") private val runtime: PythonRuntime,
) : RuntimeProvider {

    init {
        RuntimeRegistry.register(this)
    }

    override fun supportedVersions(): List<RuntimeInfo> = availableRuntimes()

    override fun create(version: String): RuntimeInfo? = resolve(version)

    fun availableRuntimes(): List<RuntimeInfo> = listOf(
        RuntimeInfo("embedded-3.11", "3.11", true, "Bundled CPython via Chaquopy"),
    )

    /** v1 only bundles Python 3.11. Other versions are not bundled to keep the APK small. */
    fun isVersionAvailable(version: String): Boolean = version.startsWith("3.11")

    fun resolve(version: String): RuntimeInfo? =
        availableRuntimes().firstOrNull { it.version == version }

    fun embeddedVersion(): String = "3.11"
}
