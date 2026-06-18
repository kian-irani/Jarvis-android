package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.Checkpointer
import com.kianirani.jarvis.core.graph.CompiledGraph
import com.kianirani.jarvis.core.graph.END
import com.kianirani.jarvis.core.graph.Node
import com.kianirani.jarvis.core.graph.VisionGraph

/**
 * VCF-A1 — assembles the ReAct agent as a graph on the VCF-G2 runtime (PRD §6.1),
 * replacing [AgentEngine]'s fixed linear walk. The model node reasons and may emit tool
 * calls; if it does, control flows model→tools→model (a cycle the runtime bounds by
 * `remainingSteps`); otherwise the run ends — optionally after a one-shot reflect node.
 * The model / tools / reflect nodes are injected (ModelNode = VCF-A2, ToolNode = VCF-T3),
 * so the topology is unit-testable with fakes.
 */
object ReActAgentFactory {
    const val MODEL = "model"
    const val TOOLS = "tools"
    const val REFLECT = "reflect"

    fun build(
        model: Node,
        tools: Node,
        reflect: Node? = null,
        checkpointer: Checkpointer? = null,
    ): CompiledGraph {
        val builder = VisionGraph.Builder()
            .addNode(MODEL, model)
            .addNode(TOOLS, tools)
            .setEntry(MODEL)
            .addEdge(TOOLS, MODEL) // observe the tool result, then reason again
            .addConditionalEdge(MODEL) { state ->
                when {
                    state.messages.lastOrNull()?.toolCalls().orEmpty().isNotEmpty() -> TOOLS
                    reflect != null -> REFLECT
                    else -> END
                }
            }
        if (reflect != null) {
            builder.addNode(REFLECT, reflect).addEdge(REFLECT, END)
        }
        return builder.compile(checkpointer)
    }
}
