package com.kianirani.jarvis.core.a2a

import kotlinx.serialization.Serializable

/**
 * A2A — agent-to-agent protocol (PRD §, paired with MCP). A small envelope that lets one Vision
 * agent (or a remote peer's agent over the mesh) hand a task to another and get a result back,
 * with a correlation id and status. Pure & serializable so it travels over the same network
 * plane as the rest of the wire contract (DS-F2). Routing the envelope to the target agent
 * (in-process via the gateway, or over the mesh) is the runtime half.
 */
@Serializable
enum class A2AType { REQUEST, RESPONSE, ERROR }

@Serializable
data class A2AMessage(
    val type: A2AType,
    /** Correlation id — a response carries the request's id. */
    val id: String,
    val fromAgent: String,
    val toAgent: String,
    /** The task (REQUEST) or the result (RESPONSE) or the message (ERROR). */
    val content: String,
    val metadata: Map<String, String> = emptyMap(),
) {
    companion object {
        fun request(id: String, from: String, to: String, task: String, metadata: Map<String, String> = emptyMap()) =
            A2AMessage(A2AType.REQUEST, id, from, to, task, metadata)

        /** Build the matching response to [request] (swaps from/to, keeps id). */
        fun respond(request: A2AMessage, result: String) =
            A2AMessage(A2AType.RESPONSE, request.id, request.toAgent, request.fromAgent, result)

        fun error(request: A2AMessage, message: String) =
            A2AMessage(A2AType.ERROR, request.id, request.toAgent, request.fromAgent, message)
    }
}
