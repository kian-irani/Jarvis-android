package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.BrainEvent
import com.kianirani.jarvis.brain.data.ChatMessage
import com.kianirani.jarvis.brain.data.EventBus
import com.kianirani.jarvis.brain.server.brainJson
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * VCF-R3 — Streaming network plane (§10 / DS-C2).
 *
 * A single WebSocket at `/v1/stream` that lets a remote/desktop surface drive a chat
 * turn and receive it **incrementally** (token by token), plus a live relay of ambient
 * [BrainEvent]s. Brain-Lite's [ChatPort] is request/response (not token-native), so the
 * reply is re-tokenized here into ordered deltas that reassemble to the original — the
 * surface gets a streaming feel today and can switch to a true token stream later with
 * no protocol change. The framing/tokenizing logic lives in [StreamProtocol] (pure, so
 * it's unit-tested without Ktor); the route is a thin transport over it.
 */

/** Client → server. `type` is "chat" (drive a turn) or "ping" (liveness). */
@Serializable
data class StreamRequest(
    val type: String = "chat",
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String? = null, // convenience: a single user turn, used when [messages] is empty
    val model: String? = null,
)

/** Server → client. One flat shape; only the fields relevant to [type] are set. */
@Serializable
data class StreamFrame(
    val type: String, // "token" | "done" | "event" | "pong" | "error"
    val delta: String? = null, // token text (type=token)
    val text: String? = null, // full reply (type=done)
    val kind: String? = null, // event kind (type=event)
    val payload: String? = null, // event payload (type=event)
    val code: String? = null, // error code (type=error)
    val message: String? = null, // error detail (type=error)
)

/** Pure framing + tokenizing — no Ktor, no coroutines, fully JVM-testable. */
object StreamProtocol {

    /**
     * Splits [text] into ordered tokens that reassemble exactly: each token is a word
     * plus its trailing whitespace (leading whitespace, if any, is its own first token),
     * so `tokenize(t).joinToString("") == t`. Empty text → no tokens.
     */
    fun tokenize(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var i = 0
        val lead = i
        while (i < text.length && text[i].isWhitespace()) i++
        if (i > lead) out += text.substring(lead, i)
        while (i < text.length) {
            val start = i
            while (i < text.length && !text[i].isWhitespace()) i++ // the word
            while (i < text.length && text[i].isWhitespace()) i++ // its trailing space(s)
            out += text.substring(start, i)
        }
        return out
    }

    /** The turn to send to the model: explicit [StreamRequest.messages], else a single prompt. */
    fun resolveMessages(req: StreamRequest): List<ChatMessage> = when {
        req.messages.isNotEmpty() -> req.messages
        !req.prompt.isNullOrBlank() -> listOf(ChatMessage(role = "user", content = req.prompt))
        else -> emptyList()
    }

    fun token(delta: String) = StreamFrame(type = "token", delta = delta)
    fun done(full: String) = StreamFrame(type = "done", text = full)
    fun event(e: BrainEvent) = StreamFrame(type = "event", kind = e.kind, payload = e.payload)
    fun error(code: String, message: String) = StreamFrame(type = "error", code = code, message = message)
    val pong = StreamFrame(type = "pong")

    fun encode(frame: StreamFrame): String = brainJson.encodeToString(StreamFrame.serializer(), frame)

    /** Parse a client text frame; null on malformed JSON (caller answers with an error frame). */
    fun decode(text: String): StreamRequest? =
        runCatching { brainJson.decodeFromString(StreamRequest.serializer(), text) }.getOrNull()
}

fun Route.streamRoutes(chat: ChatPort, bus: EventBus) {
    webSocket("/v1/stream") {
        suspend fun emit(frame: StreamFrame) = send(Frame.Text(StreamProtocol.encode(frame)))

        // Relay ambient events to this surface for as long as the socket is open.
        val relay = launch { bus.events.collect { emit(StreamProtocol.event(it)) } }
        try {
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                val req = StreamProtocol.decode(frame.readText())
                if (req == null) {
                    emit(StreamProtocol.error("BAD_REQUEST", "malformed frame"))
                    continue
                }
                when (req.type) {
                    "ping" -> emit(StreamProtocol.pong)
                    "chat" -> {
                        val messages = StreamProtocol.resolveMessages(req)
                        if (messages.isEmpty()) {
                            emit(StreamProtocol.error("VALIDATION", "messages or prompt required"))
                        } else {
                            val attempt = runCatching { chat.chat(messages, req.model) }
                            val reply = attempt.getOrNull()
                            if (reply == null) {
                                emit(StreamProtocol.error("CHAT_FAILED", attempt.exceptionOrNull()?.message ?: "chat failed"))
                            } else {
                                for (tok in StreamProtocol.tokenize(reply.content)) emit(StreamProtocol.token(tok))
                                emit(StreamProtocol.done(reply.content))
                            }
                        }
                    }
                    else -> emit(StreamProtocol.error("UNKNOWN_TYPE", req.type))
                }
            }
        } finally {
            relay.cancel()
        }
    }
}
