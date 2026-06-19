package com.kianirani.jarvis.core.memory

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-B3 acceptance: the session window stays bounded, hands the right slice off for
 * summarization, folds the summary into the prompt, and degrades gracefully when the
 * summarizer yields nothing. Pure, no model, no device.
 */
class SessionMemoryTest {

    private fun user(t: String) = VisionMessage.text(Role.USER, t)

    private fun fill(mem: SessionMemory, n: Int) {
        repeat(n) { mem.add(user("turn-$it")) }
    }

    @Test fun `window is just the turns until the first compaction`() {
        val mem = SessionMemory(maxTurns = 6, keepRecent = 3)
        fill(mem, 4)
        assertFalse(mem.needsCompaction())
        assertTrue(mem.compactable().isEmpty())
        assertEquals(4, mem.window().size)
        assertNull(mem.summary())
    }

    @Test fun `compactable is the oldest turns beyond keepRecent once over budget`() {
        val mem = SessionMemory(maxTurns = 6, keepRecent = 3)
        fill(mem, 7) // over budget (7 > 6) → compact oldest 7-3 = 4
        assertTrue(mem.needsCompaction())
        val slice = mem.compactable()
        assertEquals(4, slice.size)
        assertEquals("turn-0", slice.first().text())
        assertEquals("turn-3", slice.last().text())
    }

    @Test fun `applySummary drops compacted turns and prepends the summary to the window`() {
        val mem = SessionMemory(maxTurns = 6, keepRecent = 3)
        fill(mem, 7)
        mem.applySummary("user is planning a trip to Shiraz")
        assertEquals(3, mem.size()) // only keepRecent remain
        val window = mem.window()
        assertEquals(Role.SYSTEM, window.first().role)
        assertTrue(window.first().text().contains("Shiraz"))
        // the 3 newest turns survived
        assertEquals(listOf("turn-4", "turn-5", "turn-6"), window.drop(1).map { it.text() })
    }

    @Test fun `a blank summary preserves all turns (graceful summarizer failure)`() {
        val mem = SessionMemory(maxTurns = 6, keepRecent = 3)
        fill(mem, 7)
        mem.applySummary("   ")
        assertEquals(7, mem.size())
        assertNull(mem.summary())
    }

    @Test fun `successive compactions accumulate the running summary`() {
        val mem = SessionMemory(maxTurns = 6, keepRecent = 3)
        fill(mem, 7)
        mem.applySummary("fact A")
        fill(mem, 4) // 3 + 4 = 7 again → over budget
        mem.applySummary("fact B")
        val summary = mem.summary()!!
        assertTrue(summary.contains("fact A"))
        assertTrue(summary.contains("fact B"))
        assertEquals(Role.SYSTEM, mem.window().first().role)
    }

    @Test fun `clear resets turns and summary`() {
        val mem = SessionMemory(maxTurns = 6, keepRecent = 3)
        fill(mem, 7)
        mem.applySummary("something")
        mem.clear()
        assertEquals(0, mem.size())
        assertNull(mem.summary())
        assertTrue(mem.window().isEmpty())
    }

    @Test fun `non-text turns are carried through the window verbatim`() {
        val mem = SessionMemory(maxTurns = 6, keepRecent = 3)
        val toolMsg = VisionMessage(Role.TOOL, listOf(ContentPart.ToolResult("c1", "recall", listOf(ContentPart.Text("hit")))))
        mem.add(toolMsg)
        assertEquals(toolMsg, mem.window().single())
    }
}
