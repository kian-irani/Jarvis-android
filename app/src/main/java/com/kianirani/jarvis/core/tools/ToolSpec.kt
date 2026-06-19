package com.kianirani.jarvis.core.tools

import com.kianirani.jarvis.core.agent.ActionRisk
import com.kianirani.jarvis.core.graph.ContentPart
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * VCF-T1 — the tool contract (PRD §7.1). A [VisionTool] advertises a [ToolSpec]
 * (name + description + JSON-Schema parameters + its [ActionRisk] trust) and executes
 * structured args into a [ContentPart.ToolResult]. Reuses the SAFE [ActionRisk]
 * vocabulary (v60) so the trust gate and the tool layer speak the same language.
 * Pure of Android → the contract + schema generation are unit-tested; the concrete
 * device tools under `data/tools` migrate onto this interface in a later, on-device pass.
 */
@Serializable
data class ToolSpec(
    val name: String,
    val description: String,
    val parameters: JsonObject = JsonObject(emptyMap()), // JSON Schema (draft-07 subset)
    val trust: ActionRisk = ActionRisk.AUTO,
    /**
     * True only for side-effect-free reads (e.g. recall, get_*). The [ToolNode] runs
     * read-only calls in a single step concurrently; anything that mutates state stays
     * sequential. Distinct from [trust]: `open_app` is AUTO (no confirmation) yet mutates,
     * so it is NOT read-only. Defaults false — unmarked tools keep running sequentially.
     */
    val readOnly: Boolean = false,
)

/** Execution environment for a tool call (expanded as device tools migrate). */
data class ToolContext(
    val sessionId: String = "",
    val preApproved: Boolean = false,
)

/** A callable capability. [execute] turns validated [args] into a result for the model. */
interface VisionTool {
    val spec: ToolSpec

    suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult
}

/** OpenAI-shaped function description for native function-calling (VCF-T2). */
fun ToolSpec.toOpenAiFunction(): JsonObject = buildJsonObject {
    put("type", "function")
    putJsonObject("function") {
        put("name", name)
        put("description", description)
        put("parameters", parameters)
    }
}
