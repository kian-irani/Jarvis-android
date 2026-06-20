package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.tools.DeviceCommandTool
import com.kianirani.jarvis.core.tools.RecallTool
import com.kianirani.jarvis.core.tools.ToolRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * VCF-LIVE-6 — proves THIS session's wiring (v128) end-to-end without a device: the *real*
 * [DeviceCommandTool] and [RecallTool] (the same tools BrainFacadeModule now puts in the
 * gateway, replacing the old empty registry) actually execute inside a [VisionAgent] ReAct
 * loop. The model is faked; the tools are the production classes fed test bridges. This is the
 * headless half of "live VCF" — only the provider FC round-trip still needs on-device confirm.
 */
class AgentDeviceToolWiringTest {

    /** A model that asks for one tool call, then answers once it sees the tool's result. */
    private fun modelThatCalls(toolName: String, argsJson: String, finalAnswer: String) =
        ModelClient { messages, _ ->
            val sawResult = messages.any { m -> m.content.any { it is ContentPart.ToolResult } }
            val reply = if (sawResult) {
                VisionMessage.text(Role.ASSISTANT, finalAnswer)
            } else {
                VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall("c1", toolName, argsJson)))
            }
            ModelResponse(reply, modelId = "fake")
        }

    private fun toolResultText(state: com.kianirani.jarvis.core.graph.GraphState): String =
        state.messages
            .filter { it.role == Role.TOOL }
            .flatMap { it.content.filterIsInstance<ContentPart.ToolResult>() }
            .flatMap { it.content.filterIsInstance<ContentPart.Text>() }
            .joinToString(" ") { it.text }

    @Test fun `agent runs the real DeviceCommandTool and answers from its output`() {
        var ranWith: String? = null
        val device = DeviceCommandTool { cmd -> ranWith = cmd; "Battery at 80%." }
        val agent = VisionAgent(
            modelThatCalls("device_command", """{"command":"battery"}""", "Your battery is at 80%."),
            ToolRegistry(listOf(device, RecallTool { _, _ -> emptyList() })),
        )

        val events = runBlocking { agent.run("what's my battery?").toList() }
        val done = events.last() as GraphEvent.Done

        // The real device tool was invoked with the model's argument.
        assertEquals("battery", ranWith)
        // device_command is AUTO trust → no confirmation pause, the run completes.
        assertFalse(events.any { it is GraphEvent.Interrupted })
        // The tool result actually flowed back into the conversation…
        assertTrue(toolResultText(done.state).contains("Battery at 80%"))
        // …and the agent answered from it.
        assertEquals("Your battery is at 80%.", done.state.messages.last().text())
    }

    @Test fun `an unrecognized device command comes back as an error the agent can see`() {
        val device = DeviceCommandTool { null } // nothing matches → error result (honest)
        val agent = VisionAgent(
            modelThatCalls("device_command", """{"command":"do a backflip"}""", "I can't do that on the device."),
            ToolRegistry(listOf(device)),
        )

        val done = runBlocking { agent.run("do a backflip").toList() }.last() as GraphEvent.Done
        val toolMsg = done.state.messages.first { it.role == Role.TOOL }
            .content.filterIsInstance<ContentPart.ToolResult>().single()
        assertTrue(toolMsg.isError)
        assertTrue(toolResultText(done.state).contains("Not a device command"))
    }

    @Test fun `the real RecallTool surfaces stored memories to the agent`() {
        val recall = RecallTool { query, _ ->
            assertEquals("dark mode", query)
            listOf("user prefers dark mode")
        }
        val agent = VisionAgent(
            modelThatCalls("recall", """{"query":"dark mode"}""", "You prefer dark mode."),
            ToolRegistry(listOf(recall, DeviceCommandTool { null })),
        )

        val done = runBlocking { agent.run("what do I prefer?").toList() }.last() as GraphEvent.Done
        assertTrue(toolResultText(done.state).contains("user prefers dark mode"))
        assertEquals("You prefer dark mode.", done.state.messages.last().text())
    }
}
