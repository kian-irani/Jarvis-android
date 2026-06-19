package com.kianirani.jarvis.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * DS-C3 acceptance: the universal clipboard tracks the newest clip across devices, keeps a
 * bounded history, ignores blanks, and merges order-independently/idempotently. Pure.
 */
class ClipboardSyncTest {

    @Test fun `the most recent copy is current`() {
        val c = ClipboardSync()
        c.copy("first", timestamp = 1, nodeId = "phone")
        c.copy("second", timestamp = 2, nodeId = "phone")
        assertEquals("second", c.current()?.content)
        assertEquals(listOf("second", "first"), c.history().map { it.content })
    }

    @Test fun `blank clips are ignored`() {
        val c = ClipboardSync()
        c.copy("   ", timestamp = 1, nodeId = "phone")
        assertNull(c.current())
    }

    @Test fun `history is capped at the limit, dropping the oldest`() {
        val c = ClipboardSync(historyLimit = 3)
        (1..5).forEach { c.copy("clip$it", timestamp = it.toLong(), nodeId = "phone") }
        assertEquals(listOf("clip5", "clip4", "clip3"), c.history().map { it.content })
    }

    @Test fun `merge is order-independent`() {
        fun phone() = ClipboardSync().apply { copy("p1", 1, "phone"); copy("p2", 3, "phone") }
        fun desktop() = ClipboardSync().apply { copy("d1", 2, "desktop"); copy("d2", 4, "desktop") }
        val ab = phone().merge(desktop())
        val ba = desktop().merge(phone())
        assertEquals(ab.current(), ba.current())
        assertEquals("d2", ab.current()?.content) // newest overall (t=4)
        assertEquals(ab.history(), ba.history())
    }

    @Test fun `merge is idempotent`() {
        val other = ClipboardSync().apply { copy("x", 1, "a"); copy("y", 2, "b") }
        val once = ClipboardSync().merge(other)
        val twice = ClipboardSync().merge(other).merge(other)
        assertEquals(once.history(), twice.history())
    }

    @Test fun `equal timestamps break by nodeId deterministically`() {
        val c = ClipboardSync()
        c.copy("fromAlpha", timestamp = 5, nodeId = "alpha")
        c.copy("fromBeta", timestamp = 5, nodeId = "beta")
        assertEquals("fromBeta", c.current()?.content) // "beta" > "alpha"
    }
}
