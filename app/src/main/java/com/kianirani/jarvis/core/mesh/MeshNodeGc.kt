package com.kianirani.jarvis.core.mesh

/**
 * Mesh node garbage-collection (PRD §, "node garbage-collection — پاک‌سازی نودهای آفلاین قدیمی").
 * The pure policy for pruning the mesh roster: a node not heard from within a grace window is
 * stale and dropped, so the registry doesn't accumulate dead peers. Pure (caller supplies "now"
 * and each node's last-heartbeat) → JVM-tested; the live registry + heartbeat receiver are the
 * network half.
 */
data class MeshNodeRecord(val id: String, val lastSeenMillis: Long)

object MeshNodeGc {

    /** Default grace: a node silent this long is considered gone. */
    const val DEFAULT_STALE_AFTER_MS = 90_000L // 90s (~3 missed 30s heartbeats)

    /** True if [node] hasn't been heard from within [staleAfterMs] of [now]. */
    fun isStale(node: MeshNodeRecord, now: Long, staleAfterMs: Long = DEFAULT_STALE_AFTER_MS): Boolean =
        now - node.lastSeenMillis > staleAfterMs

    /** The live nodes (fresh enough to keep), preserving input order. */
    fun live(nodes: List<MeshNodeRecord>, now: Long, staleAfterMs: Long = DEFAULT_STALE_AFTER_MS): List<MeshNodeRecord> =
        nodes.filter { !isStale(it, now, staleAfterMs) }

    /** The stale node ids to evict. */
    fun toEvict(nodes: List<MeshNodeRecord>, now: Long, staleAfterMs: Long = DEFAULT_STALE_AFTER_MS): List<String> =
        nodes.filter { isStale(it, now, staleAfterMs) }.map { it.id }
}
