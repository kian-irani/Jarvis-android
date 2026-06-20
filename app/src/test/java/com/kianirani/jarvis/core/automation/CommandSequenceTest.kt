package com.kianirani.jarvis.core.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Multi-step command acceptance: split on connectives (EN/FA), single vs multi, plan indices. Pure. */
class CommandSequenceTest {

    @Test fun `splits on then and and-then`() {
        assertEquals(
            listOf("open Maps", "search for coffee", "navigate"),
            CommandSequence.split("open Maps then search for coffee and then navigate"),
        )
    }

    @Test fun `splits on persian connectives`() {
        assertEquals(listOf("نقشه را باز کن", "قهوه را جستجو کن"), CommandSequence.split("نقشه را باز کن سپس قهوه را جستجو کن"))
    }

    @Test fun `single command is one step`() {
        assertEquals(listOf("call mom"), CommandSequence.split("call mom"))
        assertFalse(CommandSequence.isMultiStep("call mom"))
        assertTrue(CommandSequence.isMultiStep("call mom then text dad"))
    }

    @Test fun `plan numbers the steps`() {
        val plan = CommandSequence.plan("a then b then c")
        assertEquals(listOf(1, 2, 3), plan.map { it.index })
        assertEquals("b", plan[1].value)
    }

    @Test fun `blank segments are dropped`() {
        assertEquals(listOf("a", "b"), CommandSequence.split("a ; ; b"))
    }
}
