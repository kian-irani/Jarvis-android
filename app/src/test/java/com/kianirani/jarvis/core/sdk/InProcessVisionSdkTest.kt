package com.kianirani.jarvis.core.sdk

import com.kianirani.jarvis.core.brain.VisionBrain
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.memory.MemoryEngine
import com.kianirani.jarvis.core.memory.MemoryType
import com.kianirani.jarvis.core.protocol.VisionRequest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.kianirani.jarvis.core.gateway.VisionRequest as GatewayRequest

/** DS-F3 — the in-process adapter is pure delegation onto VisionBrain, speaking protocol types. */
class InProcessVisionSdkTest {

    @Test fun `send projects the brain's events into protocol responses`() = runTest {
        val brain = mockk<VisionBrain>()
        every { brain.handle(any<GatewayRequest>()) } returns flowOf(
            GraphEvent.Token("Hel"),
            GraphEvent.Token("lo"),
            GraphEvent.Done(GraphState(messages = listOf(VisionMessage.text(Role.ASSISTANT, "Hello")))),
        )
        val out = InProcessVisionSdk(brain).send(VisionRequest("hi", sessionId = "s1")).toList()
        assertEquals("Hello", out.last().text)
        assertTrue(out.last().finished)
        assertEquals("s1", out.last().sessionId)
    }

    @Test fun `resume wraps the answer string into a USER message`() = runTest {
        val brain = mockk<VisionBrain>()
        val answer = slot<VisionMessage>()
        every { brain.resume(eq("s1"), capture(answer)) } returns emptyFlow()
        InProcessVisionSdk(brain).resume("s1", "yes").toList()
        assertEquals(Role.USER, answer.captured.role)
        assertEquals("yes", answer.captured.text())
    }

    @Test fun `recall maps Recalled to protocol MemoryDto with relevance in metadata`() = runTest {
        val brain = mockk<VisionBrain>()
        coEvery { brain.recall("ip", 5) } returns
            listOf(MemoryEngine.Recalled("id1", "vps ip is 1.2.3.4", MemoryType.FACT, 0.9f))
        val dto = InProcessVisionSdk(brain).recall("ip").single()
        assertEquals("id1", dto.id)
        assertEquals("vps ip is 1.2.3.4", dto.content)
        assertEquals(MemoryType.FACT, dto.type)
        assertEquals("0.9", dto.metadata["score"])
    }

    @Test fun `remember and sessions defer straight to the brain`() = runTest {
        val brain = mockk<VisionBrain>()
        coEvery { brain.remember("x", MemoryType.FACT, 0.5f, emptyMap()) } returns "id9"
        every { brain.state() } returns setOf("s1", "s2")
        val sdk = InProcessVisionSdk(brain)
        assertEquals("id9", sdk.remember("x"))
        assertEquals(setOf("s1", "s2"), sdk.sessions())
    }
}
