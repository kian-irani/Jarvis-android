package com.kianirani.jarvis.core.gateway

import com.kianirani.jarvis.core.agent.ModelClient
import com.kianirani.jarvis.core.agent.ModelResponse
import com.kianirani.jarvis.core.agent.VisionAgent
import com.kianirani.jarvis.core.graph.Checkpointer
import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.tools.ToolRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class VisionGatewayTest {

    private val checkpointer = FakeCheckpointer()

    private fun agent(answer: String) = VisionAgent(
        ModelClient { _, _ -> ModelResponse(VisionMessage.text(Role.ASSISTANT, answer)) },
        ToolRegistry(emptyList()),
        checkpointer = checkpointer,
    )

    private fun gateway(seen: MutableList<Channel> = mutableListOf()) = VisionGateway { channel ->
        seen += channel
        agent(if (channel == Channel.MAIN) "main-answer" else "restricted-answer")
    }

    private fun finalText(events: List<GraphEvent>) = (events.last() as GraphEvent.Done).state.messages.last().text()

    @Test fun `submit routes to the channel's agent`() {
        val gw = gateway()
        assertEquals("main-answer", finalText(runBlocking { gw.submit(VisionRequest("hi", "s1", Channel.MAIN)).toList() }))
        assertEquals(
            "restricted-answer",
            finalText(runBlocking { gw.submit(VisionRequest("hi", "s2", Channel.GROUP)).toList() }),
        )
    }

    @Test fun `tracks active sessions`() {
        val gw = gateway()
        runBlocking { gw.submit(VisionRequest("a", "s1", Channel.MAIN)).toList() }
        runBlocking { gw.submit(VisionRequest("b", "s2", Channel.GROUP)).toList() }
        assertEquals(setOf("s1", "s2"), gw.activeSessions())
    }

    @Test fun `resume routes to the session's original channel`() {
        val seen = mutableListOf<Channel>()
        val gw = gateway(seen)
        runBlocking { gw.submit(VisionRequest("a", "s1", Channel.GROUP)).toList() }
        seen.clear()
        runBlocking { gw.resume("s1", VisionMessage.text(Role.USER, "yes")).toList() }
        assertEquals(listOf(Channel.GROUP), seen)
    }

    private class FakeCheckpointer : Checkpointer {
        private data class Snap(val state: GraphState, val cursor: String)

        private val log = mutableMapOf<String, MutableList<Snap>>()

        override suspend fun save(threadId: String, state: GraphState, cursor: String) {
            log.getOrPut(threadId) { mutableListOf() }.add(Snap(state, cursor))
        }

        override suspend fun load(threadId: String): GraphState? = log[threadId]?.lastOrNull()?.state
        override suspend fun loadCursor(threadId: String): String? = log[threadId]?.lastOrNull()?.cursor
        override suspend fun history(threadId: String): List<GraphState> = log[threadId]?.map { it.state } ?: emptyList()
    }
}
