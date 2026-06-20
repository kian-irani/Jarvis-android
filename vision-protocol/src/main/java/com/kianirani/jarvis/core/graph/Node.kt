package com.kianirani.jarvis.core.graph

import kotlinx.serialization.json.JsonObject

/** Terminal pseudo-node: reaching it ends the run. */
const val END = "__end__"

/**
 * VCF-G2 — side-channel handed to every node. Carries human approvals (thread ids the
 * user has OK'd, so a re-entered trust gate proceeds); model/tool handles are added in
 * the agent layer (VCF-A).
 */
data class NodeContext(
    val preApproved: Set<String> = emptySet(),
)

/**
 * A unit of work in the graph. A node reads the (immutable) [GraphState] and returns a
 * [NodeResult]; it never mutates state — the runner applies the update via the reducer.
 */
fun interface Node {
    suspend fun run(state: GraphState, ctx: NodeContext): NodeResult
}

sealed interface NodeResult {
    /** Advance: apply [update], then jump to [goto] or follow the graph's edges. */
    data class Continue(val update: StateUpdate = StateUpdate(), val goto: String? = null) : NodeResult

    /** Pause for human input (HIL): the run persists at this node and returns until resumed. */
    data class Interrupt(val reason: String, val payload: JsonObject = JsonObject(emptyMap())) : NodeResult
}
