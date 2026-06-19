package com.kianirani.jarvis.core.tools

import com.kianirani.jarvis.core.agent.ActionRisk
import com.kianirani.jarvis.core.graph.ContentPart
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * VCF-MEM1 — long-term memory exposed as tools the agent can call mid-run (PRD §11). The
 * actual store is injected as a suspend lambda (real impl bridges to MemoryEngine), so the
 * tools are unit-tested without Room/embeddings. AUTO trust — remembering/recalling is safe.
 */
class RememberTool(private val onRemember: suspend (content: String) -> Boolean) : VisionTool {
    override val spec = ToolSpec(
        name = "remember",
        description = "Save a durable fact, preference, or decision about the user to long-term memory.",
        parameters = stringParam("content", "The fact to remember."),
        trust = ActionRisk.AUTO,
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult {
        val content = args["content"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        if (content.isBlank()) return errorResult("remember", "missing 'content'")
        val ok = onRemember(content)
        return textResult("remember", if (ok) "Saved to memory." else "Couldn't save (memory not ready).", isError = !ok)
    }
}

class RecallTool(private val onRecall: suspend (query: String, topK: Int) -> List<String>) : VisionTool {
    override val spec = ToolSpec(
        name = "recall",
        description = "Search the user's long-term memory for relevant facts.",
        parameters = stringParam("query", "What to search memory for."),
        trust = ActionRisk.AUTO,
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult {
        val query = args["query"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        if (query.isBlank()) return errorResult("recall", "missing 'query'")
        val hits = onRecall(query, RECALL_TOP_K)
        val text = if (hits.isEmpty()) "No relevant memories." else hits.joinToString("\n") { "• $it" }
        return textResult("recall", text)
    }
}

private const val RECALL_TOP_K = 5

private fun stringParam(name: String, description: String): JsonObject = buildJsonObject {
    put("type", "object")
    putJsonObject("properties") {
        putJsonObject(name) {
            put("type", "string")
            put("description", description)
        }
    }
}

private fun textResult(name: String, text: String, isError: Boolean = false) =
    ContentPart.ToolResult("?", name, listOf(ContentPart.Text(text)), isError = isError)

private fun errorResult(name: String, message: String) = textResult(name, message, isError = true)
