package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.EventBus
import com.kianirani.jarvis.brain.data.FileRepository
import com.kianirani.jarvis.brain.data.NodeRepository
import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.brain.data.db.TaskEntity
import com.kianirani.jarvis.brain.server.installBrainPlugins
import com.kianirani.jarvis.brain.server.routes.eventRoutes
import com.kianirani.jarvis.brain.server.routes.fileRoutes
import com.kianirani.jarvis.brain.server.routes.nodeRoutes
import com.kianirani.jarvis.brain.server.routes.statusRoutes
import com.kianirani.jarvis.brain.server.routes.taskRoutes
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuxRoutesTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `POST nodes registers and GET lists`() = testApplication {
        val nodes = mockk<NodeRepository>()
        coEvery { nodes.register("phone", "127.0.0.1:7799", "{}", 80) } returns "n1"
        coEvery { nodes.list() } returns emptyList()
        application { installBrainPlugins(); routing { nodeRoutes(nodes) } }
        val res = client.post("/nodes") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"phone","address":"127.0.0.1:7799","capabilities":"{}","brain_score":80}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("n1"))
        assertEquals(HttpStatusCode.OK, client.get("/nodes").status)
    }

    @Test
    fun `POST task enqueues and GET by id returns status`() = testApplication {
        val tasksRepo = mockk<TaskRepository>()
        coEvery { tasksRepo.enqueue("echo", """{"x":1}""") } returns "t1"
        coEvery { tasksRepo.byId("t1") } returns TaskEntity("t1", "echo", """{"x":1}""", "pending", null, 0, null)
        coEvery { tasksRepo.byId("missing") } returns null
        application { installBrainPlugins(); routing { taskRoutes(tasksRepo) } }
        val res = client.post("/task") {
            contentType(ContentType.Application.Json)
            setBody("""{"kind":"echo","payload":{"x":1}}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("t1"))
        assertTrue(client.get("/task/t1").bodyAsText().contains("pending"))
        assertEquals(HttpStatusCode.NotFound, client.get("/task/missing").status)
    }

    @Test
    fun `files roundtrip via routes`() = testApplication {
        application { installBrainPlugins(); routing { fileRoutes(FileRepository(tmp.root)) } }
        val w = client.post("/files") {
            contentType(ContentType.Application.Json)
            setBody("""{"op":"write","path":"a/b.txt","content":"hey"}""")
        }
        assertEquals(HttpStatusCode.OK, w.status)
        val r = client.post("/files") {
            contentType(ContentType.Application.Json)
            setBody("""{"op":"read","path":"a/b.txt"}""")
        }
        assertTrue(r.bodyAsText().contains("hey"))
        assertTrue(client.get("/files?path=a").bodyAsText().contains("a/b.txt"))
    }

    @Test
    fun `status reports key pool`() = testApplication {
        application {
            installBrainPlugins()
            routing { statusRoutes(keyStatus = { listOf("ok", "rate_limited", "ok") }, requestCount = { 42 }) }
        }
        val body = client.get("/status").bodyAsText()
        assertTrue(body.contains("rate_limited"))
        assertTrue(body.contains("42"))
    }

    @Test
    fun `POST events publishes to bus`() = testApplication {
        val bus = EventBus()
        application { installBrainPlugins(); routing { eventRoutes(bus) } }
        val res = client.post("/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"kind":"ui.tap","payload":"{}"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }
}
