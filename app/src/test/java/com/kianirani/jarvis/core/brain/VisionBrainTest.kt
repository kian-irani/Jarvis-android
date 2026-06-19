package com.kianirani.jarvis.core.brain

import com.kianirani.jarvis.core.gateway.Channel
import com.kianirani.jarvis.core.gateway.VisionGateway
import com.kianirani.jarvis.core.gateway.VisionRequest
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.memory.MemoryEngine
import com.kianirani.jarvis.core.memory.MemoryType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** DS-F1 — the VisionBrain facade is pure delegation onto VisionGateway + MemoryEngine. */
class VisionBrainTest {

    @Test
    fun `handle(text) builds a MAIN-channel main-session request and submits it`() {
        val gateway = mockk<VisionGateway>()
        val req = slot<VisionRequest>()
        every { gateway.submit(capture(req)) } returns emptyFlow()
        VisionBrain(gateway, mockk()).handle("hello")
        assertEquals("hello", req.captured.text)
        assertEquals(Channel.MAIN, req.captured.channel)
        assertEquals("main", req.captured.sessionId)
    }

    @Test
    fun `handle(request) passes the request straight through to the gateway`() {
        val gateway = mockk<VisionGateway>()
        val request = VisionRequest("hi", "s1", Channel.WIDGET)
        val flow = emptyFlow<GraphEvent>()
        every { gateway.submit(request) } returns flow
        assertSame(flow, VisionBrain(gateway, mockk()).handle(request))
    }

    @Test
    fun `state and resume defer to the gateway`() {
        val gateway = mockk<VisionGateway>(relaxed = true)
        every { gateway.activeSessions() } returns setOf("s1", "s2")
        val brain = VisionBrain(gateway, mockk())
        assertEquals(setOf("s1", "s2"), brain.state())
        val answer = VisionMessage.text(Role.USER, "yes")
        brain.resume("s1", answer)
        verify { gateway.resume("s1", answer) }
    }

    @Test
    fun `remember and recall defer to the memory engine with sane defaults`() = runTest {
        val memory = mockk<MemoryEngine>()
        coEvery { memory.remember("my ip is 1.2.3.4", MemoryType.FACT, 0.5f, emptyMap()) } returns "id1"
        // recall has defaulted pool/now (now = wall clock), so match query+topK and ignore the rest.
        coEvery { memory.recall(eq("ip"), eq(5), any(), any()) } returns
            listOf(MemoryEngine.Recalled("id1", "my ip is 1.2.3.4", MemoryType.FACT, 0.9f))
        val brain = VisionBrain(mockk(), memory)
        assertEquals("id1", brain.remember("my ip is 1.2.3.4"))
        assertEquals("id1", brain.recall("ip").single().id)
    }
}
