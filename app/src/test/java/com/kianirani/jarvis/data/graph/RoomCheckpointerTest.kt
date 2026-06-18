package com.kianirani.jarvis.data.graph

import com.kianirani.jarvis.brain.data.db.CheckpointDao
import com.kianirani.jarvis.brain.data.db.CheckpointEntity
import com.kianirani.jarvis.core.graph.ActionPlan
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.PlanStep
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomCheckpointerTest {

    /** In-memory stand-in for the Room DAO so the checkpointer logic is JVM-testable. */
    private class FakeDao : CheckpointDao {
        val rows = mutableListOf<CheckpointEntity>()

        override suspend fun insert(c: CheckpointEntity) {
            rows.removeAll { it.threadId == c.threadId && it.seq == c.seq }
            rows.add(c)
        }

        override suspend fun latest(threadId: String): CheckpointEntity? =
            rows.filter { it.threadId == threadId }.maxByOrNull { it.seq }

        override suspend fun maxSeq(threadId: String): Int? =
            rows.filter { it.threadId == threadId }.maxOfOrNull { it.seq }

        override suspend fun all(threadId: String): List<CheckpointEntity> =
            rows.filter { it.threadId == threadId }.sortedBy { it.seq }
    }

    @Test fun `empty thread has no checkpoint`() = runBlocking {
        val cp = RoomCheckpointer(FakeDao())
        assertNull(cp.load("t"))
        assertNull(cp.loadCursor("t"))
        assertEquals(emptyList<GraphState>(), cp.history("t"))
    }

    @Test fun `save then load returns the state and cursor`() = runBlocking {
        val cp = RoomCheckpointer(FakeDao())
        val state = GraphState(messages = listOf(VisionMessage.text(Role.USER, "hi")), remainingSteps = 7)
        cp.save("t", state, "nodeA")
        assertEquals(state, cp.load("t"))
        assertEquals("nodeA", cp.loadCursor("t"))
    }

    @Test fun `latest revision wins and history keeps order`() = runBlocking {
        val dao = FakeDao()
        val cp = RoomCheckpointer(dao)
        val s1 = GraphState(remainingSteps = 5)
        val s2 = GraphState(remainingSteps = 4, messages = listOf(VisionMessage.text(Role.ASSISTANT, "step")))
        cp.save("t", s1, "a")
        cp.save("t", s2, "b")
        assertEquals(s2, cp.load("t"))
        assertEquals("b", cp.loadCursor("t"))
        assertEquals(listOf(s1, s2), cp.history("t"))
        assertEquals(2, dao.maxSeq("t"))
    }

    @Test fun `rich state round-trips through json`() = runBlocking {
        val cp = RoomCheckpointer(FakeDao())
        val state = GraphState(
            messages = listOf(VisionMessage.text(Role.USER, "plan it")),
            plan = ActionPlan("goal", listOf(PlanStep("s1", "do"))),
            remainingSteps = 9,
            scratch = mapOf("k" to "v"),
            sessionId = "sess",
        )
        cp.save("t", state, "n")
        assertEquals(state, cp.load("t"))
    }

    @Test fun `threads are isolated`() = runBlocking {
        val cp = RoomCheckpointer(FakeDao())
        cp.save("a", GraphState(remainingSteps = 1), "x")
        cp.save("b", GraphState(remainingSteps = 2), "y")
        assertEquals(1, cp.load("a")!!.remainingSteps)
        assertEquals("y", cp.loadCursor("b"))
    }
}
