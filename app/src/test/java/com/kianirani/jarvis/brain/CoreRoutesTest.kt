package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.ChatReply
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.data.SearchHit
import com.kianirani.jarvis.brain.server.installBrainPlugins
import com.kianirani.jarvis.brain.server.routes.ChatPort
import com.kianirani.jarvis.brain.server.routes.EmbedPort
import com.kianirani.jarvis.brain.server.routes.chatRoutes
import com.kianirani.jarvis.brain.server.routes.embedRoutes
import com.kianirani.jarvis.brain.server.routes.memoryRoutes
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreRoutesTest {
    @Test
    fun `POST chat returns assistant reply`() = testApplication {
        val chat = mockk<ChatPort>()
        coEvery { chat.chat(any(), any()) } returns ChatReply(content = "salam")
        application { installBrainPlugins(); routing { chatRoutes(chat) } }
        val res = client.post("/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"messages":[{"role":"user","content":"hi"}]}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("salam"))
    }

    @Test
    fun `POST chat with empty messages is 400 VALIDATION`() = testApplication {
        application { installBrainPlugins(); routing { chatRoutes(mockk<ChatPort>()) } }
        val res = client.post("/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"messages":[]}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
        assertTrue(res.bodyAsText().contains("VALIDATION"))
    }

    @Test
    fun `POST embed before model ready is 503 MODEL_NOT_READY`() = testApplication {
        val embedPort = mockk<EmbedPort>()
        every { embedPort.isReady() } returns false
        application { installBrainPlugins(); routing { embedRoutes(embedPort, mockk<MemoryRepository>()) } }
        val res = client.post("/embed") {
            contentType(ContentType.Application.Json)
            setBody("""{"texts":["a"]}""")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        assertTrue(res.bodyAsText().contains("MODEL_NOT_READY"))
    }

    @Test
    fun `POST search delegates to memory repository`() = testApplication {
        val embedPort = mockk<EmbedPort>()
        every { embedPort.isReady() } returns true
        val mem = mockk<MemoryRepository>()
        coEvery { mem.search("cats", 5) } returns listOf(SearchHit("id1", "about cats", 0.9f))
        application { installBrainPlugins(); routing { embedRoutes(embedPort, mem) } }
        val res = client.post("/search") {
            contentType(ContentType.Application.Json)
            setBody("""{"query":"cats","top_k":5}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("id1"))
    }

    @Test
    fun `POST memory stores and returns id`() = testApplication {
        val mem = mockk<MemoryRepository>()
        coEvery { mem.store("episodic", "note", "{}") } returns "uuid-1"
        application { installBrainPlugins(); routing { memoryRoutes(mem) } }
        val res = client.post("/memory") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"episodic","content":"note","metadata":"{}"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("uuid-1"))
    }
}
