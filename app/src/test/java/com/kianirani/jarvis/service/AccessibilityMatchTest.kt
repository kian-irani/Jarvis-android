package com.kianirani.jarvis.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PAU acceptance: matching normalizes case/whitespace and does exact + substring. Pure. */
class AccessibilityMatchTest {

    @Test fun `normalize collapses whitespace and lowercases`() {
        assertEquals("send now", AccessibilityMatch.normalize("  Send   NOW "))
        assertEquals("", AccessibilityMatch.normalize(null))
    }

    @Test fun `label prefers text then content description`() {
        assertEquals("Send", AccessibilityMatch.label("Send", "button"))
        assertEquals("button", AccessibilityMatch.label("", "button"))
        assertEquals("button", AccessibilityMatch.label(null, "button"))
    }

    @Test fun `exact and substring match, case-insensitive`() {
        val target = AccessibilityMatch.normalize("send")
        assertTrue(AccessibilityMatch.matches("Send", target))
        assertTrue(AccessibilityMatch.matches("Send message", target)) // substring
        assertFalse(AccessibilityMatch.matches("Cancel", target))
    }

    @Test fun `empty target or label never matches`() {
        assertFalse(AccessibilityMatch.matches("Send", ""))
        assertFalse(AccessibilityMatch.matches(null, "send"))
        assertFalse(AccessibilityMatch.matches("", "send"))
    }
}
