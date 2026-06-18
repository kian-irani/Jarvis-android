package com.kianirani.jarvis.data.ai

import com.kianirani.jarvis.core.agent.ModelClient
import com.kianirani.jarvis.core.agent.ModelResponse
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.serialization.json.JsonArray
import javax.inject.Inject

/**
 * VCF-A2 — the production [ModelClient]: bridges the agent layer to the existing VB router.
 * It flattens the conversation into a prompt and calls [CloudChatRouter.chat], which
 * already does provider routing, substitution, token-pool rotation, and memory. Native
 * function-calling (VCF-T2) is not yet sent over the wire, so tool use flows through the
 * text TOOL_PROTOCOL fallback; [toolSchema] is accepted for forward-compatibility.
 * Additive — the existing chat path is untouched, so wiring this in can't regress chat.
 */
class RouterModelClient @Inject constructor(
    private val router: CloudChatRouter,
) : ModelClient {
    override suspend fun complete(messages: List<VisionMessage>, toolSchema: JsonArray?): ModelResponse {
        val prompt = messages.joinToString("\n") { "${it.role}: ${it.text()}" }
        return router.chat(prompt).fold(
            onSuccess = { ModelResponse(VisionMessage.text(Role.ASSISTANT, it.text), modelId = it.provider.name) },
            onFailure = { ModelResponse(VisionMessage.text(Role.ASSISTANT, ""), error = it.message) },
        )
    }
}
