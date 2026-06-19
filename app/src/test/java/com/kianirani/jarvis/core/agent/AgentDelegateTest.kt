package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.tools.ToolContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentDelegateTest {

    private fun role(id: String, label: String) = AgentRole(id, label, "do $label", "backstory")

    private val roster = listOf(
        role("RESEARCH", "research assistant"),
        role("DEVICE", "device operator"),
    )

    private fun args(coworker: String? = null, task: String? = null): JsonObject = buildJsonObject {
        coworker?.let { put("coworker", it) }
        task?.let { put("task", it) }
    }

    private fun text(r: ContentPart.ToolResult) =
        r.content.filterIsInstance<ContentPart.Text>().joinToString(" ") { it.text }

    @Test fun `delegates to the named coworker and returns its answer`() {
        var seenRole: AgentRole? = null
        var seenTask: String? = null
        val delegate = AgentDelegate(roster, { r, t -> seenRole = r; seenTask = t; "found it" })
        val result = runBlocking { delegate.execute(args("research assistant", "who won?"), ToolContext()) }
        assertFalse(result.isError)
        assertEquals("found it", text(result))
        assertEquals("RESEARCH", seenRole?.id)
        assertEquals("who won?", seenTask)
    }

    @Test fun `matches a coworker by id, case-insensitively`() {
        val delegate = AgentDelegate(roster, { r, _ -> "ran ${r.id}" })
        val result = runBlocking { delegate.execute(args("device", "turn on flashlight"), ToolContext()) }
        assertEquals("ran DEVICE", text(result))
    }

    @Test fun `unknown coworker is an error result listing valid names, runner not called`() {
        var called = false
        val delegate = AgentDelegate(roster, { _, _ -> called = true; "x" })
        val result = runBlocking { delegate.execute(args("chef", "cook"), ToolContext()) }
        assertTrue(result.isError)
        assertTrue(text(result).contains("research assistant"))
        assertFalse(called)
    }

    @Test fun `a missing argument is an error result`() {
        val delegate = AgentDelegate(roster, { _, _ -> "x" })
        assertTrue(runBlocking { delegate.execute(args(coworker = "research assistant"), ToolContext()) }.isError)
        assertTrue(runBlocking { delegate.execute(args(task = "no coworker"), ToolContext()) }.isError)
    }

    @Test fun `a failing coworker degrades to an error result, never throws`() {
        val delegate = AgentDelegate(roster, { _, _ -> throw IllegalStateException("model down") })
        val result = runBlocking { delegate.execute(args("device operator", "do it"), ToolContext()) }
        assertTrue(result.isError)
        assertTrue(text(result).contains("model down"))
    }

    @Test fun `spec advertises the coworkers and requires both params`() {
        val delegate = AgentDelegate(roster, { _, _ -> "x" })
        assertEquals("delegate_to_coworker", delegate.spec.name)
        assertEquals(ActionRisk.AUTO, delegate.spec.trust)
        assertTrue(delegate.spec.description.contains("device operator"))
        val required = delegate.spec.parameters["required"]!!.jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("coworker", "task"), required)
        assertTrue(delegate.spec.parameters["properties"]!!.jsonObject.containsKey("coworker"))
    }
}
