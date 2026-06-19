package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.BrainEvent
import com.kianirani.jarvis.brain.data.ChatMessage
import com.kianirani.jarvis.brain.data.ChatReply
import com.kianirani.jarvis.brain.data.EventBus
import com.kianirani.jarvis.brain.server.installBrainPlugins
import com.kianirani.jarvis.brain.server.routes.ChatPort
import com.kianirani.jarvis.brain.server.routes.StreamFrame
import com.kianirani.jarvis.brain.server.routes.StreamProtocol
import com.kianirani.jarvis.brain.server.routes.StreamRequest
import com.kianirani.jarvis.brain.server.routes.streamRoutes
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** VCF-R3 — pure framing ([StreamProtocol]) + the `/v1/stream` WebSocket transport. */
class StreamRoutesTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun decode(text: String): StreamFrame = json.decodeFromString(StreamFrame.serializer(), text)

    // --- pure StreamProtocol ---

    @Test
    fun `tokenize is lossless and word-grained`() {
        assertEquals(listOf("salam ", "dustam"), StreamProtocol.tokenize("salam dustam"))
        assertEquals("salam dustam", StreamProtocol.tokenize("salam dustam").joinToString(""))
        assertTrue(StreamProtocol.tokenize("").isEmpty())
        // multiple/trailing spaces and Persian are preserved exactly on reassembly
        val t = "سلام  دنیا "
        assertEquals(t, StreamProtocol.tokenize(t).joinToString(""))
        // leading whitespace becomes its own first token (still lossless)
        assertEquals("  hi", StreamProtocol.tokenize("  hi").joinToString(""))
    }

    @Test
    fun `resolveMessages prefers explicit messages, then a single prompt`() {
        val msgs = listOf(ChatMessage("user", "a"), ChatMessage("assistant", "b"))
        assertEquals(msgs, StreamProtocol.resolveMessages(StreamRequest(messages = msgs)))
        assertEquals(
            listOf(ChatMessage("user", "hi")),
            StreamProtocol.resolveMessages(StreamRequest(prompt = "hi")),
        )
        assertTrue(StreamProtocol.resolveMessages(StreamRequest()).isEmpty())
        assertTrue(StreamProtocol.resolveMessages(StreamRequest(prompt = "   ")).isEmpty())
    }

    // --- /v1/stream transport ---

    @Test
    fun `chat streams tokens then a final done frame`() = testApplication {
        val chat = mockk<ChatPort>()
        coEvery { chat.chat(any(), any()) } returns ChatReply(content = "salam dustam")
        application { installBrainPlugins(); routing { streamRoutes(chat, EventBus()) } }
        val ws = createClient { install(WebSockets) }
        ws.webSocket("/v1/stream") {
            send(Frame.Text("""{"type":"chat","prompt":"hi"}"""))
            val acc = StringBuilder()
            while (true) {
                val f = decode((incoming.receive() as Frame.Text).readText())
                when (f.type) {
                    "token" -> acc.append(f.delta)
                    "done" -> { assertEquals("salam dustam", f.text); break }
                    else -> fail("unexpected frame ${f.type}")
                }
            }
            assertEquals("salam dustam", acc.toString())
        }
    }

    @Test
    fun `ping yields pong`() = testApplication {
        application { installBrainPlugins(); routing { streamRoutes(mockk<ChatPort>(), EventBus()) } }
        val ws = createClient { install(WebSockets) }
        ws.webSocket("/v1/stream") {
            send(Frame.Text("""{"type":"ping"}"""))
            assertEquals("pong", decode((incoming.receive() as Frame.Text).readText()).type)
        }
    }

    @Test
    fun `malformed frame yields a BAD_REQUEST error`() = testApplication {
        application { installBrainPlugins(); routing { streamRoutes(mockk<ChatPort>(), EventBus()) } }
        val ws = createClient { install(WebSockets) }
        ws.webSocket("/v1/stream") {
            send(Frame.Text("not json at all"))
            val f = decode((incoming.receive() as Frame.Text).readText())
            assertEquals("error", f.type)
            assertEquals("BAD_REQUEST", f.code)
        }
    }

    @Test
    fun `chat without messages or prompt is a validation error`() = testApplication {
        application { installBrainPlugins(); routing { streamRoutes(mockk<ChatPort>(), EventBus()) } }
        val ws = createClient { install(WebSockets) }
        ws.webSocket("/v1/stream") {
            send(Frame.Text("""{"type":"chat"}"""))
            val f = decode((incoming.receive() as Frame.Text).readText())
            assertEquals("error", f.type)
            assertEquals("VALIDATION", f.code)
        }
    }

    @Test
    fun `ambient events are relayed to the socket`() = testApplication {
        val bus = EventBus()
        application { installBrainPlugins(); routing { streamRoutes(mockk<ChatPort>(), bus) } }
        val ws = createClient { install(WebSockets) }
        ws.webSocket("/v1/stream") {
            bus.publish(BrainEvent(kind = "wake", payload = "hey"))
            val f = decode((incoming.receive() as Frame.Text).readText())
            assertEquals("event", f.type)
            assertEquals("wake", f.kind)
            assertEquals("hey", f.payload)
        }
    }
}
