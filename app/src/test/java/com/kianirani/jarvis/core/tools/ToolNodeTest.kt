package com.kianirani.jarvis.core.tools

import com.kianirani.jarvis.core.agent.ActionRisk
import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolNodeTest {

    private fun tool(name: String, trust: ActionRisk = ActionRisk.AUTO, body: () -> String = { "ran:$name" }) =
        object : VisionTool {
            override val spec = ToolSpec(name, "desc", JsonObject(emptyMap()), trust)
            override suspend fun execute(args: JsonObject, ctx: ToolContext) =
                ContentPart.ToolResult("placeholder", name, listOf(ContentPart.Text(body())))
        }

    private fun stateWithCall(name: String, id: String = "c1", args: String = "{}") =
        GraphState(messages = listOf(VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall(id, name, args)))))

    private fun run(node: ToolNode, state: GraphState, ctx: NodeContext = NodeContext()) =
        runBlocking { node.run(state, ctx) }

    @Test fun `read-only tool executes and yields a tool result and observation`() {
        val node = ToolNode(ToolRegistry(listOf(tool("get_time"))))
        val r = run(node, stateWithCall("get_time")) as NodeResult.Continue
        val msg = r.update.appendMessages.single()
        assertEquals(Role.TOOL, msg.role)
        val result = msg.content.single() as ContentPart.ToolResult
        assertEquals("c1", result.callId) // node stamps the call id
        assertEquals("ran:get_time", (result.content.single() as ContentPart.Text).text)
        assertFalse(r.update.appendObservations.single().isError)
    }

    @Test fun `unknown tool degrades to an error observation`() {
        val node = ToolNode(ToolRegistry(emptyList()))
        val r = run(node, stateWithCall("ghost")) as NodeResult.Continue
        val result = r.update.appendMessages.single().content.single() as ContentPart.ToolResult
        assertTrue(result.isError)
        assertTrue(result.content.toString().contains("unknown tool"))
        assertTrue(r.update.appendObservations.single().isError)
    }

    @Test fun `a throwing tool becomes an error observation not a crash`() {
        val node = ToolNode(ToolRegistry(listOf(tool("boom") { throw RuntimeException("nope") })))
        val r = run(node, stateWithCall("boom")) as NodeResult.Continue
        val result = r.update.appendMessages.single().content.single() as ContentPart.ToolResult
        assertTrue(result.isError)
    }

    @Test fun `a critical tool without approval interrupts for confirmation`() {
        val node = ToolNode(ToolRegistry(listOf(tool("wipe", ActionRisk.CRITICAL))))
        val r = run(node, stateWithCall("wipe", id = "x1"))
        val interrupt = r as NodeResult.Interrupt
        assertEquals("confirm_tool:wipe", interrupt.reason)
    }

    @Test fun `an always-critical name interrupts even when declared auto`() {
        val node = ToolNode(ToolRegistry(listOf(tool("send_sms", ActionRisk.AUTO))))
        assertTrue(run(node, stateWithCall("send_sms")) is NodeResult.Interrupt)
    }

    @Test fun `a critical tool runs once its call id is pre-approved`() {
        val node = ToolNode(ToolRegistry(listOf(tool("wipe", ActionRisk.CRITICAL))))
        val r = run(node, stateWithCall("wipe", id = "x1"), NodeContext(preApproved = setOf("x1")))
        assertTrue(r is NodeResult.Continue)
    }

    @Test fun `no tool calls is a no-op continue`() {
        val node = ToolNode(ToolRegistry(emptyList()))
        val r = run(node, GraphState(messages = listOf(VisionMessage.text(Role.ASSISTANT, "hi")))) as NodeResult.Continue
        assertTrue(r.update.appendMessages.isEmpty())
    }
}
