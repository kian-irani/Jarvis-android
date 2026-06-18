package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Node
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.StateUpdate
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReActAgentFactoryTest {

    /** Model that calls a tool once, then (seeing the tool result) gives a final answer. */
    private val reactModel = Node { state, _ ->
        val sawToolResult = state.messages.any { m -> m.content.any { it is ContentPart.ToolResult } }
        val msg = if (sawToolResult) {
            VisionMessage.text(Role.ASSISTANT, "final answer")
        } else {
            VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall("c1", "get_time", "{}")))
        }
        NodeResult.Continue(StateUpdate(appendMessages = listOf(msg), spendStep = true))
    }

    /** Tool node that answers the most recent tool call with a result message. */
    private val toolNode = Node { state, _ ->
        val call = state.messages.last().toolCalls().first()
        val result = ContentPart.ToolResult(call.id, call.name, listOf(ContentPart.Text("12:00")))
        NodeResult.Continue(StateUpdate(appendMessages = listOf(VisionMessage(Role.TOOL, listOf(result)))))
    }

    private fun run(graph: com.kianirani.jarvis.core.graph.CompiledGraph, input: GraphState = GraphState()) =
        runBlocking { graph.stream(input, "t", NodeContext()).toList() }

    private fun List<GraphEvent>.done() = last() as GraphEvent.Done

    @Test fun `model with no tool calls answers directly`() {
        val direct = Node { _, _ ->
            NodeResult.Continue(StateUpdate(appendMessages = listOf(VisionMessage.text(Role.ASSISTANT, "answer"))))
        }
        val events = run(ReActAgentFactory.build(model = direct, tools = toolNode))
        assertEquals(listOf("start:model", "end:model", "done"), events.map { it.tag() })
        assertEquals("answer", events.done().state.messages.last().text())
    }

    @Test fun `react loop cycles model tools model then answers`() {
        val events = run(ReActAgentFactory.build(model = reactModel, tools = toolNode))
        assertEquals(
            listOf("start:model", "end:model", "start:tools", "end:tools", "start:model", "end:model", "done"),
            events.map { it.tag() },
        )
        assertEquals("final answer", events.done().state.messages.last().text())
    }

    @Test fun `a model that always calls tools is bounded by the step budget`() {
        val loopingModel = Node { _, _ ->
            NodeResult.Continue(
                StateUpdate(
                    appendMessages = listOf(VisionMessage(Role.ASSISTANT, listOf(ContentPart.ToolCall("c", "get_time", "{}")))),
                    spendStep = true,
                ),
            )
        }
        val events = run(ReActAgentFactory.build(model = loopingModel, tools = toolNode), GraphState(remainingSteps = 3))
        assertTrue(events.last() is GraphEvent.Done)
        assertTrue(events.count { it is GraphEvent.NodeStart } <= 7) // bounded, not infinite
    }

    @Test fun `reflect node runs once after the final answer`() {
        val reflect = Node { _, _ ->
            NodeResult.Continue(StateUpdate(appendMessages = listOf(VisionMessage.text(Role.ASSISTANT, "reflected"))))
        }
        val direct = Node { _, _ ->
            NodeResult.Continue(StateUpdate(appendMessages = listOf(VisionMessage.text(Role.ASSISTANT, "answer"))))
        }
        val events = run(ReActAgentFactory.build(model = direct, tools = toolNode, reflect = reflect))
        assertEquals(listOf("start:model", "end:model", "start:reflect", "end:reflect", "done"), events.map { it.tag() })
        assertEquals("reflected", events.done().state.messages.last().text())
    }

    private fun GraphEvent.tag(): String = when (this) {
        is GraphEvent.NodeStart -> "start:$node"
        is GraphEvent.NodeEnd -> "end:$node"
        is GraphEvent.Done -> "done"
        is GraphEvent.Interrupted -> "interrupt"
        is GraphEvent.Failed -> "failed:$message"
        else -> "other"
    }
}
