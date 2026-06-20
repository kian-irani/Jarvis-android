package com.kianirani.jarvis.core.search

import com.kianirani.jarvis.core.notes.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A3 acceptance: heterogeneous sources merge into one ranked list; source weights bias ties. Pure. */
class UnifiedSearchTest {

    @Test fun `apps contacts and notes all searchable in one list`() {
        val results = UnifiedSearch.search(
            query = "ali",
            apps = listOf(UnifiedSearch.AppItem("com.aliexpress", "AliExpress")),
            contacts = listOf(UnifiedSearch.ContactItem("c1", "Ali Rezaei", "mobile")),
            notes = listOf(Note("n1", "call Ali tomorrow", createdAt = 1)),
        )
        val ids = results.map { it.id }.toSet()
        assertTrue(ids.contains("c1")) // contact
        assertTrue(ids.contains("n1")) // note
        assertTrue(ids.contains("com.aliexpress")) // app
    }

    @Test fun `an exact contact name outranks a partial app match`() {
        val results = UnifiedSearch.search(
            query = "maps",
            apps = listOf(UnifiedSearch.AppItem("com.maps.pro", "Maps Pro")),
            contacts = listOf(UnifiedSearch.ContactItem("c1", "Maps")), // exact
        )
        assertEquals("c1", results.first().id)
    }

    @Test fun `limit caps the unified output`() {
        val apps = (1..10).map { UnifiedSearch.AppItem("p$it", "app$it") }
        assertEquals(3, UnifiedSearch.search("app", apps = apps, limit = 3).size)
    }

    @Test fun `no sources yields empty`() {
        assertTrue(UnifiedSearch.search("anything").isEmpty())
    }
}
