package com.kianirani.jarvis.core.tools

import com.kianirani.jarvis.core.agent.ActionRisk
import com.kianirani.jarvis.core.graph.ContentPart
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {

    private fun fakeTool(name: String, trust: ActionRisk = ActionRisk.AUTO) = object : VisionTool {
        override val spec = ToolSpec(name, "desc of $name", buildJsonObject { put("type", "object") }, trust)
        override suspend fun execute(args: JsonObject, ctx: ToolContext) =
            ContentPart.ToolResult(callId = "c", name = name, content = listOf(ContentPart.Text("ok")))
    }

    private val registry = ToolRegistry(listOf(fakeTool("get_time"), fakeTool("send_sms", ActionRisk.CRITICAL)))

    @Test fun `byName finds a tool or returns null`() {
        assertEquals("get_time", registry.byName("get_time")?.spec?.name)
        assertNull(registry.byName("nope"))
    }

    @Test fun `allowlist restricts permitted tools`() {
        val reg = ToolRegistry(listOf(fakeTool("get_time"), fakeTool("send_sms")), ToolAllowlist(setOf("get_time")))
        assertEquals(listOf("get_time"), reg.permitted().map { it.spec.name })
    }

    @Test fun `null allowlist permits everything`() {
        assertEquals(listOf("get_time", "send_sms"), registry.permitted().map { it.spec.name })
        assertTrue(ToolAllowlist().permits("anything"))
    }

    @Test fun `functionSchema is null when the model lacks function-calling`() {
        assertNull(registry.functionSchema(supportsFunctionCalling = false))
    }

    @Test fun `functionSchema lists permitted tools in openai shape`() {
        val schema = registry.functionSchema(supportsFunctionCalling = true)!!
        assertEquals(2, schema.size)
        val fn = schema[0].jsonObject
        assertEquals("function", fn["type"]!!.jsonPrimitive.content)
        assertEquals("get_time", fn["function"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        assertEquals("desc of get_time", fn["function"]!!.jsonObject["description"]!!.jsonPrimitive.content)
    }

    @Test fun `functionSchema respects the allowlist`() {
        val reg = ToolRegistry(listOf(fakeTool("get_time"), fakeTool("send_sms")), ToolAllowlist(setOf("get_time")))
        val schema = reg.functionSchema(supportsFunctionCalling = true)!!
        assertEquals(1, schema.size)
        assertEquals("get_time", schema[0].jsonObject["function"]!!.jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test fun `tool spec round-trips through json with trust and parameters`() {
        val spec = ToolSpec("call", "make a call", buildJsonObject { put("type", "object") }, ActionRisk.CRITICAL)
        val json = Json
        assertEquals(spec, json.decodeFromString<ToolSpec>(json.encodeToString(spec)))
    }
}
