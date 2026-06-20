package com.kianirani.jarvis.core.search

/**
 * SRCH / DS-L4 (PRD §9) — the source a search hit came from. [weight] biases the unified
 * ranking so that, all else equal, an actionable hit (run a command, open an app) outranks
 * a web result. Pure data — the adapters that actually query each source (apps via
 * PackageManager, contacts via the provider, memory via [com.kianirani.jarvis.core.memory],
 * web via the brain) are the on-device/network half.
 */
enum class SearchSource(val weight: Float) {
    ACTIONS(1.20f),
    APPS(1.10f),
    CONTACTS(1.00f),
    MEMORY(0.90f),
    SETTINGS(0.85f),
    MESSAGES(0.80f),
    FILES(0.70f),
    WEB(0.50f),
}

/** A raw hit handed to the ranker by a source adapter (before unified scoring). */
data class SearchCandidate(
    val id: String,
    val title: String,
    val source: SearchSource,
    val subtitle: String = "",
    /** Extra 0f..1f signal from the source (e.g. usage/recency for apps, cosine for memory). */
    val relevanceBoost: Float = 0f,
)

/** A scored, ranked result shown in the unified search list. */
data class SearchResult(
    val id: String,
    val title: String,
    val source: SearchSource,
    val subtitle: String,
    val score: Float,
)

/**
 * SRCH / DS-L4 — the pure, deterministic core of universal search: merge candidates from
 * every source into one ranked, semantically-scored list (apps/files/settings/contacts/
 * AI-actions/web/memory). One scoring function over all sources, so the result list is
 * comparable across them. JVM-testable; no Android, no embeddings required (a source may
 * pass a cosine score in via [SearchCandidate.relevanceBoost]).
 */
object SearchRanker {

    /**
     * Rank [candidates] for [query]. With a blank query, hits are ordered by source weight
     * + boost (a useful "all sources" browse). Otherwise a candidate must have a non-zero
     * text match to appear. Ties keep input order (stable); [limit] caps the output (≤0 = all).
     */
    fun rank(query: String, candidates: List<SearchCandidate>, limit: Int = 0): List<SearchResult> {
        val q = query.trim()
        val scored = candidates.mapNotNull { c ->
            val text = if (q.isEmpty()) 1f else textScore(q, c.title, c.subtitle)
            if (q.isNotEmpty() && text <= 0f) return@mapNotNull null
            val score = text * c.source.weight + c.relevanceBoost
            SearchResult(c.id, c.title, c.source, c.subtitle, score)
        }
        val ranked = scored.sortedByDescending { it.score } // sortedBy* is stable → ties keep order
        return if (limit > 0) ranked.take(limit) else ranked
    }

    /**
     * Pure text relevance in `0f..1f`: exact title match = 1.0, title prefix = 0.85, all query
     * tokens present in title = 0.7, partial token overlap scales in, a subtitle-only match =
     * 0.3, else 0. Case-insensitive.
     */
    fun textScore(query: String, title: String, subtitle: String = ""): Float {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return 0f
        val t = title.trim().lowercase()
        when {
            t == q -> return 1.0f
            t.startsWith(q) -> return 0.85f
        }
        val qTokens = q.split(WS).filter { it.isNotBlank() }
        if (qTokens.isEmpty()) return 0f
        val tTokens = t.split(WS).filter { it.isNotBlank() }.toSet()
        val hit = qTokens.count { token -> tTokens.any { it == token || it.startsWith(token) } }
        if (hit == qTokens.size) return 0.70f
        if (hit > 0) return 0.40f * (hit.toFloat() / qTokens.size)
        if (subtitle.trim().lowercase().contains(q)) return 0.30f
        return 0f
    }

    private val WS = Regex("\\s+")
}
