package com.kianirani.jarvis.voice

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * FV-FA — reliable free Persian (and English) TTS via Google Translate's read-aloud endpoint.
 *
 * The user's recurring complaint is that Persian replies are silent: AUTO mode routes Persian
 * to the Edge neural voice, but Microsoft's Bing websocket is unreliable/geo-blocked, and the
 * on-device fallback ships **no `fa-IR` voice** — so nothing plays. Google's `translate_tts`
 * endpoint speaks `fa` fluently over a plain HTTP GET (no key, no token), so it is the most
 * dependable free Persian voice and becomes the primary online engine.
 *
 * Code-switch aware: the reply is split by [VoiceSegmenter] and each Persian/Latin run is
 * fetched with the right `tl` (fa/en) and the MP3 chunks concatenated, so "یک playlist از
 * Shakira" speaks each language correctly instead of one mangling the other. The endpoint caps
 * a request near 200 chars, so each run is [chunk]ed on word boundaries. Any failure returns
 * null so the caller falls back to Edge, then on-device — never silent, never regressive.
 */
class GoogleTtsClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    /**
     * Synthesize [text] to MP3 bytes, code-switch aware. [neutralLang] ("fa"/"en") is used for
     * script-neutral runs (digits, punctuation). Returns null on any network/format failure.
     */
    fun synthesize(text: String, neutralLang: String): ByteArray? = runCatching {
        val out = ByteArrayOutputStream()
        for (seg in VoiceSegmenter.segment(text)) {
            val tl = when (seg.script) {
                VoiceSegmenter.Script.PERSIAN -> "fa"
                VoiceSegmenter.Script.LATIN -> "en"
                VoiceSegmenter.Script.NEUTRAL -> neutralLang
            }
            for (part in chunk(seg.text, MAX_CHARS)) {
                val bytes = fetch(part, tl) ?: return@runCatching null
                out.write(bytes)
            }
        }
        out.toByteArray().takeIf { it.isNotEmpty() }
    }.getOrNull()

    private fun fetch(q: String, tl: String): ByteArray? = runCatching {
        val url = "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob" +
            "&tl=$tl&total=1&idx=0&textlen=${q.length}&q=${URLEncoder.encode(q, "UTF-8")}"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Referer", "https://translate.google.com/")
            .build()
        client.newCall(req).execute().use { res ->
            // Only accept real audio — a block page / redirect comes back as text/html, which we
            // reject so the caller can fall back instead of "playing" an HTML error.
            val ctype = res.header("Content-Type").orEmpty()
            if (!res.isSuccessful || !ctype.contains("audio", ignoreCase = true)) {
                null
            } else {
                res.body?.bytes()?.takeIf { it.isNotEmpty() }
            }
        }
    }.getOrNull()

    fun close() {
        runCatching {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }

    companion object {
        private const val MAX_CHARS = 190
        private const val UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36"

        /** Split [text] into ≤[max]-char chunks on word boundaries (Google TTS request cap). */
        fun chunk(text: String, max: Int): List<String> {
            val t = text.trim()
            if (t.isEmpty()) return emptyList()
            if (t.length <= max) return listOf(t)
            val out = mutableListOf<String>()
            val sb = StringBuilder()
            for (word in t.split(Regex("\\s+"))) {
                when {
                    word.length > max -> { // a single oversized token → hard-split it
                        if (sb.isNotEmpty()) { out.add(sb.toString()); sb.clear() }
                        word.chunked(max).forEach(out::add)
                    }
                    sb.isNotEmpty() && sb.length + 1 + word.length > max -> {
                        out.add(sb.toString()); sb.clear(); sb.append(word)
                    }
                    else -> {
                        if (sb.isNotEmpty()) sb.append(' ')
                        sb.append(word)
                    }
                }
            }
            if (sb.isNotEmpty()) out.add(sb.toString())
            return out
        }
    }
}
