package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.END
import com.kianirani.jarvis.core.graph.Node
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.StateUpdate
import com.kianirani.jarvis.core.graph.VisionGraph
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.tools.ToolContext
import com.kianirani.jarvis.core.tools.ToolSpec
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentAsToolTest {

    private fun echoSub() = VisionGraph.Builder()
        .addNode(
            "a",
            Node { state, _ ->
                NodeResult.Continue(
                    StateUpdate(appendMessages = listOf(VisionMessage.text(Role.ASSISTANT, "did: ${state.messages.last().text()}"))),
                )
            },
        )
        .addEdge("a", END).setEntry("a").compile()

    @Test fun `runs the sub-agent and returns its final answer`() {
        val tool = AgentAsTool(echoSub(), ToolSpec("researcher", "delegate research"))
        val result = runBlocking { tool.execute(buildJsonObject { put("task", "find X") }, ToolContext()) }
        assertEquals("did: find X", (result.content.single() as ContentPart.Text).text)
        assertFalse(result.isError)
        assertEquals("researcher", result.name)
    }

    @Test fun `a failing sub-agent yields an error result`() {
        val sub = VisionGraph.Builder()
            .addNode("a", Node { _, _ -> throw RuntimeException("sub boom") })
            .setEntry("a").compile()
        val tool = AgentAsTool(sub, ToolSpec("flaky", "x"))
        val result = runBlocking { tool.execute(buildJsonObject { put("task", "go") }, ToolContext()) }
        assertTrue(result.isError)
        assertEquals("sub boom", (result.content.single() as ContentPart.Text).text)
    }

    @Test fun `missing task argument runs with empty input`() {
        val tool = AgentAsTool(echoSub(), ToolSpec("researcher", "x"))
        val result = runBlocking { tool.execute(buildJsonObject { }, ToolContext()) }
        assertEquals("did: ", (result.content.single() as ContentPart.Text).text)
        assertFalse(result.isError)
    }
}
