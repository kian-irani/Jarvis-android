package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.score.DeviceMetrics
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeartbeatSenderTest {

    private fun sender(server: MockWebServer) = HeartbeatSender(
        nodeId = "node-42",
        nodeName = "pixel",
        address = "10.0.0.5:7799",
        brainBaseUrl = { server.url("/").toString() },
        metrics = { DeviceMetrics(ramFreeGb = 4.0, cpuCores = 8, batteryPercent = 90) },
    )

    @Test fun `beat posts stable id and encoded metrics to nodes endpoint`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"ok":true}"""))
            sender(server).beat()
            val req = server.takeRequest()
            assertEquals("POST", req.method)
            assertEquals("/nodes", req.path)
            val body = req.body.readUtf8()
            assertTrue(body.contains(""""id":"node-42""""))
            assertTrue(body.contains("ram_free_gb"))
            assertTrue(body.contains(""""name":"pixel""""))
            assertTrue(body.contains(""""brain_score""""))
        }
    }

    @Test fun `beat throws on http error so loop can retry next tick`() = runTest {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            val failed = runCatching { sender(server).beat() }.isFailure
            assertTrue(failed)
        }
    }
}
