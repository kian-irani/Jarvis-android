package com.kianirani.jarvis.core.agent

/**
 * AGSK — routes a goal to the right specialist (PRD §, "AgentEngine یک هدف را به ایجنت/اسکیلِ
 * مناسب مسیریابی می‌کند — مثل routerِ مدل، اما برای قابلیت‌ها"). Given the user's goal and the
 * available [AgentRole]s, it picks the agent whose role/goal/backstory and tool names best
 * match the request — so "call mom" goes to the Device agent and "research the news" to the
 * Research agent, instead of one generic loop doing everything.
 *
 * Pure token-overlap scoring (no model call) → deterministic and JVM-tested. The agent it
 * returns is then driven by the real ReAct loop (VCF-A1/A2) with that role's tool allowlist.
 */
object SkillRouter {

    private val WORD = Regex("[a-z0-9]+")

    private val STOPWORDS = setOf(
        "the", "a", "an", "to", "for", "of", "and", "or", "my", "me", "i", "please",
        "can", "you", "with", "on", "in", "is", "it", "this", "that",
    )

    private fun tokens(s: String): Set<String> =
        WORD.findAll(s.lowercase()).map { it.value }.filter { it.length > 1 && it !in STOPWORDS }.toSet()

    /**
     * How well [agent] fits [goal]: the number of meaningful goal tokens that appear in the
     * agent's role, goal, backstory, or tool names (tool-name segments split on `_`). Higher =
     * better fit; 0 = no signal.
     */
    fun score(goal: String, agent: AgentRole): Int {
        val goalTokens = tokens(goal)
        if (goalTokens.isEmpty()) return 0
        val agentVocab = buildSet {
            addAll(tokens(agent.role))
            addAll(tokens(agent.goal))
            addAll(tokens(agent.backstory))
            agent.tools.forEach { addAll(it.lowercase().split("_", "-", ".").filter { p -> p.length > 1 }) }
        }
        return goalTokens.count { it in agentVocab }
    }

    /**
     * The best-matching agent for [goal], or null if none has any signal (the caller then
     * falls back to the default/general agent). Ties break by agent id for determinism.
     */
    fun route(goal: String, agents: List<AgentRole>): AgentRole? =
        agents.map { it to score(goal, it) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<AgentRole, Int>> { it.second }.thenBy { it.first.id })
            .firstOrNull()
            ?.first
}
