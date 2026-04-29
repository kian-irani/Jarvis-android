package com.kianirani.jarvis.data.repository

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class ChatRequest(val message: String, val node_id: String? = null)
@Serializable data class ChatResponse(val response: String, val model: String = "", val node_used: String? = null, val duration_ms: Long = 0)
@Serializable data class NodeMetrics(val cpu_percent: Float = 0f, val ram_used_gb: Float = 0f, val ram_free_gb: Float = 0f, val ram_total_gb: Float = 0f, val disk_used_gb: Float = 0f, val disk_free_gb: Float = 0f)
@Serializable data class ApiNode(val node_id: String, val name: String, val status: String = "offline", val metrics: NodeMetrics? = null, val capabilities: List<String> = emptyList())
@Serializable data class NodesResponse(val nodes: List<ApiNode> = emptyList(), val count: Int = 0)
@Serializable data class HealthResponse(val status: String = "ok", val version: String = "", val nodes: Int = 0)

sealed class BrainEvent {
    data class NodeUpdated(val node: ApiNode) : BrainEvent()
    data class ChatReply(val text: String)    : BrainEvent()
    data class Connected(val msg: String)     : BrainEvent()
    data object Disconnected                  : BrainEvent()
    data class Error(val cause: Throwable)    : BrainEvent()
    data class LogEntry(val message: String, val level: String = "info") : BrainEvent()
}

@Singleton
class BrainRepository @Inject constructor() {
    companion object {
        private const val TAG      = "BrainRepo"
        private const val BASE_URL = "http://212.87.199.62:8000"
        private const val WS_URL   = "ws://212.87.199.62:8000/ws/mobile"
    }
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(WebSockets)         { pingInterval = 20_000L }
        install(Logging)            { level = LogLevel.INFO }
        install(HttpTimeout)        { requestTimeoutMillis = 15_000; connectTimeoutMillis = 8_000 }
    }
    private val _events    = MutableSharedFlow<BrainEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BrainEvent> = _events.asSharedFlow()
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    suspend fun health()   = runCatching { http.get("$BASE_URL/health").body<HealthResponse>() }
    suspend fun getNodes() = runCatching { http.get("$BASE_URL/nodes").body<NodesResponse>() }
    suspend fun chat(msg: String, nodeId: String? = null) = runCatching {
        http.post("$BASE_URL/chat") {
            contentType(ContentType.Application.Json)
            setBody(ChatRequest(msg, nodeId))
        }.body<ChatResponse>()
    }
    suspend fun runCommand(nodeId: String, cmd: String) = runCatching {
        http.post("$BASE_URL/run") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("node_id" to nodeId, "command" to cmd))
        }.bodyAsText()
    }

    fun connectWebSocket(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            var delay = 3_000L
            while (isActive) {
                try {
                    http.webSocket(WS_URL) {
                        _connected.value = true
                        delay = 3_000L
                        _events.emit(BrainEvent.Connected("Brain connected"))
                        send(Frame.Text(json.encodeToString(mapOf("type" to "mobile_register", "client" to "JARVIS-Android"))))
                        for (frame in incoming) {
                            if (frame is Frame.Text) handleWs(frame.readText())
                        }
                    }
                } catch (e: CancellationException) { throw e
                } catch (e: Exception) {
                    _connected.value = false
                    _events.emit(BrainEvent.Disconnected)
                    _events.emit(BrainEvent.LogEntry("Reconnecting in ${delay/1000}s...", "warn"))
                }
                kotlinx.coroutines.delay(delay)
                delay = (delay * 1.5f).toLong().coerceAtMost(30_000)
            }
        }
    }

    private suspend fun handleWs(raw: String) {
        try {
            val obj = json.parseToJsonElement(raw).jsonObject
            when (obj["type"]?.jsonPrimitive?.content) {
                "heartbeat"  -> {
                    val nid = obj["node_id"]?.jsonPrimitive?.content ?: return
                    val m   = obj["metrics"]?.let { json.decodeFromJsonElement<NodeMetrics>(it) }
                    _events.emit(BrainEvent.NodeUpdated(ApiNode(nid, nid, metrics = m)))
                }
                "chat_stream" -> _events.emit(BrainEvent.ChatReply(obj["delta"]?.jsonPrimitive?.content ?: ""))
                "log"         -> _events.emit(BrainEvent.LogEntry(
                    obj["message"]?.jsonPrimitive?.content ?: "",
                    obj["level"]?.jsonPrimitive?.content   ?: "info"))
                "connected"   -> obj["nodes"]?.jsonArray?.forEach { n ->
                    _events.emit(BrainEvent.NodeUpdated(json.decodeFromJsonElement<ApiNode>(n)))
                }
            }
        } catch (e: Exception) { Log.e(TAG, "WS parse: $e") }
    }

    fun startPolling(scope: CoroutineScope, ms: Long = 5_000L) {
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                kotlinx.coroutines.delay(ms)
                getNodes().onSuccess { r -> r.nodes.forEach { _events.emit(BrainEvent.NodeUpdated(it)) } }
                          .onFailure { _events.emit(BrainEvent.Error(it)) }
            }
        }
    }
    fun close() { http.close() }
}
