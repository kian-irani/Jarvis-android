package com.kianirani.jarvis.core.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vision Notes acceptance: add/edit/pin/delete, pinned-first ordering, substring search. Pure. */
class NoteStoreTest {

    @Test fun `add ignores blank and trims`() {
        val s = NoteStore()
        assertNull(s.add("1", "   ", now = 1))
        assertEquals("hi", s.add("2", "  hi  ", now = 1)?.text)
    }

    @Test fun `all is pinned-first then newest-first`() {
        val s = NoteStore()
        s.add("a", "first", now = 10)
        s.add("b", "second", now = 20)
        s.add("c", "pin me", now = 5); s.setPinned("c", true)
        assertEquals(listOf("c", "b", "a"), s.all().map { it.id })
    }

    @Test fun `edit updates text, fails on missing or blank`() {
        val s = NoteStore()
        s.add("a", "old", now = 1)
        assertTrue(s.edit("a", "new"))
        assertEquals("new", s.get("a")?.text)
        assertFalse(s.edit("a", "  "))
        assertFalse(s.edit("missing", "x"))
    }

    @Test fun `delete removes`() {
        val s = NoteStore()
        s.add("a", "x", now = 1)
        assertTrue(s.delete("a"))
        assertFalse(s.delete("a"))
    }

    @Test fun `search matches text and tags case-insensitively`() {
        val s = NoteStore()
        s.add("a", "Buy MILK", now = 1, tags = listOf("shopping"))
        s.add("b", "call dad", now = 2)
        assertEquals(listOf("a"), s.search("milk").map { it.id })
        assertEquals(listOf("a"), s.search("SHOPPING").map { it.id })
        assertEquals(2, s.search("").size) // blank → all
    }
}
