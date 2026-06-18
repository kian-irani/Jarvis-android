package com.kianirani.jarvis.core.memory

/**
 * CF4.3 — pure, model-independent filtering for the Memory screen. Semantic recall
 * needs the embedding model downloaded; this lets browse/search work regardless by
 * filtering the already-loaded list on a type and a case-insensitive substring.
 * No Android deps → unit-tested.
 */
object MemoryFilter {
    fun filter(
        rows: List<MemoryEngine.Stored>,
        query: String = "",
        type: MemoryType? = null,
    ): List<MemoryEngine.Stored> {
        val q = query.trim().lowercase()
        return rows.filter { row ->
            (type == null || row.type == type) &&
                (q.isEmpty() || row.content.lowercase().contains(q))
        }
    }

    /** The set of memory types present in [rows], in first-seen order — drives the filter chips. */
    fun typesPresent(rows: List<MemoryEngine.Stored>): List<MemoryType> =
        rows.map { it.type }.distinct()
}
