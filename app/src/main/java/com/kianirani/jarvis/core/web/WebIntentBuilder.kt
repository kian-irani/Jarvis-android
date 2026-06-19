package com.kianirani.jarvis.core.web

/**
 * PAB — browser / YouTube / media intent construction (PRD example: *"find Shakira's new song
 * on YouTube and play it"*). Pure builder of the **URIs** a `WebTool`/`MediaTool` fires as real
 * Android `Intent`s — YouTube search, a general web search, or opening a typed URL. Keeping URI
 * construction pure (percent-encoding, URL-vs-query detection) makes it deterministic and
 * JVM-tested; wrapping each [IntentSpec] in an actual `Intent` and launching it is the
 * on-device half (so Vision *really* opens it, never falsely claims to — FA3/CF2).
 */
data class IntentSpec(val uri: String, val target: Target) {
    enum class Target { YOUTUBE_WEB, YOUTUBE_APP, WEB_SEARCH, OPEN_URL }
}

object WebIntentBuilder {

    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"

    /** RFC-3986 percent-encoding (spaces → %20, not '+'), so the URIs work in any app. */
    fun encode(s: String): String = buildString {
        for (b in s.trim().toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            if (c.toChar() in UNRESERVED) append(c.toChar())
            else append('%').append("%02X".format(c))
        }
    }

    /** YouTube results page on the web (works even without the app installed). */
    fun youtubeSearch(query: String): IntentSpec =
        IntentSpec("https://www.youtube.com/results?search_query=${encode(query)}", IntentSpec.Target.YOUTUBE_WEB)

    /** Deep link the YouTube app's search (the device layer falls back to [youtubeSearch]). */
    fun youtubeApp(query: String): IntentSpec =
        IntentSpec("vnd.youtube://results?search_query=${encode(query)}", IntentSpec.Target.YOUTUBE_APP)

    /** A general web search. */
    fun webSearch(query: String): IntentSpec =
        IntentSpec("https://www.google.com/search?q=${encode(query)}", IntentSpec.Target.WEB_SEARCH)

    private val LOOKS_LIKE_DOMAIN = Regex("""^[a-z0-9-]+(\.[a-z0-9-]+)+(/\S*)?$""", RegexOption.IGNORE_CASE)

    /**
     * Interpret raw user input: an explicit URL (with scheme) opens as-is; a bare domain like
     * `youtube.com/x` is promoted to `https://`; anything else (free text) becomes a web search.
     */
    fun open(input: String): IntentSpec {
        val s = input.trim()
        return when {
            s.contains("://") -> IntentSpec(s, IntentSpec.Target.OPEN_URL)
            !s.contains(' ') && LOOKS_LIKE_DOMAIN.matches(s) -> IntentSpec("https://$s", IntentSpec.Target.OPEN_URL)
            else -> webSearch(s)
        }
    }
}
