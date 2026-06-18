package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Node
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.StateUpdate
import com.kianirani.jarvis.core.graph.VisionMessage

/**
 * VCF-A4 — one-shot self-correction (CrewAI/AutoGen reflection). When [GraphState.scratch]
 * flags [FLAG], it asks the model to critique and improve the last answer, appends the
 * improved reply, and clears the flag so it never loops. A no-op when the flag is absent
 * or the model call fails (the flag is still cleared, so the run can finish).
 */
class ReflectNode(private val client: ModelClient) : Node {
    override suspend fun run(state: GraphState, ctx: NodeContext): NodeResult {
        if (state.scratch[FLAG] != "true") return NodeResult.Continue()
        val improved = runCatching { client.complete(state.messages + critique(), null).message }.getOrNull()
            ?: return NodeResult.Continue(StateUpdate(scratch = mapOf(FLAG to "false")))
        return NodeResult.Continue(StateUpdate(appendMessages = listOf(improved), scratch = mapOf(FLAG to "false")))
    }

    private fun critique(): VisionMessage = VisionMessage.text(
        Role.USER,
        "Critique your previous answer for correctness, completeness, and honesty, then give an " +
            "improved final answer. If it was already correct, restate it concisely.",
    )

    companion object {
        const val FLAG = "needsReflection"
    }
}
