package com.kianirani.jarvis.core.eval

import com.kianirani.jarvis.core.agent.ModelClient
import com.kianirani.jarvis.core.agent.ModelResponse
import com.kianirani.jarvis.core.agent.VisionAgent
import com.kianirani.jarvis.core.agent.ActionRisk
import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.tools.ToolContext
import com.kianirani.jarvis.core.tools.ToolRegistry
import com.kianirani.jarvis.core.tools.ToolSpec
import com.kianirani.jarvis.core.tools.VisionTool
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RunTraceTest {

    private fun getTimeTool() = object : VisionTool {
        override val spec = ToolSpec("get_time", "time", JsonObject(emptyMap()), ActionRisk.AUTO)
        override suspend fun execute(args: JsonObject, ctx: ToolContext) =
            ContentPart.ToolResult("?", "get_time", listOf(ContentPart.Text("12:00")))
    }

    @Test fun `records a full tool-using agent run`() {
        val client = ModelClient { messages, _ ->
            val sawResult = messages.any { m -> m.content.any { it is ContentPart.ToolResult } }
            val msg = if (sawResult) {
                VisionMessage.text(Role.ASSISTANT, "it is 12:00")
            } else {
                VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall("c1", "get_time", "{}")))
            }
            ModelResponse(msg)
        }
        val agent = VisionAgent(client, ToolRegistry(listOf(getTimeTool())))
        val trace = runBlocking { TraceRecorder.record("t1", agent.run("what time?")) }

        assertTrue(trace.completed)
        assertFalse(trace.failed)
        assertEquals(listOf("model", "tools", "model"), trace.nodeVisits)
        assertEquals(3, trace.stepCount)
        assertEquals(1, trace.toolRuns)
        assertEquals("it is 12:00", trace.finalState!!.messages.last().text())
    }

    @Test fun `summarizes an interrupted run`() {
        val trace = TraceRecorder.of(
            "t",
            listOf(GraphEvent.NodeStart("tools"), GraphEvent.Interrupted("confirm_tool:wipe", kotlinx.serialization.json.JsonObject(emptyMap()))),
        )
        assertTrue(trace.interrupted)
        assertFalse(trace.completed)
        assertEquals(0, trace.toolRuns)
    }

    @Test fun `summarizes a failed run`() {
        val trace = TraceRecorder.of("t", listOf(GraphEvent.NodeStart("model"), GraphEvent.Failed("boom")))
        assertTrue(trace.failed)
        assertFalse(trace.completed)
    }
}
