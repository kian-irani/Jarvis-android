package com.kianirani.jarvis.core.agent

import kotlinx.serialization.Serializable

/**
 * VCF-X1 — a role definition for a member of a multi-agent team (PRD §9, CrewAI
 * role/goal/backstory). It turns a bare agent name into behaviour: a system prompt and
 * a tool allowlist (names into the ToolRegistry). Pure/serializable → unit-tested; the
 * concrete roles for Vision's named agents live in `data/agent/AgentRoles`.
 */
@Serializable
data class AgentRole(
    val id: String,
    val role: String,
    val goal: String,
    val backstory: String,
    val tools: Set<String> = emptySet(),
) {
    /** The system prompt that gives this agent its persona and objective. */
    fun systemPrompt(): String = "You are a $role. Your goal: $goal.\n$backstory"
}
