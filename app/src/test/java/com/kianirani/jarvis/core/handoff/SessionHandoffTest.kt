package com.kianirani.jarvis.core.handoff

import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MX session-handoff acceptance: a captured session round-trips through the wire losslessly,
 * malformed payloads decode to null, and staleness is detected. Pure.
 */
class SessionHandoffTest {

    private val messages = listOf(
        VisionMessage.text(Role.USER, "plan a trip to Shiraz"),
        VisionMessage.text(Role.ASSISTANT, "When would you like to go?"),
    )

    @Test fun `capture then encode-decode round-trips losslessly`() {
        val snap = SessionHandoff.capture("s1", messages, now = 1000, originNode = "phone", summary = "trip planning")
        val restored = SessionHandoff.decode(SessionHandoff.encode(snap))
        assertEquals(snap, restored)
        assertEquals("trip planning", restored?.summary)
        assertEquals(2, restored?.messages?.size)
    }

    @Test fun `decode of malformed payload is null`() {
        assertNull(SessionHandoff.decode("{not json"))
        assertNull(SessionHandoff.decode(""))
    }

    @Test fun `staleness is detected against max age`() {
        val snap = SessionHandoff.capture("s1", messages, now = 1000)
        assertFalse(SessionHandoff.isStale(snap, now = 1500, maxAgeMillis = 1000)) // 500ms old
        assertTrue(SessionHandoff.isStale(snap, now = 3000, maxAgeMillis = 1000)) // 2000ms old
    }

    @Test fun `defaults survive the round-trip`() {
        val snap = SessionHandoff.capture("s2", emptyList(), now = 5)
        val restored = SessionHandoff.decode(SessionHandoff.encode(snap))!!
        assertEquals("", restored.originNode)
        assertNull(restored.summary)
        assertTrue(restored.messages.isEmpty())
    }
}
