package com.kianirani.jarvis.core.graph

/**
 * Durable run state so a graph survives Android process death and HIL pauses.
 * VCF-G2 defines this contract and runs against any implementation (including an
 * in-memory one in tests); VCF-G3 adds the Room-backed implementation
 * (`{threadId, seq, cursor, stateJson, ts}`, latest seq wins).
 */
interface Checkpointer {
    /** Persist [state] and the [cursor] (next node) for [threadId] as a new revision. */
    suspend fun save(threadId: String, state: GraphState, cursor: String)

    /** Latest saved state for [threadId], or null if none. */
    suspend fun load(threadId: String): GraphState?

    /** Latest saved cursor (next node) for [threadId], or null if none. */
    suspend fun loadCursor(threadId: String): String?

    /** All saved states oldest→newest — for time-travel / dry-run inspection. */
    suspend fun history(threadId: String): List<GraphState>
}
