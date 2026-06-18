package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.tools.ToolContext
import com.kianirani.jarvis.core.tools.ToolRegistry
import com.kianirani.jarvis.core.tools.ToolSpec
import com.kianirani.jarvis.core.tools.VisionTool
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** End-to-end: proves the full VCF stack (G1+G2+A1+A2+T1+T3) runs together with fakes. */
class VisionAgentTest {

    private fun tool(name: String, trust: com.kianirani.jarvis.core.agent.ActionRisk, result: String = "ok") =
        object : VisionTool {
            override val spec = ToolSpec(name, "desc", JsonObject(emptyMap()), trust)
            override suspend fun execute(args: JsonObject, ctx: ToolContext) =
                ContentPart.ToolResult("?", name, listOf(ContentPart.Text(result)))
        }

    @Test fun `agent reasons, calls a tool, observes, and answers`() {
        val client = ModelClient { messages, _ ->
            val sawToolResult = messages.any { m -> m.content.any { it is ContentPart.ToolResult } }
            val reply = if (sawToolResult) {
                VisionMessage.text(Role.ASSISTANT, "it is 12:00")
            } else {
                VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall("c1", "get_time", "{}")))
            }
            ModelResponse(reply, modelId = "fake")
        }
        val agent = VisionAgent(client, ToolRegistry(listOf(tool("get_time", ActionRisk.AUTO, "12:00"))))
        val events = runBlocking { agent.run("what time is it?").toList() }

        assertTrue(events.last() is GraphEvent.Done)
        val done = events.last() as GraphEvent.Done
        assertEquals("it is 12:00", done.state.messages.last().text())
        assertTrue("the tool actually executed", done.state.messages.any { it.role == Role.TOOL })
    }

    @Test fun `a critical tool pauses the whole agent for confirmation`() {
        val client = ModelClient { _, _ ->
            ModelResponse(VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall("c1", "wipe", "{}"))))
        }
        val agent = VisionAgent(client, ToolRegistry(listOf(tool("wipe", ActionRisk.CRITICAL))))
        val events = runBlocking { agent.run("wipe my data").toList() }

        assertTrue(events.any { it is GraphEvent.Interrupted })
        assertTrue(events.none { it is GraphEvent.Done })
    }

    @Test fun `a plain answer with no tools completes in one turn`() {
        val client = ModelClient { _, _ -> ModelResponse(VisionMessage.text(Role.ASSISTANT, "hello there")) }
        val agent = VisionAgent(client, ToolRegistry(emptyList()))
        val done = runBlocking { agent.run("hi").toList() }.last() as GraphEvent.Done
        assertEquals("hello there", done.state.messages.last().text())
    }
}
