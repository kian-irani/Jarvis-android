package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.CompiledGraph
import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.tools.ToolContext
import com.kianirani.jarvis.core.tools.ToolSpec
import com.kianirani.jarvis.core.tools.VisionTool
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * VCF-X3 — exposes a sub-agent (a compiled [CompiledGraph]) as a callable [VisionTool]
 * (AutoGen "agent-as-tool", PRD §9). A manager agent invokes it like any other tool:
 * the `task` argument seeds a fresh sub-run on its own thread, and the sub-agent's final
 * answer is returned as the tool result. A failed sub-run becomes an error result
 * (failure-as-data); the originating call id is stamped by the ToolNode.
 */
class AgentAsTool(
    private val sub: CompiledGraph,
    override val spec: ToolSpec,
) : VisionTool {
    override suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult {
        val task = runCatching { args.getValue("task").jsonPrimitive.content }.getOrDefault("")
        val input = GraphState(messages = listOf(VisionMessage.text(Role.USER, task)), sessionId = ctx.sessionId)
        val events = sub.stream(input, threadId(ctx), NodeContext()).toList()
        val failed = events.lastOrNull() is GraphEvent.Failed
        return ContentPart.ToolResult(
            callId = "placeholder",
            name = spec.name,
            content = listOf(ContentPart.Text(finalText(events))),
            isError = failed,
        )
    }

    private fun threadId(ctx: ToolContext): String = "${ctx.sessionId.ifBlank { "sub" }}:${spec.name}"

    private fun finalText(events: List<GraphEvent>): String = when (val last = events.lastOrNull()) {
        is GraphEvent.Done -> last.state.messages.lastOrNull { it.role == Role.ASSISTANT }?.text().orEmpty()
        is GraphEvent.Failed -> last.message
        is GraphEvent.Interrupted -> "paused: ${last.reason}"
        else -> ""
    }
}
