package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class TaskCreateRequest(val kind: String, val payload: JsonObject)

@Serializable
data class TaskStatusItem(val id: String, val kind: String, val status: String, val result: String?)

fun Route.taskRoutes(tasks: TaskRepository) {
    post("/task") {
        val req = call.receive<TaskCreateRequest>()
        call.respond(success(mapOf("id" to tasks.enqueue(req.kind, req.payload.toString()))))
    }
    get("/task/{id}") {
        val id = call.parameters["id"]!!
        val t = tasks.byId(id) ?: throw BrainException.notFound("task $id")
        call.respond(success(TaskStatusItem(t.id, t.kind, t.status, t.result)))
    }
}
