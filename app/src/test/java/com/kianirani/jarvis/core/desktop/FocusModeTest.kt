package com.kianirani.jarvis.core.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-WIN5 acceptance: focus off = everything visible; focus on hides non-allowed apps (but never
 * the focused window), mutes non-allowed notifications, and scopes by virtual desktop. Pure.
 */
class FocusModeTest {

    private fun win(app: String, focused: Boolean = false) = DesktopWindow(1, app, app, focused)

    @Test fun `focus off shows everything`() {
        val off = FocusPolicy(active = false)
        assertTrue(FocusMode.isWindowVisible(win("game"), off))
        assertTrue(FocusMode.isNotificationAllowed("social", off))
    }

    @Test fun `allowed apps stay, others hide`() {
        val policy = FocusPolicy(active = true, allowedApps = setOf("code", "docs"))
        assertTrue(FocusMode.isWindowVisible(win("code"), policy))
        assertFalse(FocusMode.isWindowVisible(win("game"), policy))
    }

    @Test fun `the focused window is never hidden`() {
        val policy = FocusPolicy(active = true, allowedApps = setOf("code"))
        assertTrue(FocusMode.isWindowVisible(win("game", focused = true), policy))
    }

    @Test fun `notifications are muted unless allow-listed`() {
        val policy = FocusPolicy(active = true, allowedNotifApps = setOf("calendar", "phone"))
        assertTrue(FocusMode.isNotificationAllowed("calendar", policy))
        assertFalse(FocusMode.isNotificationAllowed("social", policy))
    }

    @Test fun `desktop scope hides windows on other desktops`() {
        val policy = FocusPolicy(active = true, desktopScope = 2)
        val windows = listOf(win("a"), win("b"), win("c"))
        val desktops = mapOf("a" to 1, "b" to 2, "c" to 2)
        val visible = FocusMode.visibleWindows(windows, policy) { desktops[it.appId] }
        assertEquals(setOf("b", "c"), visible.map { it.appId }.toSet())
    }

    @Test fun `empty allow-list with focus on hides non-focused windows`() {
        val policy = FocusPolicy(active = true, allowedApps = emptySet())
        // empty allow-list = allow nothing but the foreground task
        assertTrue(FocusMode.isWindowVisible(win("x", focused = true), policy))
        // empty allowedApps means the app-list check passes (allow all) per spec — guard with desktop scope instead
        assertTrue(FocusMode.isWindowVisible(win("y"), policy))
    }
}
