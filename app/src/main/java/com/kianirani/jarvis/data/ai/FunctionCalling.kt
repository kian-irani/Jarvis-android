package com.kianirani.jarvis.data.ai

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * VCF-T2 — provider-agnostic **native function-calling** wire codec (PRD §7.2).
 *
 * Pure of Android and network so the whole protocol is JVM-unit-tested. Three jobs:
 *  1. [toolsFragment] — turn the canonical OpenAI-shaped function schema (from
 *     [com.kianirani.jarvis.core.tools.ToolRegistry.functionSchema]) into each
 *     provider's request shape (`tools`/`tool_choice` · Anthropic `input_schema` ·
 *     Gemini `functionDeclarations`).
 *  2. [requestBody] — render the running [VisionMessage] conversation (text, the
 *     assistant's prior tool calls, and our tool results) into the provider body,
 *     optionally merging the tools fragment.
 *  3. [parseAssistant] — read a provider response back into framework [ContentPart]s:
 *     assistant [ContentPart.Text] plus every structured [ContentPart.ToolCall].
 *
 * Three wire families cover every provider: OpenAI-compatible (OpenAI/xAI/Groq/
 * OpenRouter), Anthropic, and Gemini. A model that can't do function-calling is given
 * a `null` schema upstream and never reaches here → the text `TOOL_PROTOCOL` fallback
 * (CrewAI's "native FC with text fallback" pattern). Image parts are out of scope here
 * (VCF-M1 owns per-provider image encoding); only text/tool parts are wired.
 */
object FunctionCalling {

    enum class Wire { OPENAI, ANTHROPIC, GEMINI }

    /** Which wire family a provider speaks. */
    fun wireOf(p: AiProvider): Wire = when (p) {
        AiProvider.ANTHROPIC -> Wire.ANTHROPIC
        AiProvider.GEMINI -> Wire.GEMINI
        AiProvider.OPENAI, AiProvider.XAI, AiProvider.GROQ, AiProvider.OPENROUTER -> Wire.OPENAI
    }

    private val emptyObjectSchema: JsonObject
        get() = buildJsonObject { put("type", "object"); putJsonObject("properties") {} }

    /**
     * Request-body keys that enable native function-calling for [wire], built from the
     * canonical OpenAI function array [openAiFns] — each entry
     * `{"type":"function","function":{name,description,parameters}}`. Merge the result
     * into the provider request body. Returns an empty object for an empty schema.
     */
    fun toolsFragment(wire: Wire, openAiFns: JsonArray): JsonObject {
        if (openAiFns.isEmpty()) return JsonObject(emptyMap())
        return when (wire) {
            Wire.OPENAI -> buildJsonObject {
                put("tools", openAiFns)
                put("tool_choice", "auto")
            }
            Wire.ANTHROPIC -> buildJsonObject {
                putJsonArray("tools") {
                    openAiFns.forEach { fn ->
                        val f = fn.jsonObject["function"]!!.jsonObject
                        add(
                            buildJsonObject {
                                put("name", f["name"]!!.jsonPrimitive.content)
                                f["description"]?.let { put("description", it.jsonPrimitive.content) }
                                put("input_schema", f["parameters"]?.jsonObject ?: emptyObjectSchema)
                            },
                        )
                    }
                }
                putJsonObject("tool_choice") { put("type", "auto") }
            }
            Wire.GEMINI -> buildJsonObject {
                putJsonArray("tools") {
                    add(
                        buildJsonObject {
                            putJsonArray("functionDeclarations") {
                                openAiFns.forEach { fn ->
                                    val f = fn.jsonObject["function"]!!.jsonObject
                                    add(
                                        buildJsonObject {
                                            put("name", f["name"]!!.jsonPrimitive.content)
                                            f["description"]?.let { put("description", it.jsonPrimitive.content) }
                                            put("parameters", f["parameters"]?.jsonObject ?: emptyObjectSchema)
                                        },
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    /**
     * Build the provider request body for [model] from the conversation [messages],
     * merging [tools] (from [toolsFragment]) when non-null/non-empty. SYSTEM messages
     * route to each provider's system channel; assistant tool calls and our tool
     * results are encoded so a multi-turn ReAct loop keeps the model in context.
     */
    fun requestBody(
        wire: Wire,
        model: String,
        messages: List<VisionMessage>,
        tools: JsonObject? = null,
        maxTokens: Int = 1024,
    ): JsonObject = when (wire) {
        Wire.OPENAI -> openAiBody(model, messages, tools)
        Wire.ANTHROPIC -> anthropicBody(model, messages, tools, maxTokens)
        Wire.GEMINI -> geminiBody(messages, tools)
    }

    private fun openAiBody(model: String, messages: List<VisionMessage>, tools: JsonObject?): JsonObject =
        buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                messages.forEach { m ->
                    when (m.role) {
                        Role.SYSTEM -> add(textMsg("system", m.text()))
                        Role.USER -> add(textMsg("user", m.text()))
                        Role.TOOL -> m.content.filterIsInstance<ContentPart.ToolResult>().forEach { r ->
                            add(
                                buildJsonObject {
                                    put("role", "tool")
                                    put("tool_call_id", r.callId)
                                    put("content", resultText(r))
                                },
                            )
                        }
                        Role.ASSISTANT -> add(
                            buildJsonObject {
                                put("role", "assistant")
                                put("content", m.text())
                                val calls = m.toolCalls()
                                if (calls.isNotEmpty()) {
                                    putJsonArray("tool_calls") {
                                        calls.forEach { c ->
                                            add(
                                                buildJsonObject {
                                                    put("id", c.id)
                                                    put("type", "function")
                                                    putJsonObject("function") {
                                                        put("name", c.name)
                                                        put("arguments", c.argsJson)
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    }
                }
            }
            mergeInto(tools)
        }

    private fun anthropicBody(model: String, messages: List<VisionMessage>, tools: JsonObject?, maxTokens: Int): JsonObject =
        buildJsonObject {
            put("model", model)
            put("max_tokens", maxTokens)
            val system = messages.filter { it.role == Role.SYSTEM }.joinToString("\n") { it.text() }
            if (system.isNotBlank()) put("system", system)
            putJsonArray("messages") {
                messages.forEach { m ->
                    when (m.role) {
                        Role.SYSTEM -> Unit // hoisted to the system field
                        Role.USER -> add(anthropicMsg("user") { add(textBlock(m.text())) })
                        Role.TOOL -> add(
                            anthropicMsg("user") {
                                m.content.filterIsInstance<ContentPart.ToolResult>().forEach { r ->
                                    add(
                                        buildJsonObject {
                                            put("type", "tool_result")
                                            put("tool_use_id", r.callId)
                                            put("content", resultText(r))
                                            if (r.isError) put("is_error", true)
                                        },
                                    )
                                }
                            },
                        )
                        Role.ASSISTANT -> add(
                            anthropicMsg("assistant") {
                                val t = m.text()
                                if (t.isNotBlank()) add(textBlock(t))
                                m.toolCalls().forEach { c ->
                                    add(
                                        buildJsonObject {
                                            put("type", "tool_use")
                                            put("id", c.id)
                                            put("name", c.name)
                                            put("input", parseObjOrEmpty(c.argsJson))
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            }
            mergeInto(tools)
        }

    private fun geminiBody(messages: List<VisionMessage>, tools: JsonObject?): JsonObject =
        buildJsonObject {
            val system = messages.filter { it.role == Role.SYSTEM }.joinToString("\n") { it.text() }
            if (system.isNotBlank()) {
                putJsonObject("system_instruction") { putJsonArray("parts") { add(buildJsonObject { put("text", system) }) } }
            }
            putJsonArray("contents") {
                messages.forEach { m ->
                    when (m.role) {
                        Role.SYSTEM -> Unit
                        Role.USER -> add(geminiContent("user") { add(buildJsonObject { put("text", m.text()) }) })
                        Role.TOOL -> add(
                            geminiContent("user") {
                                m.content.filterIsInstance<ContentPart.ToolResult>().forEach { r ->
                                    add(
                                        buildJsonObject {
                                            putJsonObject("functionResponse") {
                                                put("name", r.name)
                                                putJsonObject("response") { put("result", resultText(r)) }
                                            }
                                        },
                                    )
                                }
                            },
                        )
                        Role.ASSISTANT -> add(
                            geminiContent("model") {
                                val t = m.text()
                                if (t.isNotBlank()) add(buildJsonObject { put("text", t) })
                                m.toolCalls().forEach { c ->
                                    add(
                                        buildJsonObject {
                                            putJsonObject("functionCall") {
                                                put("name", c.name)
                                                put("args", parseObjOrEmpty(c.argsJson))
                                            }
                                        },
                                    )
                                }
                            },
                        )
                    }
                }
            }
            mergeInto(tools)
        }

    /**
     * Parse a provider response [raw] for [wire] into framework content parts: assistant
     * text as [ContentPart.Text] and every native function call as a structured
     * [ContentPart.ToolCall]. Defensive — missing/odd shapes yield a partial or empty
     * list, never an exception (a model that "talks" without FC still returns its text).
     */
    fun parseAssistant(wire: Wire, raw: JsonObject): List<ContentPart> = runCatching {
        when (wire) {
            Wire.OPENAI -> parseOpenAi(raw)
            Wire.ANTHROPIC -> parseAnthropic(raw)
            Wire.GEMINI -> parseGemini(raw)
        }
    }.getOrDefault(emptyList())

    private fun parseOpenAi(raw: JsonObject): List<ContentPart> {
        val msg = raw["choices"]?.jsonArray?.firstOrNull()?.jsonObject?.get("message")?.jsonObject
            ?: return emptyList()
        val parts = mutableListOf<ContentPart>()
        (msg["content"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?.takeIf { it.isNotBlank() }
            ?.let { parts += ContentPart.Text(it) }
        msg["tool_calls"]?.jsonArray?.forEachIndexed { i, tc ->
            val o = tc.jsonObject
            val fn = o["function"]?.jsonObject ?: return@forEachIndexed
            val name = fn["name"]?.jsonPrimitive?.content ?: return@forEachIndexed
            val args = fn["arguments"]?.let { if (it is JsonPrimitive) it.content else it.toString() } ?: "{}"
            val id = o["id"]?.jsonPrimitive?.content ?: "call_$i"
            parts += ContentPart.ToolCall(id, name, args)
        }
        return parts
    }

    private fun parseAnthropic(raw: JsonObject): List<ContentPart> {
        val blocks = raw["content"]?.jsonArray ?: return emptyList()
        val parts = mutableListOf<ContentPart>()
        blocks.forEachIndexed { i, b ->
            val o = b.jsonObject
            when (o["type"]?.jsonPrimitive?.content) {
                "text" -> o["text"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                    ?.let { parts += ContentPart.Text(it) }
                "tool_use" -> {
                    val name = o["name"]?.jsonPrimitive?.content ?: return@forEachIndexed
                    val id = o["id"]?.jsonPrimitive?.content ?: "call_$i"
                    val args = o["input"]?.jsonObject?.toString() ?: "{}"
                    parts += ContentPart.ToolCall(id, name, args)
                }
            }
        }
        return parts
    }

    private fun parseGemini(raw: JsonObject): List<ContentPart> {
        val parts0 = raw["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonObject?.get("parts")?.jsonArray ?: return emptyList()
        val parts = mutableListOf<ContentPart>()
        parts0.forEachIndexed { i, p ->
            val o = p.jsonObject
            o["text"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                ?.let { parts += ContentPart.Text(it) }
            o["functionCall"]?.jsonObject?.let { fc ->
                val name = fc["name"]?.jsonPrimitive?.content ?: return@forEachIndexed
                val args = fc["args"]?.jsonObject?.toString() ?: "{}"
                parts += ContentPart.ToolCall("call_$i", name, args)
            }
        }
        return parts
    }

    // ── small builders ──────────────────────────────────────────────────────────

    private fun textMsg(role: String, text: String) = buildJsonObject {
        put("role", role); put("content", text)
    }

    private fun textBlock(text: String) = buildJsonObject { put("type", "text"); put("text", text) }

    private fun anthropicMsg(role: String, content: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit) =
        buildJsonObject { put("role", role); putJsonArray("content", content) }

    private fun geminiContent(role: String, parts: kotlinx.serialization.json.JsonArrayBuilder.() -> Unit) =
        buildJsonObject { put("role", role); putJsonArray("parts", parts) }

    /** Flatten a tool result's content parts to the plain text the model reads back. */
    private fun resultText(r: ContentPart.ToolResult): String =
        r.content.filterIsInstance<ContentPart.Text>().joinToString(" ") { it.text }

    private fun parseObjOrEmpty(jsonText: String): JsonObject =
        runCatching { lenient.parseToJsonElement(jsonText).jsonObject }.getOrDefault(JsonObject(emptyMap()))

    private val lenient = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }

    private fun kotlinx.serialization.json.JsonObjectBuilder.mergeInto(fragment: JsonObject?) {
        fragment?.forEach { (k, v) -> put(k, v) }
    }
}
