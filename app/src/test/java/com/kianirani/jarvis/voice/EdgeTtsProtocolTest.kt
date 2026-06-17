package com.kianirani.jarvis.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeTtsProtocolTest {

    @Test fun `gec token is 64 uppercase hex chars`() {
        val gec = EdgeTtsProtocol.secMsGec(1_750_000_000L)
        assertEquals(64, gec.length)
        assertTrue(gec.all { it in '0'..'9' || it in 'A'..'F' })
    }

    @Test fun `gec token is stable within the same 5-minute window`() {
        // Two times 60 s apart fall in the same 5-min bucket → identical token.
        assertEquals(EdgeTtsProtocol.secMsGec(1_750_000_000L), EdgeTtsProtocol.secMsGec(1_750_000_060L))
    }

    @Test fun `gec token changes across 5-minute windows`() {
        val a = EdgeTtsProtocol.secMsGec(1_750_000_000L)
        val b = EdgeTtsProtocol.secMsGec(1_750_000_000L + 600)
        assertTrue(a != b)
    }

    @Test fun `rounded ticks are a multiple of the five minute tick count`() {
        assertEquals(0L, EdgeTtsProtocol.roundedTicks(1_750_000_000L) % 3_000_000_000L)
    }

    @Test fun `ssml splits persian and english into separate neural voices`() {
        val ssml = EdgeTtsProtocol.buildSsml("سلام Shakira")
        assertTrue(ssml.contains("name='${EdgeTtsProtocol.DEFAULT_FA_VOICE}'"))
        assertTrue(ssml.contains("name='${EdgeTtsProtocol.DEFAULT_EN_VOICE}'"))
        assertTrue(ssml.startsWith("<speak"))
        assertTrue(ssml.trim().endsWith("</speak>"))
    }

    @Test fun `ssml merges consecutive same-voice runs into one voice block`() {
        // Pure English → exactly one <voice> block.
        val ssml = EdgeTtsProtocol.buildSsml("hello there friend")
        assertEquals(1, Regex("<voice ").findAll(ssml).count())
    }

    @Test fun `ssml escapes xml metacharacters`() {
        val ssml = EdgeTtsProtocol.buildSsml("Tom & Jerry <x>")
        assertTrue(ssml.contains("Tom &amp; Jerry &lt;x&gt;"))
        assertTrue(!ssml.contains("Tom & Jerry"))
    }

    @Test fun `rate and pitch are rendered as signed percentages`() {
        val ssml = EdgeTtsProtocol.buildSsml("hi", ratePercent = 10, pitchPercent = -5)
        assertTrue(ssml.contains("rate='+10%'"))
        assertTrue(ssml.contains("pitch='-5%'"))
    }

    @Test fun `audio frame parsing returns the payload after the header`() {
        val header = "Path:audio\r\n".toByteArray(Charsets.US_ASCII)
        val audio = byteArrayOf(1, 2, 3, 4, 5)
        val frame = ByteArray(2 + header.size + audio.size)
        frame[0] = (header.size shr 8).toByte()
        frame[1] = (header.size and 0xFF).toByte()
        System.arraycopy(header, 0, frame, 2, header.size)
        System.arraycopy(audio, 0, frame, 2 + header.size, audio.size)
        assertTrue(audio.contentEquals(EdgeTtsProtocol.audioFromFrame(frame)))
    }

    @Test fun `non-audio frame yields null`() {
        val header = "Path:turn.start\r\n".toByteArray(Charsets.US_ASCII)
        val frame = ByteArray(2 + header.size)
        frame[0] = (header.size shr 8).toByte()
        frame[1] = (header.size and 0xFF).toByte()
        System.arraycopy(header, 0, frame, 2, header.size)
        assertNull(EdgeTtsProtocol.audioFromFrame(frame))
    }

    @Test fun `socket url carries the signed gec params`() {
        val url = EdgeTtsProtocol.socketUrl(1_750_000_000L, "abc123")
        assertTrue(url.contains("TrustedClientToken=${EdgeTtsProtocol.TRUSTED_CLIENT_TOKEN}"))
        assertTrue(url.contains("Sec-MS-GEC="))
        assertTrue(url.contains("Sec-MS-GEC-Version=${EdgeTtsProtocol.GEC_VERSION}"))
        assertTrue(url.contains("ConnectionId=abc123"))
    }
}
