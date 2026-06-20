package com.kianirani.jarvis.core.goal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A4 acceptance: order respects dependencies, nextActionable surfaces ready tasks, blocked lists
 * held-back tasks, progress/isComplete track completion, and cycles are detected. Pure.
 */
class GoalGraphTest {

    private fun t(id: String, deps: Set<String> = emptySet(), status: TaskStatus = TaskStatus.TODO) =
        GoalTask(id, id, deps, status)

    @Test fun `order places dependencies before dependents`() {
        val g = GoalGraph(listOf(t("c", setOf("b")), t("b", setOf("a")), t("a")))
        val order = g.order().map { it.id }
        assertTrue(order.indexOf("a") < order.indexOf("b"))
        assertTrue(order.indexOf("b") < order.indexOf("c"))
    }

    @Test fun `nextActionable surfaces only tasks whose deps are done`() {
        val g = GoalGraph(listOf(t("a", status = TaskStatus.DONE), t("b", setOf("a")), t("c", setOf("b"))))
        assertEquals(listOf("b"), g.nextActionable().map { it.id }) // a done, b ready, c still blocked
    }

    @Test fun `blocked lists tasks with an unmet dependency`() {
        val g = GoalGraph(listOf(t("a"), t("b", setOf("a"))))
        assertEquals(listOf("b"), g.blocked().map { it.id }) // a not done yet
    }

    @Test fun `progress and isComplete track done count`() {
        val g = GoalGraph(listOf(t("a", status = TaskStatus.DONE), t("b", status = TaskStatus.DONE), t("c")))
        assertEquals(2f / 3f, g.progress(), 1e-6f)
        assertFalse(g.isComplete())
        val done = GoalGraph(listOf(t("a", status = TaskStatus.DONE)))
        assertTrue(done.isComplete())
    }

    @Test fun `a cycle is detected and order is empty`() {
        val g = GoalGraph(listOf(t("a", setOf("b")), t("b", setOf("a"))))
        assertTrue(g.hasCycle())
        assertTrue(g.order().isEmpty())
    }

    @Test fun `unknown dependency ids are ignored`() {
        val g = GoalGraph(listOf(t("a", setOf("ghost"))))
        assertFalse(g.hasCycle())
        assertEquals(listOf("a"), g.nextActionable().map { it.id }) // ghost dep ignored → actionable
    }

    @Test fun `empty goal has zero progress and is not complete`() {
        val g = GoalGraph(emptyList())
        assertEquals(0f, g.progress(), 1e-6f)
        assertFalse(g.isComplete())
    }
}
