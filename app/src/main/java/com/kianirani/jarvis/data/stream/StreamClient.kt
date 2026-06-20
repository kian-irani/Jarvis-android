package com.kianirani.jarvis.data.stream

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * DS-C2 — the network-plane **client** for Brain-Lite's `/v1/stream` WebSocket (the server side
 * shipped as VCF-R3). A remote/desktop surface uses this to drive one chat turn on the brain and
 * receive it token-by-token. Frames are the server's `{type,delta,text,code,message}` shape; this
 * accumulates `token` deltas and emits the running text, completing on `done` or `error`.
 *
 * Built on OkHttp's WebSocket (already on the classpath via ktor-client-okhttp — no new
 * dependency). The pure framing/tokenizing lives server-side in `StreamProtocol`.
 */
class StreamClient(private val client: OkHttpClient = OkHttpClient()) {

    private val json = Json { ignoreUnknownKeys = true }

    /** A streamed chunk: the cumulative [text] so far and whether the turn is [finished]. */
    data class Chunk(val text: String, val finished: Boolean, val error: String? = null)

    /**
     * Open `/v1/stream` at [baseUrl] (e.g. `ws://10.0.0.5:7799`), send [prompt], and emit
     * [Chunk]s as tokens arrive. The flow completes when the server sends `done`/`error` or the
     * socket closes.
     */
    fun stream(baseUrl: String, prompt: String): Flow<Chunk> = callbackFlow {
        val request = Request.Builder().url("$baseUrl/v1/stream").build()
        val acc = StringBuilder()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(buildJsonObject { put("prompt", prompt) }.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val frame = runCatching { json.decodeFromString(JsonObject.serializer(), text) }.getOrNull() ?: return
                when (frame["type"]?.jsonPrimitive?.content) {
                    "token" -> {
                        acc.append(frame["delta"]?.jsonPrimitive?.content ?: "")
                        trySend(Chunk(acc.toString(), finished = false))
                    }
                    "done" -> {
                        val full = frame["text"]?.jsonPrimitive?.content ?: acc.toString()
                        trySend(Chunk(full, finished = true))
                        close()
                    }
                    "error" -> {
                        trySend(Chunk(acc.toString(), finished = true, error = frame["message"]?.jsonPrimitive?.content ?: "stream error"))
                        close()
                    }
                    else -> Unit // pong / event — ignored for a single-turn drive
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySend(Chunk(acc.toString(), finished = true, error = t.message ?: "connection failed"))
                close()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }
        }
        val ws = client.newWebSocket(request, listener)
        awaitClose { ws.cancel() }
    }
}
