package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Node
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.StateUpdate
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.serialization.json.JsonArray

/**
 * VCF-A2 — the reasoning node in the ReAct graph. Calls the [ModelClient] with the
 * running conversation (and the optional [toolSchema] for native function-calling),
 * appends the assistant's reply (which may carry tool calls), and spends a step so the
 * loop stays bounded. Any failure degrades to a spoken error message instead of crashing
 * the run; the answering model id is recorded in scratch for telemetry/escalation.
 */
class ModelNode(
    private val client: ModelClient,
    private val toolSchema: JsonArray? = null,
) : Node {
    override suspend fun run(state: GraphState, ctx: NodeContext): NodeResult {
        val response = runCatching { client.complete(state.messages, toolSchema) }
            .getOrElse { return reply(errorMessage(it.message)) }
        if (response.isError) return reply(errorMessage(response.error))
        val scratch = if (response.modelId.isNotBlank()) mapOf(LAST_MODEL to response.modelId) else emptyMap()
        return NodeResult.Continue(StateUpdate(appendMessages = listOf(response.message), spendStep = true, scratch = scratch))
    }

    private fun reply(message: VisionMessage) =
        NodeResult.Continue(StateUpdate(appendMessages = listOf(message), spendStep = true))

    private fun errorMessage(detail: String?): VisionMessage =
        VisionMessage.text(Role.ASSISTANT, "I ran into a problem reaching the model: ${detail ?: "unknown error"}.")

    companion object {
        const val LAST_MODEL = "lastModel"
    }
}
