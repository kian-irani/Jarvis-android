package com.kianirani.jarvis.core.plugin

import com.kianirani.jarvis.core.tools.ToolRegistry
import com.kianirani.jarvis.core.tools.VisionTool

/**
 * DS-X1 — the plugin SDK (PRD §, "registryِ اکشن + ابزارهای قابل‌توسعه + sandbox"). A plugin
 * is a third-party bundle that contributes [VisionTool]s; the registry installs it **under a
 * capability sandbox** so a plugin can only do what the user has granted.
 *
 * Sandbox model: a [PluginManifest] *requests* a set of [Capability] (network, messaging, …);
 * install fails closed if any requested capability is not in the granted set. Each contributed
 * tool also declares the capabilities it needs — a tool whose needs exceed the plugin's
 * effective grant is dropped, not exposed. The surviving tools compose into a [ToolRegistry]
 * the agent already knows how to drive.
 *
 * Pure Kotlin (no Android, no dynamic class loading here) → JVM-tested. Loading plugin code
 * from an APK/DEX and the real permission UI are the on-device half; this defines the
 * contract and the gate.
 */
enum class Capability {
    NETWORK,
    DEVICE_CONTROL,
    MEMORY,
    CONTACTS,
    MESSAGING,
    FILES,
    LOCATION,
}

/** Identity + requested permissions of a plugin. */
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val capabilities: Set<Capability> = emptySet(),
)

/** A tool a plugin contributes, tagged with the capabilities it needs to run. */
data class PluginTool(val tool: VisionTool, val requires: Set<Capability> = emptySet())

/** A bundle the registry can install. */
interface VisionPlugin {
    val manifest: PluginManifest
    fun tools(): List<PluginTool>
}

/** Outcome of an install attempt. */
sealed interface InstallResult {
    /** Installed; [exposedTools] are live, [deniedTools] were dropped for missing capabilities. */
    data class Installed(
        val manifest: PluginManifest,
        val exposedTools: List<String>,
        val deniedTools: List<String>,
    ) : InstallResult

    /** Rejected before install: the plugin requested capabilities the user did not grant. */
    data class Denied(val manifest: PluginManifest, val missing: Set<Capability>) : InstallResult
}

class PluginRegistry {

    private data class Entry(val plugin: VisionPlugin, val granted: Set<Capability>, val exposed: List<VisionTool>)

    // keyed by plugin id so re-installing a plugin replaces it (upgrade), never duplicates.
    private val installed = LinkedHashMap<String, Entry>()

    /**
     * Install [plugin], granting it [granted] capabilities. Fails closed ([InstallResult.Denied])
     * if the manifest asks for anything not granted. Otherwise every contributed tool whose
     * [PluginTool.requires] is within the grant is exposed; the rest are denied (reported).
     */
    fun install(plugin: VisionPlugin, granted: Set<Capability>): InstallResult {
        val missing = plugin.manifest.capabilities - granted
        if (missing.isNotEmpty()) return InstallResult.Denied(plugin.manifest, missing)

        val (allowed, denied) = plugin.tools().partition { it.requires.all { cap -> cap in granted } }
        installed[plugin.manifest.id] = Entry(plugin, granted, allowed.map { it.tool })
        return InstallResult.Installed(
            plugin.manifest,
            exposedTools = allowed.map { it.tool.spec.name },
            deniedTools = denied.map { it.tool.spec.name },
        )
    }

    /** Remove an installed plugin (and its tools) by id; true if it was present. */
    fun uninstall(pluginId: String): Boolean = installed.remove(pluginId) != null

    /** Manifests of currently installed plugins, in install order. */
    fun installed(): List<PluginManifest> = installed.values.map { it.plugin.manifest }

    /** Every exposed tool across all installed plugins. */
    fun tools(): List<VisionTool> = installed.values.flatMap { it.exposed }

    /** A [ToolRegistry] over all exposed plugin tools, ready to hand to the agent. */
    fun toolRegistry(): ToolRegistry = ToolRegistry(tools())
}
