package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelNodeTest {

    private fun client(response: ModelResponse) = ModelClient { _, _ -> response }

    private fun run(node: ModelNode) =
        runBlocking { node.run(GraphState(), NodeContext()) } as NodeResult.Continue

    @Test fun `text response becomes an assistant turn and spends a step`() {
        val node = ModelNode(client(ModelResponse(VisionMessage.text(Role.ASSISTANT, "hello"), modelId = "gpt-x")))
        val update = run(node).update
        assertEquals("hello", update.appendMessages.single().text())
        assertTrue(update.spendStep)
        assertEquals("gpt-x", update.scratch["lastModel"])
    }

    @Test fun `error response degrades to a spoken error`() {
        val node = ModelNode(client(ModelResponse(VisionMessage.text(Role.ASSISTANT, ""), error = "429 rate limited")))
        val update = run(node).update
        assertTrue(update.appendMessages.single().text().contains("429"))
        assertTrue(update.spendStep)
    }

    @Test fun `a throwing client degrades to a spoken error`() {
        val node = ModelNode(ModelClient { _, _ -> throw RuntimeException("network down") })
        assertTrue(run(node).update.appendMessages.single().text().contains("network down"))
    }

    @Test fun `tool calls in the response are passed through`() {
        val msg = VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall("c1", "get_time", "{}")))
        val node = ModelNode(client(ModelResponse(msg)))
        assertEquals(1, run(node).update.appendMessages.single().toolCalls().size)
    }
}
