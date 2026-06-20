package com.kianirani.jarvis.core.graph

/**
 * VCF-G2 — a directed graph of [Node]s joined by static and conditional edges,
 * compiled into a runnable [CompiledGraph]. Cycles are allowed (the runner bounds
 * them by [GraphState.remainingSteps]). Built via [Builder]. (LangGraph StateGraph.)
 */
class VisionGraph private constructor(
    private val nodes: Map<String, Node>,
    private val edges: Map<String, String>,
    private val conditional: Map<String, (GraphState) -> String>,
    val entry: String,
) {
    fun node(name: String): Node = nodes[name] ?: error("VisionGraph: no node named '$name'")

    /** The node after [from]: a conditional route wins, else a static edge, else [END]. */
    fun next(from: String, state: GraphState): String =
        conditional[from]?.invoke(state) ?: edges[from] ?: END

    class Builder {
        private val nodes = mutableMapOf<String, Node>()
        private val edges = mutableMapOf<String, String>()
        private val conditional = mutableMapOf<String, (GraphState) -> String>()
        private var entry = ""

        fun addNode(name: String, node: Node) = apply { nodes[name] = node }
        fun addEdge(from: String, to: String) = apply { edges[from] = to }
        fun addConditionalEdge(from: String, route: (GraphState) -> String) = apply { conditional[from] = route }
        fun setEntry(name: String) = apply { entry = name }

        fun compile(checkpointer: Checkpointer? = null): CompiledGraph {
            require(entry.isNotEmpty()) { "VisionGraph: setEntry(...) is required before compile()" }
            require(nodes.containsKey(entry)) { "VisionGraph: entry node '$entry' is not registered" }
            return CompiledGraph(
                VisionGraph(nodes.toMap(), edges.toMap(), conditional.toMap(), entry),
                checkpointer,
            )
        }
    }
}
