package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.serialization.json.JsonArray

/** One assistant turn from a model: the reply (text and/or tool calls), the model that answered, or an error. */
data class ModelResponse(
    val message: VisionMessage,
    val modelId: String = "",
    val error: String? = null,
) {
    val isError: Boolean get() = error != null
}

/**
 * VCF-A2 — the port the agent nodes use to talk to a model, decoupling the graph from
 * the VB router/providers. A non-null [toolSchema] requests native function-calling; the
 * real adapter (RouterModelClient) bridges this to CloudChatRouter, while a fake makes
 * the agent nodes unit-testable without network.
 */
fun interface ModelClient {
    suspend fun complete(messages: List<VisionMessage>, toolSchema: JsonArray?): ModelResponse
}
