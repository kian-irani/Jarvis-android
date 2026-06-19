package com.kianirani.jarvis.core.web

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * PAB acceptance: queries percent-encode correctly (spaces → %20), YouTube/web-search URIs are
 * well-formed, and `open` distinguishes a URL, a bare domain, and free text. Pure.
 */
class WebIntentBuilderTest {

    @Test fun `encode percent-encodes spaces and specials, not unreserved`() {
        assertEquals("Shakira%20new%20song", WebIntentBuilder.encode("Shakira new song"))
        assertEquals("a%26b%3Dc", WebIntentBuilder.encode("a&b=c"))
        assertEquals("hello-world_1.2~x", WebIntentBuilder.encode("hello-world_1.2~x"))
    }

    @Test fun `youtube search builds a web results url`() {
        val spec = WebIntentBuilder.youtubeSearch("Shakira new song")
        assertEquals("https://www.youtube.com/results?search_query=Shakira%20new%20song", spec.uri)
        assertEquals(IntentSpec.Target.YOUTUBE_WEB, spec.target)
    }

    @Test fun `youtube app builds a deep link`() {
        val spec = WebIntentBuilder.youtubeApp("lofi")
        assertEquals("vnd.youtube://results?search_query=lofi", spec.uri)
        assertEquals(IntentSpec.Target.YOUTUBE_APP, spec.target)
    }

    @Test fun `web search builds a google query`() {
        assertEquals(
            "https://www.google.com/search?q=weather%20today",
            WebIntentBuilder.webSearch("weather today").uri,
        )
    }

    @Test fun `open keeps an explicit url`() {
        val spec = WebIntentBuilder.open("https://example.com/path?x=1")
        assertEquals("https://example.com/path?x=1", spec.uri)
        assertEquals(IntentSpec.Target.OPEN_URL, spec.target)
    }

    @Test fun `open promotes a bare domain to https`() {
        val spec = WebIntentBuilder.open("youtube.com/feed")
        assertEquals("https://youtube.com/feed", spec.uri)
        assertEquals(IntentSpec.Target.OPEN_URL, spec.target)
    }

    @Test fun `open treats free text as a web search`() {
        val spec = WebIntentBuilder.open("best pizza near me")
        assertEquals(IntentSpec.Target.WEB_SEARCH, spec.target)
        assertEquals("https://www.google.com/search?q=best%20pizza%20near%20me", spec.uri)
    }
}
