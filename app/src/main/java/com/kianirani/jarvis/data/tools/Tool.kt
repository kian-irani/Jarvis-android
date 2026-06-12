package com.kianirani.jarvis.data.tools

/**
 * Phase 5 (Agentic Reasoning Core) — the tool abstraction. Each Tool matches a
 * natural-language request (EN/FA) and performs one device action, returning a
 * spoken-friendly reply. The CommandInterpreter dispatches to the registry
 * before falling through to the brain/cloud, so device actions stay instant and
 * fully on-device.
 */
interface Tool {
    /** Stable id for logging/registry. */
    val id: String

    /** True if this tool should handle the (lowercased, trimmed) message. */
    fun matches(message: String): Boolean

    /** Execute and return a short reply. */
    fun run(message: String): ToolResult
}

data class ToolResult(val reply: String)
