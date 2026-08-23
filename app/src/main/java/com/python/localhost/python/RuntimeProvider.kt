package com.python.localhost.python

/**
 * Extension point for additional Python runtimes. v1 registers only the embedded
 * Chaquopy CPython, but a downloadable-interpreter provider (or extra versions) can
 * implement this interface and register itself, and the rest of the app will discover
 * it via RuntimeRegistry without code changes.
 */
interface RuntimeProvider {
    fun supportedVersions(): List<RuntimeInfo>
    fun create(version: String): RuntimeInfo?
}

object RuntimeRegistry {
    private val providers = mutableListOf<RuntimeProvider>()

    fun register(p: RuntimeProvider) {
        if (!providers.contains(p)) providers.add(p)
    }

    fun allVersions(): List<RuntimeInfo> = providers.flatMap { it.supportedVersions() }
}
