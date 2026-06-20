package com.kianirani.jarvis.data.tools

import java.net.URLEncoder

/**
 * PAB — pure parser that turns a natural-language web/media request (EN/FA) into a
 * single launchable web target. Kept free of Android so the routing is JVM unit-tested;
 * [WebTool] wraps it with the real `ACTION_VIEW` intent.
 *
 * Handles three shapes, in priority order:
 *  1. an explicit URL ("open github.com", "go to https://x.dev") → open it,
 *  2. a YouTube request ("play shakira on youtube", "آهنگ جدید شکیرا در یوتیوب") → YouTube search,
 *  3. a web search ("search for kotlin flows", "google …", "جستجو …") → Google search.
 * Anything else returns null so the command falls through to the other tools / the AI.
 */
object WebIntents {
    data class Target(val url: String, val reply: String)

    fun parse(message: String): Target? {
        val m = message.trim()
        if (m.isEmpty()) return null
        val low = m.lowercase()
        explicitUrl(m)?.let { return it }
        youtube(low, m)?.let { return it }
        webSearch(low, m)?.let { return it }
        return null
    }

    // A single bare-URL token (optionally prefixed by open/go-to/visit), e.g. "github.com",
    // "https://x.dev/y". Requires a known TLD so "open camera" stays an app launch, not a URL.
    private val URL_ONLY = Regex(
        """(https?://)?([a-z0-9-]+\.)+(com|org|net|io|dev|ir|me|app|co|gov|edu|ai|tv|info)(/\S*)?""",
        RegexOption.IGNORE_CASE,
    )

    private fun explicitUrl(m: String): Target? {
        var token = m
        for (p in listOf("open ", "go to ", "goto ", "visit ", "باز کن ", "برو به ")) {
            if (token.startsWith(p, ignoreCase = true)) { token = token.substring(p.length).trim(); break }
        }
        if (token.isEmpty() || token.any { it.isWhitespace() }) return null
        if (!URL_ONLY.matches(token)) return null
        val url = if (token.startsWith("http", ignoreCase = true)) token else "https://$token"
        return Target(url, "Opening $url.")
    }

    private fun youtube(low: String, original: String): Target? {
        if (!low.contains("youtube") && !original.contains("یوتیوب")) return null
        var q = original
        for (marker in YOUTUBE_NOISE) q = q.replace(marker, " ", ignoreCase = true)
        q = q.replace(WS, " ").trim().trim('،', ',', '.', '؟', '?', '!')
        return if (q.isBlank()) {
            Target("https://www.youtube.com", "Opening YouTube.")
        } else {
            Target("https://www.youtube.com/results?search_query=${enc(q)}", "Searching YouTube for \"$q\".")
        }
    }

    private fun webSearch(low: String, original: String): Target? {
        // Must START with a search verb — a command — so a sentence merely mentioning
        // "google"/"search" isn't hijacked into a search in the chat path.
        val marker = SEARCH_MARKERS.firstOrNull { low.startsWith(it) } ?: return null
        val q = original.substring(marker.length).trim().trim('،', ',', '.', '؟', '?', '!')
        if (q.isBlank()) return null
        return Target("https://www.google.com/search?q=${enc(q)}", "Searching the web for \"$q\".")
    }

    private val WS = Regex("""\s+""")
    private fun enc(q: String): String = URLEncoder.encode(q, "UTF-8")

    // YouTube markers + the command verbs around them, stripped to leave the query.
    private val YOUTUBE_NOISE = listOf(
        "on youtube", "in youtube", "youtube for", "youtube",
        "در یوتیوب", "از یوتیوب", "روی یوتیوب", "یوتیوب",
        "search for", "search", "play", "find", "open",
        "پخش کن", "پیدا کن", "نشونم بده", "جستجو کن", "جستجو", "سرچ کن", "سرچ",
    )

    // Web-search triggers; the query is whatever follows the (first-matching) marker. Longer,
    // more specific markers come first so "search the web for X" yields "X", not "the web for X".
    private val SEARCH_MARKERS = listOf(
        "search the web for", "search for", "google for", "look up", "search", "google",
        "جستجو کن", "جستجو برای", "جستجو", "سرچ کن", "سرچ", "بگرد دنبال",
    )
}
