package com.kianirani.jarvis.core.tools

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray

/**
 * VCF-T1 — a per-session sandbox allowlist (openclaw). `null` allows every tool; a set
 * restricts to those names (e.g. a low-trust group chat surface).
 */
class ToolAllowlist(private val allowed: Set<String>? = null) {
    fun permits(name: String): Boolean = allowed == null || name in allowed
}

/**
 * VCF-T1 — the tool registry (PRD §7.1). Resolves tools by name and produces the
 * model-facing function schema for native function-calling, filtered by the
 * [ToolAllowlist]. Returns null when the model can't do function-calling, signalling
 * the caller (VCF-T2) to fall back to the text TOOL_PROTOCOL + ToolCaller.
 */
class ToolRegistry(
    private val tools: List<VisionTool>,
    private val allowlist: ToolAllowlist = ToolAllowlist(),
) {
    /** Tool with this exact name, or null. */
    fun byName(name: String): VisionTool? = tools.firstOrNull { it.spec.name == name }

    /** Allowed tools only (what the model is permitted to call this session). */
    fun permitted(): List<VisionTool> = tools.filter { allowlist.permits(it.spec.name) }

    /** OpenAI/Gemini/Anthropic-shaped function schema, or null when the model lacks FC. */
    fun functionSchema(supportsFunctionCalling: Boolean): JsonArray? =
        if (!supportsFunctionCalling) {
            null
        } else {
            buildJsonArray { permitted().forEach { add(it.spec.toOpenAiFunction()) } }
        }
}
