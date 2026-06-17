package com.kianirani.jarvis.voice

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Edge neural TTS websocket client (Voice Engine v3). Connects to Microsoft's
 * free read-aloud endpoint, sends the audio-format config + the code-switch SSML
 * built by [EdgeTtsProtocol], and collects the streamed MP3 bytes. Returns null
 * on any failure (blocked network, endpoint change, timeout) so the caller can
 * fall back to on-device TTS — the whole feature is opt-in and non-regressive.
 *
 * Network/coroutine only; all the parseable logic lives in [EdgeTtsProtocol] and
 * is unit-tested. This class is intentionally not unit-tested (it needs the live
 * service).
 */
class EdgeTtsClient {

    private val client by lazy { HttpClient(OkHttp) { install(WebSockets) } }

    /** Synthesize [text] to MP3 bytes, or null if anything goes wrong. */
    suspend fun synthesize(
        text: String,
        faVoice: String,
        enVoice: String,
        ratePercent: Int,
        pitchPercent: Int,
        nowSeconds: Long = System.currentTimeMillis() / 1000L,
        timeoutMs: Long = 12_000L,
    ): ByteArray? = runCatching {
        withTimeoutOrNull(timeoutMs) {
            val connectionId = UUID.randomUUID().toString().replace("-", "")
            val requestId = UUID.randomUUID().toString().replace("-", "")
            val ssml = EdgeTtsProtocol.buildSsml(text, faVoice, enVoice, ratePercent, pitchPercent)
            val out = ByteArrayOutputStream()

            client.webSocket(
                urlString = EdgeTtsProtocol.socketUrl(nowSeconds, connectionId),
                request = {
                    header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                    header("Pragma", "no-cache")
                    header("Cache-Control", "no-cache")
                    header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0",
                    )
                },
            ) {
                val ts = timestamp()
                send(Frame.Text(EdgeTtsProtocol.speechConfigMessage(ts)))
                send(Frame.Text(EdgeTtsProtocol.ssmlMessage(ssml, requestId, ts)))
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Binary -> EdgeTtsProtocol.audioFromFrame(frame.readBytes())?.let(out::write)
                        is Frame.Text -> if (frame.readText().contains("Path:turn.end")) return@webSocket
                        else -> Unit
                    }
                }
            }
            out.toByteArray().takeIf { it.isNotEmpty() }
        }
    }.getOrNull()

    private fun timestamp(): String =
        SimpleDateFormat(
            "EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'",
            Locale.US,
        ).format(System.currentTimeMillis())

    fun close() = runCatching { client.close() }
}
