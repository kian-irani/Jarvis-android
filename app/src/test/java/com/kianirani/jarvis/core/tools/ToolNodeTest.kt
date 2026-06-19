package com.kianirani.jarvis.core.tools

import com.kianirani.jarvis.core.agent.ActionRisk
import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolNodeTest {

    private fun tool(
        name: String,
        trust: ActionRisk = ActionRisk.AUTO,
        readOnly: Boolean = false,
        body: () -> String = { "ran:$name" },
    ) = object : VisionTool {
        override val spec = ToolSpec(name, "desc", JsonObject(emptyMap()), trust, readOnly)
        override suspend fun execute(args: JsonObject, ctx: ToolContext) =
            ContentPart.ToolResult("placeholder", name, listOf(ContentPart.Text(body())))
    }

    private fun stateWithCall(name: String, id: String = "c1", args: String = "{}") =
        GraphState(messages = listOf(VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall(id, name, args)))))

    /** A single assistant turn carrying several tool calls, in (name, id) order. */
    private fun stateWithCalls(calls: List<Pair<String, String>>) =
        GraphState(
            messages = listOf(
                VisionMessage(Role.ASSISTANT, calls.map { (name, id) -> ContentPart.ToolCall(id, name, "{}") }),
            ),
        )

    private fun resultNames(r: NodeResult.Continue) =
        r.update.appendMessages.map { (it.content.single() as ContentPart.ToolResult).name }

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

    // --- VCF-T3: read-only fan-out, mutating stays sequential, order preserved ---

    /** A read-only tool that takes [delayMs] of (virtual) time, for concurrency proofs. */
    private fun slowRead(name: String, delayMs: Long) = object : VisionTool {
        override val spec = ToolSpec(name, "d", JsonObject(emptyMap()), ActionRisk.AUTO, readOnly = true)
        override suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult {
            delay(delayMs)
            return ContentPart.ToolResult("p", name, listOf(ContentPart.Text("ran:$name")))
        }
    }

    @Test fun `read-only tools in one step run concurrently`() = runTest {
        // Two read-only tools that each take 100ms. Concurrent → ~100ms virtual time;
        // sequential would be ~200ms. The delay overlap is the proof.
        val node = ToolNode(ToolRegistry(listOf(slowRead("get_time", 100), slowRead("get_battery", 100))))
        val r = node.run(stateWithCalls(listOf("get_time" to "a", "get_battery" to "b")), NodeContext()) as NodeResult.Continue
        assertEquals(listOf("get_time", "get_battery"), resultNames(r)) // order preserved
        assertEquals(100L, testScheduler.currentTime) // overlapped, not 200
    }

    @Test fun `mutating tools run sequentially in call order`() = runTest {
        val order = mutableListOf<String>()
        fun rec(name: String) = object : VisionTool {
            override val spec = ToolSpec(name, "d", JsonObject(emptyMap()), ActionRisk.AUTO, readOnly = false)
            override suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult {
                order += name
                return ContentPart.ToolResult("p", name, listOf(ContentPart.Text("ran:$name")))
            }
        }
        val node = ToolNode(ToolRegistry(listOf(rec("write_a"), rec("write_b"))))
        val r = node.run(stateWithCalls(listOf("write_a" to "a", "write_b" to "b")), NodeContext()) as NodeResult.Continue
        assertEquals(listOf("write_a", "write_b"), order) // executed in order
        assertEquals(listOf("write_a", "write_b"), resultNames(r)) // results in call order
    }

    @Test fun `mixed read-only and mutating calls keep call order in the output`() = runTest {
        val node = ToolNode(
            ToolRegistry(
                listOf(
                    tool("recall", readOnly = true) { "memory" }, // read-only
                    tool("open_app") { "opened" }, // AUTO but mutating (default readOnly=false)
                ),
            ),
        )
        val r = node.run(stateWithCalls(listOf("recall" to "a", "open_app" to "b")), NodeContext()) as NodeResult.Continue
        assertEquals(listOf("recall", "open_app"), resultNames(r))
    }
}
