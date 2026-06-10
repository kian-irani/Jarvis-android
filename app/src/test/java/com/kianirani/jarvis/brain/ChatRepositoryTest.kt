package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.ChatMessage
import com.kianirani.jarvis.brain.data.ChatRepository
import com.kianirani.jarvis.brain.server.BrainException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ChatRepositoryTest {
    private val okBody = """{"choices":[{"message":{"role":"assistant","content":"hi"}}]}"""

    @Test
    fun `rotates to next key on 429`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setResponseCode(200).setBody(okBody).addHeader("Content-Type", "application/json"))
        server.start()
        val repo = ChatRepository(keys = listOf("k1", "k2", "k3"), baseUrl = server.url("/").toString())
        val reply = repo.chat(listOf(ChatMessage("user", "hello")), model = null)
        assertEquals("hi", reply.content)
        assertEquals(2, server.requestCount)
        server.shutdown()
    }

    @Test
    fun `throws ALL_KEYS_RATE_LIMITED when every key 429s`() = runTest {
        val server = MockWebServer()
        repeat(3) { server.enqueue(MockResponse().setResponseCode(429)) }
        server.start()
        val repo = ChatRepository(keys = listOf("k1", "k2", "k3"), baseUrl = server.url("/").toString())
        try {
            repo.chat(listOf(ChatMessage("user", "hello")), model = null)
            fail("expected BrainException")
        } catch (e: BrainException) {
            assertEquals("ALL_KEYS_RATE_LIMITED", e.code)
        }
        server.shutdown()
    }
}
