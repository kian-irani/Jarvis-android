package com.kianirani.jarvis.data.ai

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import com.kianirani.jarvis.core.tools.ToolSpec
import com.kianirani.jarvis.core.tools.toOpenAiFunction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionCallingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private fun obj(text: String): JsonObject = json.decodeFromString(JsonObject.serializer(), text)

    private val schema = buildJsonArray {
        add(
            ToolSpec(
                name = "send_sms",
                description = "Send a text message",
                parameters = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") { putJsonObject("to") { put("type", "string") } }
                },
            ).toOpenAiFunction(),
        )
    }

    // ── wire mapping ──────────────────────────────────────────────────────────

    @Test fun `maps every provider to a wire family`() {
        assertEquals(FunctionCalling.Wire.ANTHROPIC, FunctionCalling.wireOf(AiProvider.ANTHROPIC))
        assertEquals(FunctionCalling.Wire.GEMINI, FunctionCalling.wireOf(AiProvider.GEMINI))
        for (p in listOf(AiProvider.OPENAI, AiProvider.XAI, AiProvider.GROQ, AiProvider.OPENROUTER)) {
            assertEquals(FunctionCalling.Wire.OPENAI, FunctionCalling.wireOf(p))
        }
    }

    // ── tools fragment per wire ───────────────────────────────────────────────

    @Test fun `openai fragment carries the schema and auto tool_choice`() {
        val frag = FunctionCalling.toolsFragment(FunctionCalling.Wire.OPENAI, schema)
        assertEquals("auto", frag["tool_choice"]!!.jsonPrimitive.content)
        val fn = frag["tools"]!!.jsonArray.first().jsonObject["function"]!!.jsonObject
        assertEquals("send_sms", fn["name"]!!.jsonPrimitive.content)
    }

    @Test fun `anthropic fragment renames parameters to input_schema`() {
        val frag = FunctionCalling.toolsFragment(FunctionCalling.Wire.ANTHROPIC, schema)
        val tool = frag["tools"]!!.jsonArray.first().jsonObject
        assertEquals("send_sms", tool["name"]!!.jsonPrimitive.content)
        assertTrue(tool.containsKey("input_schema"))
        assertEquals("auto", frag["tool_choice"]!!.jsonObject["type"]!!.jsonPrimitive.content)
    }

    @Test fun `gemini fragment nests functionDeclarations`() {
        val frag = FunctionCalling.toolsFragment(FunctionCalling.Wire.GEMINI, schema)
        val decls = frag["tools"]!!.jsonArray.first().jsonObject["functionDeclarations"]!!.jsonArray
        assertEquals("send_sms", decls.first().jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test fun `empty schema yields an empty fragment`() {
        val frag = FunctionCalling.toolsFragment(FunctionCalling.Wire.OPENAI, buildJsonArray { })
        assertTrue(frag.isEmpty())
    }

    // ── response parsing ──────────────────────────────────────────────────────

    @Test fun `parses an openai text-only reply`() {
        val raw = obj("""{"choices":[{"message":{"role":"assistant","content":"hello"}}]}""")
        val parts = FunctionCalling.parseAssistant(FunctionCalling.Wire.OPENAI, raw)
        assertEquals(listOf(ContentPart.Text("hello")), parts)
    }

    @Test fun `parses an openai native tool call`() {
        val raw = obj(
            """{"choices":[{"message":{"content":null,"tool_calls":[
               {"id":"call_9","type":"function","function":{"name":"send_sms","arguments":"{\"to\":\"mom\"}"}}]}}]}""",
        )
        val parts = FunctionCalling.parseAssistant(FunctionCalling.Wire.OPENAI, raw)
        assertEquals(1, parts.size)
        val tc = parts.first() as ContentPart.ToolCall
        assertEquals("call_9", tc.id)
        assertEquals("send_sms", tc.name)
        assertEquals("mom", obj(tc.argsJson)["to"]!!.jsonPrimitive.content)
    }

    @Test fun `parses openai text and tool call together`() {
        val raw = obj(
            """{"choices":[{"message":{"content":"on it","tool_calls":[
               {"id":"c1","function":{"name":"flashlight","arguments":"{}"}}]}}]}""",
        )
        val parts = FunctionCalling.parseAssistant(FunctionCalling.Wire.OPENAI, raw)
        assertEquals(ContentPart.Text("on it"), parts[0])
        assertEquals("flashlight", (parts[1] as ContentPart.ToolCall).name)
    }

    @Test fun `parses an anthropic tool_use block`() {
        val raw = obj(
            """{"content":[{"type":"text","text":"sure"},
               {"type":"tool_use","id":"toolu_1","name":"call","input":{"who":"ali"}}]}""",
        )
        val parts = FunctionCalling.parseAssistant(FunctionCalling.Wire.ANTHROPIC, raw)
        assertEquals(ContentPart.Text("sure"), parts[0])
        val tc = parts[1] as ContentPart.ToolCall
        assertEquals("toolu_1", tc.id)
        assertEquals("ali", obj(tc.argsJson)["who"]!!.jsonPrimitive.content)
    }

    @Test fun `parses a gemini functionCall`() {
        val raw = obj(
            """{"candidates":[{"content":{"parts":[
               {"text":"ok"},{"functionCall":{"name":"open_app","args":{"app":"maps"}}}]}}]}""",
        )
        val parts = FunctionCalling.parseAssistant(FunctionCalling.Wire.GEMINI, raw)
        assertEquals(ContentPart.Text("ok"), parts[0])
        val tc = parts[1] as ContentPart.ToolCall
        assertEquals("open_app", tc.name)
        assertEquals("maps", obj(tc.argsJson)["app"]!!.jsonPrimitive.content)
    }

    @Test fun `parsing never throws on a malformed response`() {
        assertTrue(FunctionCalling.parseAssistant(FunctionCalling.Wire.OPENAI, obj("""{"oops":true}""")).isEmpty())
        assertTrue(FunctionCalling.parseAssistant(FunctionCalling.Wire.ANTHROPIC, obj("{}")).isEmpty())
    }

    // ── request body building ─────────────────────────────────────────────────

    private val convo = listOf(
        VisionMessage.text(Role.SYSTEM, "You are Vision"),
        VisionMessage.text(Role.USER, "text mom hi"),
        VisionMessage(
            Role.ASSISTANT,
            listOf(ContentPart.ToolCall("c1", "send_sms", """{"to":"mom"}""")),
        ),
        VisionMessage(
            Role.TOOL,
            listOf(ContentPart.ToolResult("c1", "send_sms", listOf(ContentPart.Text("sent")))),
        ),
    )

    @Test fun `openai body maps roles, tool calls and tool results`() {
        val body = FunctionCalling.requestBody(FunctionCalling.Wire.OPENAI, "gpt-4o-mini", convo)
        val msgs = body["messages"]!!.jsonArray
        assertEquals("system", msgs[0].jsonObject["role"]!!.jsonPrimitive.content)
        assertEquals("assistant", msgs[2].jsonObject["role"]!!.jsonPrimitive.content)
        val call = msgs[2].jsonObject["tool_calls"]!!.jsonArray.first().jsonObject
        assertEquals("send_sms", call["function"]!!.jsonObject["name"]!!.jsonPrimitive.content)
        val toolMsg = msgs[3].jsonObject
        assertEquals("tool", toolMsg["role"]!!.jsonPrimitive.content)
        assertEquals("c1", toolMsg["tool_call_id"]!!.jsonPrimitive.content)
        assertEquals("sent", toolMsg["content"]!!.jsonPrimitive.content)
    }

    @Test fun `anthropic body hoists system and encodes tool_use plus tool_result`() {
        val body = FunctionCalling.requestBody(FunctionCalling.Wire.ANTHROPIC, "claude", convo)
        assertEquals("You are Vision", body["system"]!!.jsonPrimitive.content)
        val msgs = body["messages"]!!.jsonArray
        // no SYSTEM message in the array (hoisted), so: user, assistant(tool_use), user(tool_result)
        val assistant = msgs[1].jsonObject["content"]!!.jsonArray.first().jsonObject
        assertEquals("tool_use", assistant["type"]!!.jsonPrimitive.content)
        val result = msgs[2].jsonObject["content"]!!.jsonArray.first().jsonObject
        assertEquals("tool_result", result["type"]!!.jsonPrimitive.content)
        assertEquals("c1", result["tool_use_id"]!!.jsonPrimitive.content)
    }

    @Test fun `gemini body uses system_instruction and functionCall plus functionResponse`() {
        val body = FunctionCalling.requestBody(FunctionCalling.Wire.GEMINI, "gemini", convo)
        assertTrue(body.containsKey("system_instruction"))
        val contents = body["contents"]!!.jsonArray
        val model = contents[1].jsonObject
        assertEquals("model", model["role"]!!.jsonPrimitive.content)
        assertEquals(
            "send_sms",
            model["parts"]!!.jsonArray.first().jsonObject["functionCall"]!!.jsonObject["name"]!!.jsonPrimitive.content,
        )
        val resp = contents[2].jsonObject["parts"]!!.jsonArray.first().jsonObject["functionResponse"]!!.jsonObject
        assertEquals("send_sms", resp["name"]!!.jsonPrimitive.content)
    }

    @Test fun `request body merges the tools fragment when provided`() {
        val frag = FunctionCalling.toolsFragment(FunctionCalling.Wire.OPENAI, schema)
        val body = FunctionCalling.requestBody(FunctionCalling.Wire.OPENAI, "gpt-4o-mini", convo, frag)
        assertEquals("auto", body["tool_choice"]!!.jsonPrimitive.content)
        assertTrue(body.containsKey("tools"))
    }

    @Test fun `request body omits tools when none provided`() {
        val body = FunctionCalling.requestBody(FunctionCalling.Wire.OPENAI, "gpt-4o-mini", convo)
        assertNull(body["tools"])
        assertNull(body["tool_choice"])
    }
}
