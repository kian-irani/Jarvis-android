package com.kianirani.jarvis.core.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Focus session acceptance: active window, remaining time, notif mute + app block. Pure. */
class FocusSessionTest {

    private val start = 1_000_000L
    private val session = FocusSession(start, durationMillis = 60_000, allowedApps = setOf("calendar"), blockedApps = setOf("social"))

    @Test fun `active only within the window`() {
        assertTrue(FocusManager.isActive(session, start + 30_000))
        assertFalse(FocusManager.isActive(session, start + 70_000))
        assertFalse(FocusManager.isActive(session, start - 1))
    }

    @Test fun `remaining time counts down to zero`() {
        assertEquals(40_000L, FocusManager.remainingMillis(session, start + 20_000))
        assertEquals(0L, FocusManager.remainingMillis(session, start + 100_000))
    }

    @Test fun `notifications muted except allowed during focus`() {
        assertTrue(FocusManager.allowsNotification(session, "calendar", start + 10_000))
        assertFalse(FocusManager.allowsNotification(session, "social", start + 10_000))
        assertTrue(FocusManager.allowsNotification(session, "social", start + 90_000)) // session over
    }

    @Test fun `blocked apps blocked only during focus`() {
        assertTrue(FocusManager.blocksApp(session, "social", start + 10_000))
        assertFalse(FocusManager.blocksApp(session, "social", start + 90_000))
        assertFalse(FocusManager.blocksApp(session, "code", start + 10_000))
    }
}
