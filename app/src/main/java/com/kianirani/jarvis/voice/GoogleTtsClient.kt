package com.kianirani.jarvis.voice

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Free English/Latin TTS via Google Translate's read-aloud endpoint — a **Latin-only fallback**
 * behind Edge neural.
 *
 * IMPORTANT: Google Translate has **no Persian voice**. `translate_tts?tl=fa` returns HTTP 400
 * for any input (verified 2026-06), so this engine must never be used for a reply that contains
 * Persian — the caller guards that via [VoiceRouting.googleCanSpeak]. The earlier belief that
 * Google "speaks fa fluently" was wrong and made it the primary Persian engine (v129), which
 * only ever 400'd and delayed the real (Edge) voice. Edge neural is the primary engine; this is
 * the fallback for Latin-only replies when Edge's socket is blocked.
 *
 * Code-switch aware: the reply is split by [VoiceSegmenter] and each Latin/neutral run is fetched
 * with the right `tl` and the MP3 chunks concatenated. The endpoint caps a request near 200
 * chars, so each run is [chunk]ed on word boundaries. Any failure returns null so the caller
 * falls back to on-device — never silent, never regressive.
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
