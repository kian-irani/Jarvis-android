package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class MemoryStoreRequest(val type: String, val content: String, val metadata: String = "{}")

@Serializable
data class MemoryListItem(val id: String, val type: String, val content: String, val metadata: String, val created_at: Long)

fun Route.memoryRoutes(memory: MemoryRepository) {
    post("/memory") {
        val req = call.receive<MemoryStoreRequest>()
        if (req.type !in setOf("episodic", "semantic")) throw BrainException.validation("type must be episodic|semantic")
        call.respond(success(mapOf("id" to memory.store(req.type, req.content, req.metadata))))
    }
    get("/memory") {
        val type = call.request.queryParameters["type"]
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        val items = memory.list(type, limit, offset)
            .map { MemoryListItem(it.id, it.type, it.content, it.metadata, it.created_at) }
        call.respond(success(items))
    }
}
