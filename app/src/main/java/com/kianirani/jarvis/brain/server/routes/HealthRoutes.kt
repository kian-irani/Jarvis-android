package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.server.success
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

class HealthState(
    val version: String,
    val embedReady: () -> Boolean,
    val storageUsedBytes: () -> Long,
    val startedAtMs: Long = System.currentTimeMillis(),
    /** sha256 hex of this brain's pairing token — echoed so clients can verify the brain knows the secret. */
    val pairTokenSha: () -> String? = { null },
)

@Serializable
data class HealthData(
    val version: String,
    val uptimeMs: Long,
    val capabilities: Map<String, String>,
    val modelReady: Boolean,
    val storageUsedBytes: Long,
)

@Serializable
data class StatusData(val keys: List<String>, val requestCount: Long)

fun Route.healthRoutes(state: HealthState) {
    get("/health") {
        state.pairTokenSha()?.let { call.response.header("X-Pair-Ack", it) }
        call.respond(
            success(
                HealthData(
                    version = state.version,
                    uptimeMs = System.currentTimeMillis() - state.startedAtMs,
                    capabilities = mapOf("chat" to "cloud", "embed" to "local"),
                    modelReady = state.embedReady(),
                    storageUsedBytes = state.storageUsedBytes(),
                )
            )
        )
    }
}

fun Route.statusRoutes(keyStatus: () -> List<String>, requestCount: () -> Long) {
    get("/status") { call.respond(success(StatusData(keyStatus(), requestCount()))) }
}
