package com.kianirani.jarvis.data.repository

import com.kianirani.jarvis.brain.discovery.BrainSelectionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class ChatRequest(val message: String, val node_id: String? = null)
@Serializable data class ChatResponse(val response: String = "", val model: String = "", val node_used: String? = null, val duration_ms: Long = 0)
@Serializable data class NodeMetrics(val cpu_percent: Float = 0f, val ram_used_gb: Float = 0f, val ram_free_gb: Float = 0f, val ram_total_gb: Float = 0f, val disk_used_gb: Float = 0f, val disk_free_gb: Float = 0f, val net_sent_mb: Float = 0f, val net_recv_mb: Float = 0f)
@Serializable data class ApiNode(val node_id: String, val name: String, val status: String = "offline", val metrics: NodeMetrics? = null)
@Serializable data class HealthResponse(val status: String = "ok", val version: String = "", val nodes: Int = 0)

sealed class BrainEvent {
    data class NodeUpdated(val node: ApiNode) : BrainEvent()
    data class ChatReply(val text: String) : BrainEvent()
    data class Connected(val msg: String) : BrainEvent()
    data object Disconnected : BrainEvent()
    data class Error(val cause: Throwable) : BrainEvent()
    data class LogEntry(val message: String, val level: String = "info") : BrainEvent()
}

/**
 * HTTP client for the elected brain. The target comes from the pairing flow
 * ([BrainSelectionStore]) — never hardcoded. Brain-Lite is HTTP-only, so the
 * legacy WebSocket channel is gone. With no pairing saved, Vision keeps
 * working standalone and this repository just reports Disconnected.
 */
@Singleton
class BrainRepository @Inject constructor(
    private val store: BrainSelectionStore,
) {
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(jsonParser) }
        install(Logging) { level = LogLevel.INFO }
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 8_000 }
    }

    private val _events = MutableSharedFlow<BrainEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BrainEvent> = _events.asSharedFlow()
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private fun baseUrl(): String? = store.load()?.let { "http://${it.host}:${it.port}" }

    private fun noBrain() = IllegalStateException("No brain paired")

    suspend fun health(): Result<HealthResponse> = runCatching {
        val base = baseUrl() ?: throw noBrain()
        jsonParser.decodeFromString(HealthResponse.serializer(), http.get("$base/health").bodyAsText())
    }

    suspend fun getNodes(): Result<List<ApiNode>> = runCatching {
        val base = baseUrl() ?: throw noBrain()
        val obj = jsonParser.decodeFromString(JsonObject.serializer(), http.get("$base/nodes").bodyAsText())
        obj["nodes"]?.jsonArray?.map { jsonParser.decodeFromJsonElement(ApiNode.serializer(), it) } ?: emptyList()
    }

    suspend fun chat(msg: String, nodeId: String? = null): Result<ChatResponse> = runCatching {
        val base = baseUrl() ?: throw noBrain()
        val text = http.post("$base/chat") {
            contentType(ContentType.Application.Json)
            setBody(jsonParser.encodeToString(ChatRequest.serializer(), ChatRequest(msg, nodeId)))
        }.bodyAsText()
        jsonParser.decodeFromString(ChatResponse.serializer(), text)
    }

    /** Health-poll loop that maintains [connected] and emits connection events. */
    fun connect(scope: CoroutineScope, intervalMs: Long = 10_000L) {
        scope.launch(Dispatchers.IO) {
            var wasConnected = false
            var announcedStandalone = false
            while (isActive) {
                if (baseUrl() == null) {
                    if (wasConnected) { wasConnected = false; _connected.value = false; _events.emit(BrainEvent.Disconnected) }
                    if (!announcedStandalone) {
                        announcedStandalone = true
                        _events.emit(BrainEvent.LogEntry("Standalone — no brain paired", "info"))
                    }
                } else {
                    announcedStandalone = false
                    val ok = health().isSuccess
                    if (ok && !wasConnected) { _connected.value = true; _events.emit(BrainEvent.Connected("Brain connected")) }
                    if (!ok && wasConnected) { _connected.value = false; _events.emit(BrainEvent.Disconnected) }
                    wasConnected = ok
                }
                delay(intervalMs)
            }
        }
    }

    fun startPolling(scope: CoroutineScope, ms: Long = 5_000L) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(ms)
                if (baseUrl() == null) continue
                getNodes()
                    .onSuccess { nodes -> nodes.forEach { _events.emit(BrainEvent.NodeUpdated(it)) } }
                    .onFailure { _events.emit(BrainEvent.Error(it)) }
            }
        }
    }

    fun close() { http.close() }
}
