package com.kianirani.jarvis.core.protocol

import com.kianirani.jarvis.core.event.EventKind
import com.kianirani.jarvis.core.event.VisionEvent
import com.kianirani.jarvis.core.gateway.Channel
import com.kianirani.jarvis.core.memory.MemoryType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * DS-F2 acceptance: the protocol DTOs are a stable, lossless JSON contract (KMP-ready) and
 * the runtime mappers ([VisionRequest.toGatewayRequest], [VisionEvent.toDto]/[toEvent]) are
 * pure and round-trip exactly. Pure-logic, no device.
 */
class VisionProtocolTest {

    private val json = Json { encodeDefaults = true }

    private inline fun <reified T> roundTrip(value: T): T =
        json.decodeFromString<T>(json.encodeToString(value))

    // --- VisionRequest ---

    @Test fun `request round-trips with full wire context`() {
        val req = VisionRequest(
            text = "open spotify",
            sessionId = "s1",
            channel = Channel.WIDGET,
            locale = "fa-IR",
            intent = Intent("open_app", 0.9f, mapOf("app" to "spotify")),
            deviceContext = DeviceContext(foregroundApp = "com.android.chrome", batteryPercent = 42, charging = true),
            attachments = listOf(Attachment(AttachmentKind.IMAGE, "image/png", base64 = "AAAA")),
        )
        assertEquals(req, roundTrip(req))
    }

    @Test fun `request decodes from minimal json with defaults`() {
        val decoded = Json.decodeFromString<VisionRequest>("""{"text":"hi"}""")
        assertEquals("main", decoded.sessionId)
        assertEquals(Channel.MAIN, decoded.channel)
        assertNull(decoded.intent)
        assertNull(decoded.deviceContext)
        assertEquals(emptyList<Attachment>(), decoded.attachments)
    }

    @Test fun `toGatewayRequest narrows to the in-process request`() {
        val req = VisionRequest("hi", sessionId = "s7", channel = Channel.GROUP, locale = "en")
        val gw = req.toGatewayRequest()
        assertEquals("hi", gw.text)
        assertEquals("s7", gw.sessionId)
        assertEquals(Channel.GROUP, gw.channel)
    }

    // --- VisionResponse ---

    @Test fun `response round-trips and defaults finished true`() {
        val ok = VisionResponse(sessionId = "s1", text = "done")
        assertEquals(ok, roundTrip(ok))
        assertEquals(true, Json.decodeFromString<VisionResponse>("""{"sessionId":"s1"}""").finished)
    }

    @Test fun `response carries interrupt and error states`() {
        val paused = VisionResponse(sessionId = "s1", finished = false, awaitingConfirmation = true)
        val failed = VisionResponse(sessionId = "s1", error = "no network")
        assertEquals(paused, roundTrip(paused))
        assertEquals(failed, roundTrip(failed))
    }

    // --- Intent ---

    @Test fun `intent defaults confidence to one and CHAT is the fallback`() {
        val decoded = Json.decodeFromString<Intent>("""{"name":"search"}""")
        assertEquals(1f, decoded.confidence)
        assertEquals(emptyMap<String, String>(), decoded.slots)
        assertEquals("chat", Intent.CHAT.name)
    }

    // --- MemoryDto ---

    @Test fun `memory dto round-trips and defaults`() {
        val m = MemoryDto(content = "VPS ip is 1.2.3.4", type = MemoryType.FACT, importance = 0.8f, id = "m1", createdAt = 123L)
        assertEquals(m, roundTrip(m))
        val decoded = Json.decodeFromString<MemoryDto>("""{"content":"x"}""")
        assertEquals(MemoryType.FACT, decoded.type)
        assertEquals(0.5f, decoded.importance)
        assertNull(decoded.id)
    }

    // --- VisionEventDto + mappers ---

    @Test fun `every event variant round-trips through the dto losslessly`() {
        val events = listOf(
            VisionEvent.WakeWord,
            VisionEvent.AppOpened("com.foo"),
            VisionEvent.UserIdle(5_000L),
            VisionEvent.Scheduled("job-1"),
            VisionEvent.CommandReceived("open maps", "widget"),
            VisionEvent.ContextChanged("foregroundApp", "com.bar"),
            VisionEvent.Custom("ping", "payload"),
        )
        events.forEach { e -> assertEquals(e, e.toDto().toEvent()) }
    }

    @Test fun `event dto survives a json round-trip`() {
        val dto = VisionEvent.AppOpened("com.bar").toDto()
        assertEquals(dto, roundTrip(dto))
        assertEquals(EventKind.APP_OPENED, dto.kind)
        assertEquals("com.bar", dto.packageName)
    }

    @Test fun `event dto with missing payload rebuilds with safe defaults`() {
        assertEquals(VisionEvent.AppOpened(""), VisionEventDto(EventKind.APP_OPENED).toEvent())
        assertEquals(VisionEvent.UserIdle(0L), VisionEventDto(EventKind.USER_IDLE).toEvent())
        assertEquals(VisionEvent.Custom("", ""), VisionEventDto(EventKind.CUSTOM).toEvent())
    }
}
