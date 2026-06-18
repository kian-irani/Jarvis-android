package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectNodeTest {

    private fun run(node: ReflectNode, state: GraphState) =
        runBlocking { node.run(state, NodeContext()) } as NodeResult.Continue

    @Test fun `without the flag it is a no-op and never calls the model`() {
        val node = ReflectNode(ModelClient { _, _ -> throw AssertionError("should not be called") })
        assertTrue(run(node, GraphState()).update.appendMessages.isEmpty())
    }

    @Test fun `with the flag it appends an improved answer and clears the flag`() {
        val node = ReflectNode(ModelClient { _, _ -> ModelResponse(VisionMessage.text(Role.ASSISTANT, "better answer")) })
        val state = GraphState(
            messages = listOf(VisionMessage.text(Role.ASSISTANT, "ok")),
            scratch = mapOf("needsReflection" to "true"),
        )
        val update = run(node, state).update
        assertEquals("better answer", update.appendMessages.single().text())
        assertEquals("false", update.scratch["needsReflection"])
    }

    @Test fun `a failing reflection still clears the flag and adds nothing`() {
        val node = ReflectNode(ModelClient { _, _ -> throw RuntimeException("x") })
        val update = run(node, GraphState(scratch = mapOf("needsReflection" to "true"))).update
        assertEquals("false", update.scratch["needsReflection"])
        assertTrue(update.appendMessages.isEmpty())
    }
}
