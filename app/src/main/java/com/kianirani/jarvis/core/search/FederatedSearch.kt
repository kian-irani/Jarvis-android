package com.kianirani.jarvis.core.search

/**
 * MX cross-device search (PRD §14 "cross-device search") — merges already-ranked [SearchResult]
 * lists from several mesh nodes (this phone, a desktop, a server) into one unified list. The
 * same id surfaced by two devices is collapsed to its **best** score (so a strong local hit
 * isn't buried by a weaker remote copy), then everything is re-ranked. Pure → JVM-tested; the
 * network fan-out that gathers each node's [SearchRanker] results is the on-device/mesh half.
 */
data class NodeResults(val nodeId: String, val results: List<SearchResult>)

object FederatedSearch {

    /**
     * Unify [nodes]' results: dedupe by [SearchResult.id] keeping the highest-scoring copy,
     * then sort by score desc (ties by id for stability). [limit] ≤ 0 returns all.
     */
    fun merge(nodes: List<NodeResults>, limit: Int = 0): List<SearchResult> {
        val best = LinkedHashMap<String, SearchResult>()
        for (node in nodes) {
            for (r in node.results) {
                val existing = best[r.id]
                if (existing == null || r.score > existing.score) best[r.id] = r
            }
        }
        val ranked = best.values.sortedWith(compareByDescending<SearchResult> { it.score }.thenBy { it.id })
        return if (limit > 0) ranked.take(limit) else ranked
    }
}
