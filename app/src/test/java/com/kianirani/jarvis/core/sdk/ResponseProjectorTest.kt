package com.kianirani.jarvis.core.sdk

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.StateUpdate
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.sdk.ResponseProjector.asResponses
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-F3 acceptance: the GraphEvent → VisionResponse projection is pure & deterministic and
 * the Flow operator matches the list fold. No device.
 */
class ResponseProjectorTest {

    private fun doneWith(text: String) =
        GraphEvent.Done(GraphState(messages = listOf(VisionMessage.text(Role.ASSISTANT, text))))

    @Test fun `tokens accumulate into cumulative text and done finishes`() {
        val out = ResponseProjector.fold(
            "s1",
            listOf(GraphEvent.Token("Hel"), GraphEvent.Token("lo"), GraphEvent.Done(GraphState())),
        )
        assertEquals(listOf("Hel", "Hello", "Hello"), out.map { it.text })
        assertEquals(listOf(false, false, true), out.map { it.finished })
        assertTrue(out.all { it.sessionId == "s1" })
    }

    @Test fun `no-token run extracts the last assistant message on done`() {
        val out = ResponseProjector.fold("s1", listOf(GraphEvent.NodeStart("model"), doneWith("final answer")))
        assertEquals(1, out.size)
        assertEquals("final answer", out.single().text)
        assertTrue(out.single().finished)
    }

    @Test fun `interrupt yields an unfinished response awaiting confirmation`() {
        val out = ResponseProjector.fold(
            "s1",
            listOf(GraphEvent.Interrupted("confirm_tool:send_sms", JsonObject(emptyMap()))),
        )
        val r = out.single()
        assertFalse(r.finished)
        assertTrue(r.awaitingConfirmation)
    }

    @Test fun `failure yields a finished response carrying the error and partial text`() {
        val out = ResponseProjector.fold("s1", listOf(GraphEvent.Token("part"), GraphEvent.Failed("no network")))
        val r = out.last()
        assertTrue(r.finished)
        assertEquals("no network", r.error)
        assertEquals("part", r.text)
    }

    @Test fun `internal node and tool events emit nothing`() {
        val out = ResponseProjector.fold(
            "s1",
            listOf(
                GraphEvent.NodeStart("a"),
                GraphEvent.NodeEnd("a", StateUpdate()),
                GraphEvent.ToolStart(ContentPart.ToolCall("c1", "t", "{}")),
                GraphEvent.ToolEnd(ContentPart.ToolResult("c1", "t", emptyList())),
            ),
        )
        assertTrue(out.isEmpty())
    }

    @Test fun `asResponses flow matches the list fold`() = runTest {
        val events = listOf(GraphEvent.Token("a"), GraphEvent.Token("b"), GraphEvent.Done(GraphState()))
        val viaFlow = events.asFlow().asResponses("s1").toList()
        assertEquals(ResponseProjector.fold("s1", events), viaFlow)
    }
}
