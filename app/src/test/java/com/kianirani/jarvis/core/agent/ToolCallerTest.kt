package com.kianirani.jarvis.core.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** CF2 — ToolCaller.parse: model reply → tool invocation (pure, no Android). */
class ToolCallerTest {

    @Test
    fun `parses a bare json tool call`() {
        val inv = ToolCaller.parse("""{"tool":"action","args":"call mum"}""")
        assertEquals("action", inv?.tool)
        assertEquals("call mum", inv?.args)
    }

    @Test
    fun `parses a tool call inside a code fence`() {
        val inv = ToolCaller.parse(
            """
            Sure.
            ```json
            {"tool": "action", "args": "open camera"}
            ```
            """.trimIndent(),
        )
        assertEquals("action", inv?.tool)
        assertEquals("open camera", inv?.args)
    }

    @Test
    fun `keeps persian args intact`() {
        val inv = ToolCaller.parse("""{"tool":"action","args":"با مامان تماس بگیر"}""")
        assertEquals("با مامان تماس بگیر", inv?.args)
    }

    @Test
    fun `unescapes quotes inside args`() {
        val inv = ToolCaller.parse("""{"tool":"action","args":"text ali saying \"on my way\""}""")
        assertEquals("""text ali saying "on my way"""", inv?.args)
    }

    @Test
    fun `plain answer is not a tool call`() {
        assertNull(ToolCaller.parse("The capital of France is Paris."))
        assertNull(ToolCaller.parse(""))
        assertNull(ToolCaller.parse(null))
    }

    @Test
    fun `malformed or empty fields are rejected`() {
        assertNull(ToolCaller.parse("""{"tool":"action"}""")) // no args
        assertNull(ToolCaller.parse("""{"args":"call mum"}""")) // no tool
        assertNull(ToolCaller.parse("""{"tool":"","args":"call mum"}""")) // empty tool
        assertNull(ToolCaller.parse("""{"tool":"action","args":"   "}""")) // blank args
    }
}
