package com.kianirani.jarvis.data.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-L3 acceptance: the layout undo/redo stack records, reverses, replays, clears redo on a
 * new edit, drops identical edits, and bounds the past. Pure, no device.
 */
class LayoutHistoryTest {

    private fun layout(cols: Int) = LauncherLayout(gridCols = cols)

    @Test fun `fresh history cannot undo or redo`() {
        val h = LayoutHistory(layout(5))
        assertFalse(h.canUndo)
        assertFalse(h.canRedo)
        assertNull(h.undo())
        assertNull(h.redo())
    }

    @Test fun `record then undo restores the previous layout`() {
        val h = LayoutHistory(layout(5))
        h.record(layout(6))
        assertEquals(layout(6), h.present)
        assertTrue(h.canUndo)
        assertEquals(layout(5), h.undo())
        assertEquals(layout(5), h.present)
        assertTrue(h.canRedo)
    }

    @Test fun `redo replays an undone edit`() {
        val h = LayoutHistory(layout(5))
        h.record(layout(6))
        h.undo()
        assertEquals(layout(6), h.redo())
        assertEquals(layout(6), h.present)
        assertFalse(h.canRedo)
    }

    @Test fun `a new edit after undo clears the redo stack`() {
        val h = LayoutHistory(layout(5))
        h.record(layout(6))
        h.undo() // present back to 5, redo has 6
        h.record(layout(4)) // new branch
        assertFalse(h.canRedo)
        assertNull(h.redo())
        assertEquals(layout(4), h.present)
    }

    @Test fun `recording the same layout is a no-op`() {
        val h = LayoutHistory(layout(5))
        h.record(layout(5))
        assertFalse(h.canUndo)
        assertEquals(layout(5), h.present)
    }

    @Test fun `multi-step undo and redo walk the stack`() {
        val h = LayoutHistory(layout(1))
        h.record(layout(2))
        h.record(layout(3))
        assertEquals(layout(2), h.undo())
        assertEquals(layout(1), h.undo())
        assertNull(h.undo())
        assertEquals(layout(2), h.redo())
        assertEquals(layout(3), h.redo())
    }

    @Test fun `the past is bounded by capacity`() {
        val h = LayoutHistory(layout(0), capacity = 2)
        h.record(layout(1))
        h.record(layout(2))
        h.record(layout(3)) // past now holds {1,2} after dropping 0
        assertEquals(layout(2), h.undo())
        assertEquals(layout(1), h.undo())
        assertNull(h.undo()) // 0 was evicted
    }

    @Test(expected = IllegalArgumentException::class)
    fun `capacity below one is rejected`() {
        LayoutHistory(layout(5), capacity = 0)
    }
}
