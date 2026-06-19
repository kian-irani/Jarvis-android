package com.kianirani.jarvis.core.tools

import com.kianirani.jarvis.core.graph.ContentPart
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryToolsTest {

    private fun text(result: ContentPart.ToolResult) = (result.content.single() as ContentPart.Text).text
    private fun run(tool: VisionTool, args: JsonObject) = runBlocking { tool.execute(args, ToolContext()) }

    @Test fun `remember saves content and confirms`() {
        var saved: String? = null
        val tool = RememberTool { content -> saved = content; true }
        val result = run(tool, buildJsonObject { put("content", "user prefers dark mode") })
        assertEquals("user prefers dark mode", saved)
        assertFalse(result.isError)
        assertTrue(text(result).contains("Saved"))
    }

    @Test fun `remember reports when the store is not ready`() {
        val result = run(RememberTool { false }, buildJsonObject { put("content", "x") })
        assertTrue(result.isError)
    }

    @Test fun `remember rejects missing content`() {
        val result = run(RememberTool { true }, buildJsonObject { })
        assertTrue(result.isError)
        assertTrue(text(result).contains("missing"))
    }

    @Test fun `recall lists relevant memories`() {
        val tool = RecallTool { query, topK ->
            assertEquals("dark mode", query)
            assertEquals(5, topK)
            listOf("user prefers dark mode", "user dislikes neumorphism")
        }
        val result = run(tool, buildJsonObject { put("query", "dark mode") })
        assertTrue(text(result).contains("user prefers dark mode"))
        assertTrue(text(result).contains("user dislikes neumorphism"))
    }

    @Test fun `recall reports no matches`() {
        val result = run(RecallTool { _, _ -> emptyList() }, buildJsonObject { put("query", "x") })
        assertTrue(text(result).contains("No relevant memories"))
    }
}
