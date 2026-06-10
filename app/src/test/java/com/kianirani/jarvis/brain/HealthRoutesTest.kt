package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.server.installBrainPlugins
import com.kianirani.jarvis.brain.server.routes.HealthState
import com.kianirani.jarvis.brain.server.routes.healthRoutes
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthRoutesTest {
    @Test
    fun `health returns ok envelope with capabilities`() = testApplication {
        application {
            installBrainPlugins()
            routing { healthRoutes(HealthState(version = "16.0.0", embedReady = { false }, storageUsedBytes = { 0L })) }
        }
        val res = client.get("/health")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("\"ok\":true"))
        assertTrue(body.contains("\"chat\":\"cloud\""))
        assertTrue(body.contains("\"embed\":\"local\""))
    }
}
