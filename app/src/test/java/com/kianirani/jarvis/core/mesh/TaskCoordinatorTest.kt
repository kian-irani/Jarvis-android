package com.kianirani.jarvis.core.mesh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Coordinator acceptance: least-loaded-first assignment, capacity respected, overflow queues. Pure. */
class TaskCoordinatorTest {

    @Test fun `tasks spread to the least-loaded node first`() {
        val nodes = listOf(WorkerNode("a", maxSlots = 2), WorkerNode("b", maxSlots = 2, activeTasks = 1))
        val assign = TaskCoordinator.assign(listOf("t1", "t2"), nodes)
        // a has 2 free, b has 1 → first task to a, second still a (2>1)
        assertEquals("a", assign["t1"])
    }

    @Test fun `capacity is respected and overflow is unassigned`() {
        val nodes = listOf(WorkerNode("a", maxSlots = 1), WorkerNode("b", maxSlots = 1))
        val assign = TaskCoordinator.assign(listOf("t1", "t2", "t3"), nodes)
        assertEquals(2, assign.size) // only 2 slots total
        assertFalse(assign.containsKey("t3")) // queued
    }

    @Test fun `full nodes get nothing`() {
        val nodes = listOf(WorkerNode("a", maxSlots = 1, activeTasks = 1))
        assertEquals(emptyMap<String, String>(), TaskCoordinator.assign(listOf("t1"), nodes))
        assertEquals(0, TaskCoordinator.totalFreeSlots(nodes))
    }
}
