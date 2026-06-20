package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.Checkpointer
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.tools.ToolNode
import com.kianirani.jarvis.core.tools.ToolRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonArray

/**
 * VCF — the assembled agent and the single entry point a surface uses to talk to Vision's
 * agentic brain. It wires the ReAct graph (A1) with the model node (A2) and the
 * trust-gated tool node (T3) on the streaming runtime (G2) and durable checkpointer (G3).
 * The model is a [ModelClient] (a fake in tests, RouterModelClient in production), so the
 * whole stack is exercisable end-to-end without network.
 */
class VisionAgent(
    client: ModelClient,
    tools: ToolRegistry,
    checkpointer: Checkpointer? = null,
    toolSchema: JsonArray? = null,
    /**
     * Persona + tool-use guidance prepended as the SYSTEM turn of every run. Evaluated per
     * run so it reflects live settings (name/language) and context. Returns null/blank to run
     * with no system turn (the old behaviour). Without this the agent had no identity, no
     * language preference, and no instruction to actually call tools — so it answered like a
     * bare chat model.
     */
    private val systemPrompt: () -> String? = { null },
) {
    private val graph = ReActAgentFactory.build(
        model = ModelNode(client, toolSchema),
        tools = ToolNode(tools),
        checkpointer = checkpointer,
    )

    /** Stream a fresh turn for [userText] on [threadId] as graph lifecycle events. */
    fun run(userText: String, threadId: String = "main", ctx: NodeContext = NodeContext()): Flow<GraphEvent> {
        val messages = buildList {
            systemPrompt()?.takeIf { it.isNotBlank() }?.let { add(VisionMessage.text(Role.SYSTEM, it)) }
            add(VisionMessage.text(Role.USER, userText))
        }
        return graph.stream(GraphState(messages = messages), threadId, ctx)
    }

    /** Resume after a tool-confirmation interrupt, feeding the user's [answer]. */
    fun resume(threadId: String, answer: VisionMessage, ctx: NodeContext = NodeContext()): Flow<GraphEvent> =
        graph.resume(threadId, answer, ctx)
}
