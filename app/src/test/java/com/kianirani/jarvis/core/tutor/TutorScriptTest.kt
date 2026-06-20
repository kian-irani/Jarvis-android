package com.kianirani.jarvis.core.tutor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vision Tutor acceptance: stepping, skip, completion, progress. Pure. */
class TutorScriptTest {

    private fun script() = TutorScript(listOf(
        TutorStep("orb", "The orb", "tap to talk"),
        TutorStep("dock", "The dock", "your apps"),
        TutorStep("search", "Search", "find anything"),
    ))

    @Test fun `starts on the first step`() {
        assertEquals("orb", script().current()?.key)
        assertEquals(1 to 3, script().position())
    }

    @Test fun `next advances and finishes after the last`() {
        val s = script()
        assertEquals("dock", s.next()?.key)
        assertEquals("search", s.next()?.key)
        assertNull(s.next()) // past the end → finished
        assertTrue(s.isComplete())
    }

    @Test fun `previous goes back but not before start`() {
        val s = script()
        s.next()
        assertEquals("orb", s.previous()?.key)
        assertEquals("orb", s.previous()?.key) // clamped
    }

    @Test fun `skip ends the tour`() {
        val s = script()
        s.skip()
        assertTrue(s.isComplete())
        assertNull(s.current())
        assertEquals(1f, s.progress(), 1e-6f)
    }

    @Test fun `progress increases through the tour`() {
        val s = script()
        assertEquals(0f, s.progress(), 1e-6f)
        s.next()
        assertTrue(s.progress() > 0f)
    }

    @Test fun `an empty script is immediately complete`() {
        val s = TutorScript(emptyList())
        assertTrue(s.isComplete())
        assertFalse(s.current() != null)
    }
}
