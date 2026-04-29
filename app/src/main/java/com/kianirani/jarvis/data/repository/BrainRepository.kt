package com.kianirani.jarvis.data.repository

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.parseToJsonElement
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

@Singleton
class BrainRepository @Inject constructor() {
    companion object {
        private const val TAG = "BrainRepo"
        private const val BASE_URL = "http://212.87.199.62:8000"
        private const val WS_URL = "ws://212.87.199.62:8000/ws/mobile"
    }

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(jsonParser) }
        install(WebSockets)
        install(Logging) { level = LogLevel.INFO }
        install(HttpTimeout) { requestTimeoutMillis = 15_000; connectTimeoutMillis = 8_000 }
    }

    private val _events = MutableSharedFlow<BrainEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BrainEvent> = _events.asSharedFlow()
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    suspend fun health(): Result<HealthResponse> = runCatching {
        jsonParser.decodeFromString(HealthResponse.serializer(), http.get("$BASE_URL/health").bodyAsText())
    }

    suspend fun getNodes(): Result<List<ApiNode>> = runCatching {
        val obj = jsonParser.parseToJsonElement(http.get("$BASE_URL/nodes").bodyAsText()).jsonObject
        obj["nodes"]?.jsonArray?.map { jsonParser.decodeFromJsonElement(ApiNode.serializer(), it) } ?: emptyList()
    }

    suspend fun chat(msg: String, nodeId: String? = null): Result<ChatResponse> = runCatching {
        val text = http.post("$BASE_URL/chat") {
            contentType(ContentType.Application.Json)
            setBody(jsonParser.encodeToString(ChatRequest.serializer(), ChatRequest(msg, nodeId)))
        }.bodyAsText()
        jsonParser.decodeFromString(ChatResponse.serializer(), text)
    }

    fun connectWebSocket(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            var delayMs = 3_000L
            while (isActive) {
                try {
                    http.webSocket(WS_URL) {
                        _connected.value = true; delayMs = 3_000L
                        _events.emit(BrainEvent.Connected("Brain connected"))
                        send(Frame.Text(jsonParser.encodeToString(mapOf("type" to "mobile_register", "client" to "JARVIS-Android"))))
                        for (frame in incoming) { if (frame is Frame.Text) handleWs(frame.readText()) }
                    }
                } catch (e: CancellationException) { throw e
                } catch (e: Exception) {
                    _connected.value = false
                    _events.emit(BrainEvent.Disconnected)
                    _events.emit(BrainEvent.LogEntry("Reconnecting...", "warn"))
                }
                delay(delayMs); delayMs = (delayMs * 1.5).toLong().coerceAtMost(30_000)
            }
        }
    }

    private suspend fun handleWs(raw: String) {
        try {
            val obj = jsonParser.parseToJsonElement(raw).jsonObject
            when (obj["type"]?.jsonPrimitive?.content) {
                "heartbeat" -> { val nid = obj["node_id"]?.jsonPrimitive?.content ?: return; val m = obj["metrics"]?.let { jsonParser.decodeFromJsonElement(NodeMetrics.serializer(), it) }; _events.emit(BrainEvent.NodeUpdated(ApiNode(nid, nid, metrics = m))) }
                "chat_stream" -> _events.emit(BrainEvent.ChatReply(obj["delta"]?.jsonPrimitive?.content ?: ""))
                "log" -> _events.emit(BrainEvent.LogEntry(obj["message"]?.jsonPrimitive?.content ?: "", obj["level"]?.jsonPrimitive?.content ?: "info"))
                "connected" -> obj["nodes"]?.jsonArray?.forEach { n -> _events.emit(BrainEvent.NodeUpdated(jsonParser.decodeFromJsonElement(ApiNode.serializer(), n))) }
            }
        } catch (e: Exception) { Log.e(TAG, "WS: $e") }
    }

    fun startPolling(scope: CoroutineScope, ms: Long = 5_000L) {
        scope.launch(Dispatchers.IO) {
            while (isActive) { delay(ms); getNodes().onSuccess { nodes -> nodes.forEach { _events.emit(BrainEvent.NodeUpdated(it)) } }.onFailure { _events.emit(BrainEvent.Error(it)) } }
        }
    }

    fun close() { http.close() }
}
