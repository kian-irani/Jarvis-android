package com.kianirani.jarvis.core.agent

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrewTest {

    private fun role(id: String) = AgentRole(id, "the $id", "do $id", "backstory $id")

    @Test fun `sequential pipes each output into the next agent`() {
        val inputs = mutableListOf<String>()
        val crew = Crew { r, input ->
            inputs += input
            "result-of-${r.id}"
        }
        val result = runBlocking {
            crew.run(listOf(role("a"), role("b")), task = "start", process = Process.SEQUENTIAL)
        }
        assertEquals(listOf("result-of-a", "result-of-b"), result.outputs)
        assertEquals("result-of-b", result.finalOutput)
        // The second agent saw the first agent's output in its context.
        assertEquals("start", inputs[0])
        assertTrue(inputs[1].contains("result-of-a"))
    }

    @Test fun `hierarchical delegates to workers then the manager synthesizes`() {
        val seen = mutableListOf<String>()
        val crew = Crew { r, input ->
            seen += r.id
            "out-${r.id}"
        }
        val result = runBlocking {
            crew.run(listOf(role("w1"), role("w2")), task = "goal", process = Process.HIERARCHICAL, manager = role("mgr"))
        }
        assertEquals(listOf("w1", "w2", "mgr"), seen) // workers then manager
        assertEquals(listOf("out-w1", "out-w2", "out-mgr"), result.outputs)
        assertEquals("out-mgr", result.finalOutput)
    }

    @Test fun `hierarchical without a manager fails fast`() {
        val crew = Crew { _, _ -> "x" }
        try {
            runBlocking { crew.run(listOf(role("a")), task = "t", process = Process.HIERARCHICAL) }
            throw AssertionError("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test fun `empty crew yields no outputs`() {
        val crew = Crew { _, _ -> "x" }
        val result = runBlocking { crew.run(emptyList(), task = "t") }
        assertTrue(result.outputs.isEmpty())
        assertEquals("", result.finalOutput)
    }
}
