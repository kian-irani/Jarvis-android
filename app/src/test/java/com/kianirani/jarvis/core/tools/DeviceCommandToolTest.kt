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

class DeviceCommandToolTest {

    private fun text(result: ContentPart.ToolResult) = (result.content.single() as ContentPart.Text).text
    private fun run(tool: VisionTool, args: JsonObject) = runBlocking { tool.execute(args, ToolContext()) }

    @Test fun `runs the device command and returns its reply`() {
        var got: String? = null
        val tool = DeviceCommandTool { cmd -> got = cmd; "Opening Camera." }
        val result = run(tool, buildJsonObject { put("command", "open camera") })
        assertEquals("open camera", got)
        assertFalse(result.isError)
        assertEquals("Opening Camera.", text(result))
    }

    @Test fun `unrecognized command becomes an error so the model self-corrects`() {
        val result = run(DeviceCommandTool { null }, buildJsonObject { put("command", "tell me a joke") })
        assertTrue(result.isError)
        assertTrue(text(result).contains("Not a device command"))
    }

    @Test fun `missing command argument is an error`() {
        val result = run(DeviceCommandTool { "x" }, buildJsonObject { })
        assertTrue(result.isError)
        assertTrue(text(result).contains("missing"))
    }

    @Test fun `advertises an AUTO-trust, non read-only spec the model can call`() {
        val spec = DeviceCommandTool { null }.spec
        assertEquals("device_command", spec.name)
        assertEquals(com.kianirani.jarvis.core.agent.ActionRisk.AUTO, spec.trust)
        assertFalse(spec.readOnly) // launching an app mutates state
    }
}
