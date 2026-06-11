package com.kianirani.jarvis.brain.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JoinPayloadTest {

    @Test fun `encode-decode round trip`() {
        val p = JoinPayload("192.168.1.20", 7799, "s3cret token+/=")
        assertEquals(p, JoinPayload.decode(p.encode()))
    }

    @Test fun `decode accepts surrounding whitespace`() {
        assertEquals(
            JoinPayload("brain.local", 7799, "abc"),
            JoinPayload.decode("  vision://join?host=brain.local&port=7799&token=abc \n"),
        )
    }

    @Test fun `decode rejects invalid payloads`() {
        assertNull(JoinPayload.decode("https://example.com?host=a&port=1&token=t")) // wrong scheme
        assertNull(JoinPayload.decode("vision://join?host=&port=7799&token=t")) // blank host
        assertNull(JoinPayload.decode("vision://join?host=a&port=99999&token=t")) // bad port
        assertNull(JoinPayload.decode("vision://join?host=a&port=7799")) // missing token
        assertNull(JoinPayload.decode("plain-token-123")) // not a URI
    }
}
