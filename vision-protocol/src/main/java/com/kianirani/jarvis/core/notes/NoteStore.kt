package com.kianirani.jarvis.core.notes

import kotlinx.serialization.Serializable

/**
 * Vision Notes — quick capture + retrieval (PRD §, "یادداشت سریع + embedding + بازیابی"). The
 * pure, in-memory note store: add/edit/pin/delete and substring search, newest-first. Semantic
 * recall via the existing MiniLM embedder is a graceful enhancement on top (the note text also
 * feeds MemoryEngine); this core is deterministic and JVM-tested. Persisting to Room is the
 * device half.
 */
@Serializable
data class Note(
    val id: String,
    val text: String,
    val createdAt: Long,
    val pinned: Boolean = false,
    val tags: List<String> = emptyList(),
)

class NoteStore {

    private val notes = LinkedHashMap<String, Note>()

    /** Add a note (blank text ignored); returns it, or null if blank. */
    fun add(id: String, text: String, now: Long, tags: List<String> = emptyList()): Note? {
        if (text.isBlank()) return null
        val note = Note(id, text.trim(), now, tags = tags)
        notes[id] = note
        return note
    }

    fun edit(id: String, text: String): Boolean {
        val n = notes[id] ?: return false
        if (text.isBlank()) return false
        notes[id] = n.copy(text = text.trim())
        return true
    }

    fun setPinned(id: String, pinned: Boolean): Boolean {
        val n = notes[id] ?: return false
        notes[id] = n.copy(pinned = pinned)
        return true
    }

    fun delete(id: String): Boolean = notes.remove(id) != null

    fun get(id: String): Note? = notes[id]

    /** All notes: pinned first, then newest-first by creation time. */
    fun all(): List<Note> =
        notes.values.sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.createdAt })

    /** Case-insensitive substring search over text + tags, ranked like [all]. */
    fun search(query: String): List<Note> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all()
        return all().filter { n -> n.text.lowercase().contains(q) || n.tags.any { it.lowercase().contains(q) } }
    }
}
