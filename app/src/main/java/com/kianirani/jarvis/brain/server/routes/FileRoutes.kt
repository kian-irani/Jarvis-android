package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.FileRepository
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class FileOpRequest(val op: String, val path: String, val content: String? = null)

fun Route.fileRoutes(files: FileRepository) {
    post("/files") {
        val req = call.receive<FileOpRequest>()
        when (req.op) {
            "read" -> call.respond(success(mapOf("content" to files.read(req.path))))
            "write" -> {
                files.write(req.path, req.content ?: throw BrainException.validation("content required for write"))
                call.respond(success(mapOf("written" to req.path)))
            }
            else -> throw BrainException.validation("op must be read|write")
        }
    }
    get("/files") {
        call.respond(success(files.list(call.request.queryParameters["path"] ?: "")))
    }
}
