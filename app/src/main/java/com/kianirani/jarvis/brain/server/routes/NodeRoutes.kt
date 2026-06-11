package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.NodeRepository
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class NodeRegisterRequest(
    val name: String,
    val address: String,
    val capabilities: String = "{}",
    val brain_score: Int = 0,
    /** Stable node id — send the same id on every heartbeat so last_seen refreshes instead of duplicating the node. */
    val id: String? = null,
)

@Serializable
data class NodeItem(val id: String, val name: String, val address: String, val brain_score: Int, val last_seen: Long)

fun Route.nodeRoutes(nodes: NodeRepository) {
    post("/nodes") {
        val req = call.receive<NodeRegisterRequest>()
        call.respond(success(mapOf("id" to nodes.register(req.name, req.address, req.capabilities, req.brain_score, req.id))))
    }
    get("/nodes") {
        call.respond(success(nodes.list().map { NodeItem(it.id, it.name, it.address, it.brain_score, it.last_seen) }))
    }
}
