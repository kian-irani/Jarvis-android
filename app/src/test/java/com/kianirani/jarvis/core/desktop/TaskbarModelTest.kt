package com.kianirani.jarvis.core.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-WIN3 acceptance: pinned apps come first, running-not-pinned follow, windows group by app,
 * and the focused app is marked active. Pure.
 */
class TaskbarModelTest {

    private fun win(h: Long, app: String, focused: Boolean = false) = DesktopWindow(h, app, "$app#$h", focused)

    @Test fun `pinned apps lead, running-not-pinned follow in first-seen order`() {
        val entries = TaskbarModel.entries(
            pinned = listOf("code", "browser"),
            windows = listOf(win(1, "browser"), win(2, "terminal"), win(3, "music")),
        )
        assertEquals(listOf("code", "browser", "terminal", "music"), entries.map { it.appId })
    }

    @Test fun `windows of one app group into a single entry`() {
        val entries = TaskbarModel.entries(
            pinned = emptyList(),
            windows = listOf(win(1, "browser"), win(2, "browser"), win(3, "browser")),
        )
        assertEquals(1, entries.size)
        assertEquals(3, entries.first().windows.size)
    }

    @Test fun `a pinned app with no window is present but not running`() {
        val entries = TaskbarModel.entries(pinned = listOf("code"), windows = emptyList())
        val code = entries.single()
        assertTrue(code.pinned)
        assertFalse(code.running)
    }

    @Test fun `the focused window marks its app active`() {
        val entries = TaskbarModel.entries(
            pinned = emptyList(),
            windows = listOf(win(1, "browser"), win(2, "terminal", focused = true)),
        )
        assertTrue(entries.first { it.appId == "terminal" }.active)
        assertFalse(entries.first { it.appId == "browser" }.active)
        assertEquals(2L, TaskbarModel.focused(listOf(win(1, "browser"), win(2, "terminal", focused = true)))?.handle)
    }
}
