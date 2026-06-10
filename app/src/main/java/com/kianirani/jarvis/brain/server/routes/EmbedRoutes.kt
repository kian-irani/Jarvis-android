package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

interface EmbedPort { fun isReady(): Boolean; fun embed(texts: List<String>): List<FloatArray> }

@Serializable data class EmbedRequest(val texts: List<String>)
@Serializable data class SearchRequest(val query: String, val top_k: Int = 5)

fun Route.embedRoutes(embedPort: EmbedPort, memory: MemoryRepository) {
    post("/embed") {
        val req = call.receive<EmbedRequest>()
        if (!embedPort.isReady()) throw BrainException.modelNotReady()
        if (req.texts.isEmpty()) throw BrainException.validation("texts must not be empty")
        call.respond(success(embedPort.embed(req.texts).map { it.toList() }))
    }
    post("/search") {
        val req = call.receive<SearchRequest>()
        if (!embedPort.isReady()) throw BrainException.modelNotReady()
        call.respond(success(memory.search(req.query, req.top_k)))
    }
}
