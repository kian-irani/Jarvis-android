package com.kianirani.jarvis.data.agent

/**
 * Layer 2 — Agents. The four conceptual agents Vision orchestrates. This is the
 * real data model behind the home "Active Agents" panel and the Agents
 * management screen (v12, 2026-06-14). Status is derived from real device
 * capabilities by [AgentRegistry]; trust + enabled are user-controlled and
 * persisted.
 */
enum class AgentId(val display: String, val glyph: String) {
    RESEARCH("Research", "🔍"),
    AUTOMATION("Automation", "⚡"),
    DEVELOPER("Developer", "{ }"),
    DEVICE("Device", "▣"),
}

/** Per-agent autonomy ceiling (spec: Read / Suggest / Auto / Critical). */
enum class TrustLevel(val label: String, val desc: String) {
    READ("Read", "observe only — never acts"),
    SUGGEST("Suggest", "proposes actions, you confirm"),
    AUTO("Auto", "acts autonomously on routine tasks"),
    CRITICAL("Critical", "full autonomy incl. sensitive actions"),
}

enum class AgentStatus { ACTIVE, IDLE, WORKING, OFF }

data class AgentState(
    val id: AgentId,
    val status: AgentStatus,
    val trust: TrustLevel,
    val enabled: Boolean,
)

/** A recent action attributed to an agent (mock history for now). */
data class AgentAction(val agent: AgentId, val time: String, val text: String)
