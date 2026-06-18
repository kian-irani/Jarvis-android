package com.kianirani.jarvis.core.tools

import com.kianirani.jarvis.core.agent.SafetyLayer
import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Node
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Observation
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.StateUpdate
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * VCF-T3 — the tool-executing node (PRD §7.3). It runs the tool calls in the last
 * message through the [ToolRegistry], gated by [SafetyLayer]: a tool that needs
 * confirmation and isn't pre-approved pauses the run with an Interrupt (HIL) so the
 * runtime can ask the user and resume. Unknown tools and thrown errors become error
 * observations (failure-as-data) — the node never crashes the graph. The node stamps
 * the originating call id onto each result so tools don't need to know it.
 */
class ToolNode(
    private val registry: ToolRegistry,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : Node {
    override suspend fun run(state: GraphState, ctx: NodeContext): NodeResult {
        val calls = state.messages.lastOrNull()?.toolCalls().orEmpty()
        if (calls.isEmpty()) return NodeResult.Continue()

        // Trust gate: the first risky, unapproved *registered* call pauses the whole run
        // (openclaw block). Unknown tools fall through to an error observation below so the
        // model self-corrects rather than prompting the user to confirm a nonexistent tool.
        val needsOk = calls.firstOrNull { call ->
            val spec = registry.byName(call.name)?.spec
            spec != null && SafetyLayer.requiresConfirmation(call.name, spec.trust) && call.id !in ctx.preApproved
        }
        if (needsOk != null) {
            return NodeResult.Interrupt(
                "confirm_tool:${needsOk.name}",
                buildJsonObject {
                    put("id", needsOk.id)
                    put("name", needsOk.name)
                    put("args", needsOk.argsJson)
                },
            )
        }

        val toolCtx = ToolContext(sessionId = state.sessionId, preApproved = ctx.preApproved.isNotEmpty())
        val results = calls.map { call ->
            val tool = registry.byName(call.name)
            if (tool == null) {
                errorResult(call.id, call.name, "unknown tool '${call.name}'")
            } else {
                runCatching { tool.execute(parseArgs(call.argsJson), toolCtx).copy(callId = call.id, name = call.name) }
                    .getOrElse { errorResult(call.id, call.name, it.message ?: "tool failed") }
            }
        }
        return NodeResult.Continue(
            StateUpdate(
                appendMessages = results.map { VisionMessage(Role.TOOL, listOf(it)) },
                appendObservations = results.map { Observation(it.name, resultText(it), it.isError) },
            ),
        )
    }

    private fun parseArgs(argsJson: String): JsonObject =
        runCatching { json.parseToJsonElement(argsJson).jsonObject }.getOrDefault(JsonObject(emptyMap()))

    private fun errorResult(callId: String, name: String, message: String): ContentPart.ToolResult =
        ContentPart.ToolResult(callId, name, listOf(ContentPart.Text(message)), isError = true)

    private fun resultText(r: ContentPart.ToolResult): String =
        r.content.filterIsInstance<ContentPart.Text>().joinToString(" ") { it.text }
}
