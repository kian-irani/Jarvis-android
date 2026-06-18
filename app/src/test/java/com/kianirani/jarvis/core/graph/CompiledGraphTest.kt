package com.kianirani.jarvis.core.graph

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CompiledGraphTest {

    private val thread = "t1"

    /** A node that appends one assistant message, optionally spending a step / forcing a goto. */
    private fun say(text: String, spend: Boolean = false, goto: String? = null) = Node { _, _ ->
        NodeResult.Continue(
            StateUpdate(appendMessages = listOf(VisionMessage.text(Role.ASSISTANT, text)), spendStep = spend),
            goto,
        )
    }

    private fun CompiledGraph.collect(input: GraphState = GraphState(), ctx: NodeContext = NodeContext()): List<GraphEvent> =
        runBlocking { stream(input, thread, ctx).toList() }

    private fun GraphEvent.tag(): String = when (this) {
        is GraphEvent.NodeStart -> "start:$node"
        is GraphEvent.NodeEnd -> "end:$node"
        is GraphEvent.Interrupted -> "interrupt:$reason"
        is GraphEvent.Done -> "done"
        is GraphEvent.Failed -> "failed:$message"
        else -> "other"
    }

    private fun List<GraphEvent>.texts() = (last() as GraphEvent.Done).state.messages.map { it.text() }

    @Test fun `linear graph runs each node in order then done`() {
        val g = VisionGraph.Builder()
            .addNode("a", say("a")).addNode("b", say("b"))
            .addEdge("a", "b").addEdge("b", END).setEntry("a").compile()
        val events = g.collect()
        assertEquals(listOf("start:a", "end:a", "start:b", "end:b", "done"), events.map { it.tag() })
        assertEquals(listOf("a", "b"), events.texts())
    }

    @Test fun `conditional edge routes on state`() {
        val g = VisionGraph.Builder()
            .addNode("a", say("a")).addNode("x", say("x")).addNode("y", say("y"))
            .addConditionalEdge("a") { st -> if (st.scratch["next"] == "x") "x" else "y" }
            .addEdge("x", END).addEdge("y", END).setEntry("a").compile()
        assertEquals(listOf("a", "x"), g.collect(GraphState(scratch = mapOf("next" to "x"))).texts())
        assertEquals(listOf("a", "y"), g.collect(GraphState(scratch = mapOf("next" to "z"))).texts())
    }

    @Test fun `explicit goto overrides the static edge`() {
        val g = VisionGraph.Builder()
            .addNode("a", say("a", goto = "c")).addNode("b", say("b")).addNode("c", say("c"))
            .addEdge("a", "b").addEdge("b", END).addEdge("c", END).setEntry("a").compile()
        assertEquals(listOf("a", "c"), g.collect().texts())
    }

    @Test fun `a cycle terminates on the step budget`() {
        val g = VisionGraph.Builder()
            .addNode("a", say("a", spend = true)).addNode("b", say("b", spend = true))
            .addEdge("a", "b").addEdge("b", "a").setEntry("a").compile()
        val events = g.collect(GraphState(remainingSteps = 3))
        assertTrue(events.last() is GraphEvent.Done)
        assertEquals(3, events.count { it is GraphEvent.NodeStart })
    }

    @Test fun `an interrupt pauses and persists the cursor`() {
        val cp = FakeCheckpointer()
        val g = gateGraph(cp)
        val events = g.collect(GraphState(messages = listOf(VisionMessage.text(Role.USER, "do it"))))
        assertEquals(listOf("start:gate", "interrupt:confirm"), events.map { it.tag() })
        assertFalse(events.any { it is GraphEvent.Done })
        assertEquals("gate", runBlocking { cp.loadCursor(thread) })
    }

    @Test fun `resume re-enters the same node and completes`() {
        val cp = FakeCheckpointer()
        val g = gateGraph(cp)
        g.collect(GraphState(messages = listOf(VisionMessage.text(Role.USER, "do it"))))
        val resumed = runBlocking { g.resume(thread, VisionMessage.text(Role.USER, "yes")).toList() }
        assertEquals(listOf("start:gate", "end:gate", "done"), resumed.map { it.tag() })
        assertEquals(listOf("do it", "yes", "done"), (resumed.last() as GraphEvent.Done).state.messages.map { it.text() })
    }

    @Test fun `a throwing node ends as failed not aborted`() {
        val g = VisionGraph.Builder()
            .addNode("boom", Node { _, _ -> throw RuntimeException("kaboom") })
            .setEntry("boom").compile()
        val failed = g.collect().last() as GraphEvent.Failed
        assertEquals("kaboom", failed.message)
        assertFalse(failed.aborted)
    }

    @Test fun `compile requires an entry that is registered`() {
        try {
            VisionGraph.Builder().addNode("a", say("a")).compile()
            fail("expected IllegalArgumentException for missing entry")
        } catch (_: IllegalArgumentException) {
        }
        try {
            VisionGraph.Builder().addNode("a", say("a")).setEntry("ghost").compile()
            fail("expected IllegalArgumentException for unregistered entry")
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun gateGraph(cp: Checkpointer): CompiledGraph =
        VisionGraph.Builder()
            .addNode("gate") { _, ctx ->
                if (thread in ctx.preApproved) {
                    NodeResult.Continue(StateUpdate(appendMessages = listOf(VisionMessage.text(Role.ASSISTANT, "done"))))
                } else {
                    NodeResult.Interrupt("confirm")
                }
            }
            .addEdge("gate", END).setEntry("gate").compile(cp)

    private class FakeCheckpointer : Checkpointer {
        private data class Snap(val state: GraphState, val cursor: String)

        private val log = mutableMapOf<String, MutableList<Snap>>()

        override suspend fun save(threadId: String, state: GraphState, cursor: String) {
            log.getOrPut(threadId) { mutableListOf() }.add(Snap(state, cursor))
        }

        override suspend fun load(threadId: String): GraphState? = log[threadId]?.lastOrNull()?.state
        override suspend fun loadCursor(threadId: String): String? = log[threadId]?.lastOrNull()?.cursor
        override suspend fun history(threadId: String): List<GraphState> = log[threadId]?.map { it.state } ?: emptyList()
    }
}
