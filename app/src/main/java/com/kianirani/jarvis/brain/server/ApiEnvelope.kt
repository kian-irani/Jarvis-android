package com.kianirani.jarvis.brain.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.sse.SSE
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiError(val code: String, val message: String)

@Serializable
data class ApiResponse<T>(val ok: Boolean, val data: T? = null, val error: ApiError? = null)

fun <T> success(data: T) = ApiResponse(ok = true, data = data)
fun failure(code: String, message: String) =
    ApiResponse<JsonElement>(ok = false, error = ApiError(code, message))

class BrainException(val code: String, val status: HttpStatusCode, message: String) :
    Exception(message) {
    companion object {
        fun modelNotReady() = BrainException("MODEL_NOT_READY", HttpStatusCode.ServiceUnavailable, "Embedding model not downloaded yet")
        fun allKeysLimited() = BrainException("ALL_KEYS_RATE_LIMITED", HttpStatusCode.ServiceUnavailable, "All provider keys are rate limited")
        fun storageBudget() = BrainException("STORAGE_BUDGET_EXCEEDED", HttpStatusCode.InsufficientStorage, "2GB Brain-Lite storage budget exceeded")
        fun notFound(what: String) = BrainException("NOT_FOUND", HttpStatusCode.NotFound, "$what not found")
        fun validation(msg: String) = BrainException("VALIDATION", HttpStatusCode.BadRequest, msg)
    }
}

val brainJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun Application.installBrainPlugins() {
    install(ContentNegotiation) { json(brainJson) }
    install(SSE)
    install(WebSockets)
    install(StatusPages) {
        exception<BrainException> { call, e ->
            call.respond(e.status, failure(e.code, e.message ?: e.code))
        }
        exception<Throwable> { call, e ->
            call.respond(HttpStatusCode.InternalServerError, failure("INTERNAL", e.message ?: "internal error"))
        }
    }
}
