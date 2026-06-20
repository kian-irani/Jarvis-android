package com.kianirani.jarvis.data.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebIntentsTest {

    @Test fun `bare domain opens as https url`() {
        val t = WebIntents.parse("open github.com")!!
        assertEquals("https://github.com", t.url)
    }

    @Test fun `explicit https url is kept verbatim`() {
        val t = WebIntents.parse("go to https://kotlinlang.org/docs")!!
        assertEquals("https://kotlinlang.org/docs", t.url)
    }

    @Test fun `open an app name is not treated as a url`() {
        // "camera" has no TLD → WebTool must not claim it (CommandInterpreter opens the app).
        assertNull(WebIntents.parse("open camera"))
    }

    @Test fun `youtube request becomes a youtube search`() {
        val t = WebIntents.parse("play shakira new song on youtube")!!
        assertTrue(t.url.startsWith("https://www.youtube.com/results?search_query="))
        assertTrue(t.url.contains("shakira"))
        assertTrue(t.reply.contains("YouTube"))
    }

    @Test fun `persian youtube request is parsed`() {
        val t = WebIntents.parse("آهنگ جدید شکیرا را در یوتیوب پیدا کن")!!
        assertTrue(t.url.startsWith("https://www.youtube.com/results?search_query="))
        // The query keeps the meaningful Persian content (encoded), markers stripped.
        assertTrue(t.url.contains("%"))
    }

    @Test fun `bare youtube opens the app`() {
        assertEquals("https://www.youtube.com", WebIntents.parse("open youtube")!!.url)
    }

    @Test fun `search for runs a google search with the query`() {
        val t = WebIntents.parse("search for kotlin coroutines")!!
        assertEquals("https://www.google.com/search?q=kotlin+coroutines", t.url)
    }

    @Test fun `search the web for strips the long marker`() {
        val t = WebIntents.parse("search the web for vision os")!!
        assertEquals("https://www.google.com/search?q=vision+os", t.url)
    }

    @Test fun `persian web search`() {
        val t = WebIntents.parse("جستجو کن قیمت دلار")!!
        assertTrue(t.url.startsWith("https://www.google.com/search?q="))
    }

    @Test fun `a plain sentence is not a web command`() {
        assertNull(WebIntents.parse("what's the weather like today"))
        assertNull(WebIntents.parse(""))
    }
}
