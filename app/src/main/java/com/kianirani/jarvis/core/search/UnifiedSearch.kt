package com.kianirani.jarvis.core.search

import com.kianirani.jarvis.core.notes.Note

/**
 * A3 / AnySearch (PRD §, "جستجوی یکپارچه روی پیام‌ها/فایل‌ها/نوت‌ها/حافظه/اپ‌ها"). Adapts each
 * heterogeneous source into [SearchCandidate]s and hands them to [SearchRanker] for one ranked
 * list across everything. The adapters here are pure (apps/contacts/notes → candidates); the
 * device adapters that *fetch* the raw items (PackageManager, contacts provider) live on the
 * surface and call these. So `find: pizza` returns the best hit whatever its source.
 */
object UnifiedSearch {

    data class AppItem(val pkg: String, val label: String, val usageBoost: Float = 0f)
    data class ContactItem(val id: String, val name: String, val detail: String = "")

    fun appsToCandidates(apps: List<AppItem>): List<SearchCandidate> =
        apps.map { SearchCandidate(it.pkg, it.label, SearchSource.APPS, relevanceBoost = it.usageBoost.coerceIn(0f, 1f)) }

    fun contactsToCandidates(contacts: List<ContactItem>): List<SearchCandidate> =
        contacts.map { SearchCandidate(it.id, it.name, SearchSource.CONTACTS, subtitle = it.detail) }

    fun notesToCandidates(notes: List<Note>): List<SearchCandidate> =
        notes.map { SearchCandidate(it.id, it.text.take(60), SearchSource.FILES, subtitle = if (it.pinned) "pinned note" else "note") }

    /**
     * Rank everything for [query] across the supplied sources in one list (apps/contacts/notes,
     * extend with messages/memory/web as their adapters arrive). [limit] caps the output.
     */
    fun search(
        query: String,
        apps: List<AppItem> = emptyList(),
        contacts: List<ContactItem> = emptyList(),
        notes: List<Note> = emptyList(),
        extra: List<SearchCandidate> = emptyList(),
        limit: Int = 20,
    ): List<SearchResult> {
        val candidates = appsToCandidates(apps) + contactsToCandidates(contacts) + notesToCandidates(notes) + extra
        return SearchRanker.rank(query, candidates, limit)
    }
}
