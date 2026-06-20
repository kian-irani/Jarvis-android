package com.kianirani.jarvis.core.plugin

import kotlinx.serialization.Serializable

/**
 * B3 — Plugin Marketplace (PRD §, "قوی‌تر از Omi — GitHub/Docker/Telegram/Discord/Notion/…"). The
 * pure catalog the marketplace screen browses: listed plugins with their source, requested
 * [Capability]s and install state, plus search/filter. Installing a catalog entry hands off to
 * the [PluginRegistry] (capability sandbox). Pure & serializable → JVM-tested.
 */
@Serializable
enum class PluginSource { GITHUB, DOCKER, TELEGRAM, DISCORD, NOTION, HOME_ASSISTANT, OPENROUTER, OLLAMA, OTHER }

@Serializable
data class CatalogEntry(
    val id: String,
    val name: String,
    val description: String,
    val source: PluginSource,
    val capabilities: Set<Capability> = emptySet(),
    val verified: Boolean = false,
)

class PluginCatalog(entries: List<CatalogEntry>) {

    private val byId = entries.associateBy { it.id }
    private val all = entries

    fun get(id: String): CatalogEntry? = byId[id]

    /** Case-insensitive search over name/description, verified-first then name. */
    fun search(query: String): List<CatalogEntry> {
        val q = query.trim().lowercase()
        val matched = if (q.isEmpty()) all else all.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
        return matched.sortedWith(compareByDescending<CatalogEntry> { it.verified }.thenBy { it.name.lowercase() })
    }

    /** Entries from a given source (e.g. only Notion integrations). */
    fun bySource(source: PluginSource): List<CatalogEntry> = all.filter { it.source == source }

    /** Whether [id]'s requested capabilities are all within [granted] (installable without new grants). */
    fun installableWith(id: String, granted: Set<Capability>): Boolean {
        val entry = byId[id] ?: return false
        return entry.capabilities.all { it in granted }
    }
}
