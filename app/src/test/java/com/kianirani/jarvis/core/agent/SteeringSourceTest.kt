package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SteeringSourceTest {

    private fun msg(text: String) = VisionMessage.text(Role.USER, text)

    @Test fun `drain ALL returns everything and clears the queue`() {
        val source = QueueSteeringSource(QueueMode.ALL)
        source.push(msg("a"))
        source.push(msg("b"))
        assertEquals(listOf("a", "b"), runBlocking { source.drain() }.map { it.text() })
        assertTrue(runBlocking { source.drain() }.isEmpty())
    }

    @Test fun `drain ONE returns a single message FIFO`() {
        val source = QueueSteeringSource(QueueMode.ONE)
        source.push(msg("a"))
        source.push(msg("b"))
        assertEquals(listOf("a"), runBlocking { source.drain() }.map { it.text() })
        assertEquals(listOf("b"), runBlocking { source.drain() }.map { it.text() })
        assertTrue(runBlocking { source.drain() }.isEmpty())
    }

    @Test fun `an empty queue drains to nothing`() {
        assertTrue(runBlocking { QueueSteeringSource().drain() }.isEmpty())
    }
}
