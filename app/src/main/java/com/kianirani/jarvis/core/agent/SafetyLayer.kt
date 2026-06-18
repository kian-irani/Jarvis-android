package com.kianirani.jarvis.core.agent

/**
 * SAFE (PRD §7.4 / Part 12) — the per-action trust gate. A pure policy that decides
 * whether a device action may run automatically or must get explicit user
 * confirmation, independent of which agent asked. Sensitive actions
 * (sms / call / email / delete / purchase / transfer) and unknown actions fail safe
 * → confirm; a curated safe-list (time / battery / open app …) runs automatically.
 * No Android deps → unit-tested. Later consumed by the VCF tool node (VCF-T3).
 *
 * Distinct from [com.kianirani.jarvis.data.agent.TrustLevel] (an *agent's* autonomy
 * ceiling): [ActionRisk] is the risk of one *action*.
 */
enum class ActionRisk { AUTO, CONFIRM, CRITICAL }

object SafetyLayer {
    /** Always require confirmation, whatever a tool or agent claims. */
    val ALWAYS_CRITICAL = setOf(
        "send_sms", "send_message", "call", "send_email", "delete", "purchase", "transfer", "pay",
    )

    /** Side-effect-light reads / launches that never need confirmation. */
    val ALWAYS_AUTO = setOf(
        "get_time", "get_date", "get_battery", "get_weather", "open_app", "flashlight", "screenshot",
    )

    /**
     * Effective risk for an action by name, with an optional [declared] override from a
     * trusted tool spec. The always-critical list wins over everything; an explicit
     * CRITICAL escalates even a normally-auto action; unknown actions default to CONFIRM.
     */
    fun riskOf(action: String, declared: ActionRisk? = null): ActionRisk {
        val name = normalize(action)
        return when {
            name in ALWAYS_CRITICAL -> ActionRisk.CRITICAL
            declared == ActionRisk.CRITICAL -> ActionRisk.CRITICAL
            name in ALWAYS_AUTO -> ActionRisk.AUTO
            else -> declared ?: ActionRisk.CONFIRM
        }
    }

    /** True when the action must be confirmed by the user before it runs. */
    fun requiresConfirmation(action: String, declared: ActionRisk? = null): Boolean =
        riskOf(action, declared) != ActionRisk.AUTO

    private fun normalize(action: String): String = action.trim().lowercase()

    /** Anti-hallucination rules to fold into the system prompt (SAFE, Part 12). */
    const val ANTI_HALLUCINATION_RULES: String =
        "Never claim you performed an action you did not actually run. If a tool failed " +
            "or was not called, say so plainly. Do not invent data, results, or confirmations. " +
            "When unsure whether an action succeeded, ask instead of assuming."
}
