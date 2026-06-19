package com.kianirani.jarvis.data.ai

import com.kianirani.jarvis.core.agent.ModelClient
import com.kianirani.jarvis.core.agent.ModelResponse
import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.serialization.json.JsonArray
import javax.inject.Inject

/**
 * VCF-A2 + VCF-T2 — the production [ModelClient]: bridges the agent layer to the VB router.
 *
 * When the agent supplies a [toolSchema] (the model can do native function-calling), the call
 * goes through [CloudChatRouter.complete], which sends the provider's native `tools` and returns
 * a structured [VisionMessage] that may carry real [ContentPart.ToolCall]s for the
 * [com.kianirani.jarvis.core.tools.ToolNode] to run. With no schema (or if the structured turn
 * yields nothing) it uses the existing plain-text [CloudChatRouter.chat] path — the
 * `TOOL_PROTOCOL` text fallback — so a model without function-calling still works (CrewAI's
 * native-FC-with-text-fallback pattern).
 *
 * Both paths reuse the router's provider routing, substitution, and token-pool rotation; the
 * live HUD chat path doesn't touch this class, so wiring native FC in here can't regress chat.
 */
class RouterModelClient @Inject constructor(
    private val router: CloudChatRouter,
) : ModelClient {
    override suspend fun complete(messages: List<VisionMessage>, toolSchema: JsonArray?): ModelResponse {
        if (toolSchema != null && toolSchema.isNotEmpty()) {
            router.complete(messages, toolSchema)
                .onSuccess { reply ->
                    if (reply.parts.isNotEmpty()) {
                        return ModelResponse(VisionMessage(Role.ASSISTANT, reply.parts), modelId = reply.provider.name)
                    }
                    // Empty structured reply → fall through to the text path below.
                }
                .onFailure { return ModelResponse(VisionMessage.text(Role.ASSISTANT, ""), error = it.message) }
        }
        // Text path (no tools, or an empty structured reply): flatten and use plain chat.
        val prompt = messages.joinToString("\n") { "${it.role}: ${it.text()}" }
        return router.chat(prompt).fold(
            onSuccess = { ModelResponse(VisionMessage.text(Role.ASSISTANT, it.text), modelId = it.provider.name) },
            onFailure = { ModelResponse(VisionMessage.text(Role.ASSISTANT, ""), error = it.message) },
        )
    }
}
