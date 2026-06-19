package com.kianirani.jarvis.core.sync

/**
 * DS-C3 — universal clipboard (PRD §14, "memory/clipboard/handoff روی mesh"). The latest clip
 * copied on any device becomes the [current] clip everywhere; older clips stay in a bounded
 * [history] for paste-back. Conflict resolution is last-writer-wins on (timestamp, nodeId), so
 * [merge] of two devices' clipboards is order-independent and convergent — the same building
 * block as [LwwMap] but specialized to a single rolling register with history.
 *
 * Pure & deterministic (timestamps are supplied) → JVM-tested. Reading/writing the real
 * Android/desktop clipboard and shipping entries over the mesh are the on-device half.
 */
data class ClipEntry(val content: String, val timestamp: Long, val nodeId: String)

class ClipboardSync(private val historyLimit: Int = DEFAULT_HISTORY) {

    // newest-first, de-duplicated; size capped at historyLimit.
    private val entries = sortedSetOf(
        compareByDescending<ClipEntry> { it.timestamp }.thenByDescending { it.nodeId }.thenBy { it.content },
    )

    /** Record a clip copied on [nodeId] at [timestamp]. Blank content is ignored. */
    fun copy(content: String, timestamp: Long, nodeId: String) {
        if (content.isBlank()) return
        entries.add(ClipEntry(content, timestamp, nodeId))
        trim()
    }

    /** The current (most recent) clip across all devices, or null if empty. */
    fun current(): ClipEntry? = entries.firstOrNull()

    /** Recent clips, newest first, capped at the history limit. */
    fun history(): List<ClipEntry> = entries.toList()

    /**
     * Fold another device's clipboard in. Order-independent and idempotent: identical entries
     * collapse, the newest overall becomes [current], and history stays capped.
     */
    fun merge(other: ClipboardSync): ClipboardSync {
        entries.addAll(other.entries)
        trim()
        return this
    }

    private fun trim() {
        while (entries.size > historyLimit) entries.remove(entries.last())
    }

    companion object {
        const val DEFAULT_HISTORY = 20
    }
}
