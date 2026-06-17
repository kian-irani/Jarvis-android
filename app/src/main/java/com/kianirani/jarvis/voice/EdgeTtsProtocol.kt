package com.kianirani.jarvis.voice

import java.security.MessageDigest

/**
 * Microsoft Edge **neural** TTS protocol (Voice Engine v3, 2026-06-17). Edge's
 * read-aloud endpoint exposes the same high-quality Azure neural voices for free
 * (no API key) — including fluent Persian (fa-IR-DilaraNeural / FaridNeural). The
 * user asked for a free engine that speaks Persian smoothly and never gets
 * confused mixing Persian/English, so this builds ONE SSML document with a
 * per-segment <voice> (driven by [VoiceSegmenter]) — Persian runs in a Persian
 * neural voice, Latin runs in an English neural voice — in a single request.
 *
 * Everything here is pure + deterministic (the network/audio lives in
 * EdgeTtsClient), so the GEC token math, SSML building and binary-frame parsing
 * are all JVM-unit-testable. The whole feature is opt-in with an on-device
 * fallback, so if the endpoint changes or is blocked nothing regresses.
 */
object EdgeTtsProtocol {

    const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    const val GEC_VERSION = "1-130.0.2849.68"
    const val OUTPUT_FORMAT = "audio-24khz-48kbitrate-mono-mp3"
    const val WSS_BASE =
        "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1"

    const val DEFAULT_FA_VOICE = "fa-IR-DilaraNeural"
    const val DEFAULT_EN_VOICE = "en-US-AriaNeural"

    /** Windows FILETIME ticks (100 ns since 1601) for [unixSeconds], floored to 5 min. */
    fun roundedTicks(unixSeconds: Long): Long {
        val winTicks = (unixSeconds + 11_644_473_600L) * 10_000_000L
        return winTicks - (winTicks % 3_000_000_000L) // 3e9 ticks = 300 s = 5 min
    }

    /** The `Sec-MS-GEC` clearance token: SHA256(ticks+token), uppercase hex. */
    fun secMsGec(unixSeconds: Long): String {
        val payload = "${roundedTicks(unixSeconds)}$TRUSTED_CLIENT_TOKEN"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.US_ASCII))
        return digest.joinToString("") { "%02X".format(it) }
    }

    /** Full websocket URL including the signed GEC params. */
    fun socketUrl(unixSeconds: Long, connectionId: String): String =
        "$WSS_BASE?TrustedClientToken=$TRUSTED_CLIENT_TOKEN" +
            "&Sec-MS-GEC=${secMsGec(unixSeconds)}&Sec-MS-GEC-Version=$GEC_VERSION" +
            "&ConnectionId=$connectionId"

    /** The first text frame configuring the audio output format. */
    fun speechConfigMessage(timestamp: String): String =
        "X-Timestamp:$timestamp\r\n" +
            "Content-Type:application/json; charset=utf-8\r\n" +
            "Path:speech.config\r\n\r\n" +
            """{"context":{"synthesis":{"audio":{"metadataoptions":""" +
            """{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},""" +
            """"outputFormat":"$OUTPUT_FORMAT"}}}}"""

    /** The SSML text frame carrying the document to synthesize. */
    fun ssmlMessage(ssml: String, requestId: String, timestamp: String): String =
        "X-RequestId:$requestId\r\n" +
            "Content-Type:application/ssml+xml\r\n" +
            "X-Timestamp:$timestamp\r\n" +
            "Path:ssml\r\n\r\n" + ssml

    /**
     * Build a code-switch SSML document: consecutive [VoiceSegmenter] runs of the
     * same voice are merged into one <voice> block, Persian → [faVoice], everything
     * else → [enVoice]. [ratePercent]/[pitchPercent] apply to every block.
     */
    fun buildSsml(
        text: String,
        faVoice: String = DEFAULT_FA_VOICE,
        enVoice: String = DEFAULT_EN_VOICE,
        ratePercent: Int = 0,
        pitchPercent: Int = 0,
    ): String {
        val sb = StringBuilder()
        sb.append("<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>")
        var currentVoice: String? = null
        for (seg in VoiceSegmenter.segment(text)) {
            val voice = if (seg.script == VoiceSegmenter.Script.PERSIAN) faVoice else enVoice
            if (voice != currentVoice) {
                if (currentVoice != null) sb.append("</prosody></voice>")
                sb.append("<voice name='").append(voice).append("'>")
                    .append("<prosody rate='").append(signed(ratePercent)).append("' pitch='")
                    .append(signed(pitchPercent)).append("'>")
                currentVoice = voice
            }
            sb.append(escapeXml(seg.text))
        }
        if (currentVoice != null) sb.append("</prosody></voice>")
        sb.append("</speak>")
        return sb.toString()
    }

    /**
     * Extract the audio payload from one binary websocket frame, or null when the
     * frame isn't an audio frame. Frame layout: 2-byte big-endian header length,
     * the ASCII header (contains `Path:audio`), then the audio bytes.
     */
    fun audioFromFrame(frame: ByteArray): ByteArray? {
        if (frame.size < 2) return null
        val headerLen = ((frame[0].toInt() and 0xFF) shl 8) or (frame[1].toInt() and 0xFF)
        val headerEnd = 2 + headerLen
        if (headerEnd > frame.size) return null
        val header = String(frame, 2, headerLen, Charsets.US_ASCII)
        if (!header.contains("Path:audio")) return null
        return frame.copyOfRange(headerEnd, frame.size)
    }

    private fun signed(percent: Int): String = if (percent >= 0) "+$percent%" else "$percent%"

    private fun escapeXml(s: String): String = buildString(s.length) {
        for (c in s) when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&apos;")
            else -> append(c)
        }
    }
}
