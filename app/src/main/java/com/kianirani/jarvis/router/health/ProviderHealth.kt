package com.kianirani.jarvis.router.health

/**
 * VB9.1 — per-provider health for the UI (PRD §, "health dots per-provider"). The pure mapping
 * from a provider's key/availability state (how many keys it has, how many are cooling down after
 * 401/429, whether the circuit is open) to a [HealthStatus] the AI-providers row renders as a
 * coloured dot **plus a label** (meaning never rides on colour alone). Pure → JVM-tested; reading
 * the live `TokenPool`/`AvailabilityGraph` into [ProviderHealthInput] is the wiring half.
 */
enum class HealthStatus(val label: String) {
    HEALTHY("Online"),
    DEGRADED("Limited"),
    DOWN("Offline"),
    UNCONFIGURED("No key"),
}

data class ProviderHealthInput(
    val totalKeys: Int,
    val coolingKeys: Int,
    /** Circuit breaker tripped (all recent calls failing). */
    val circuitOpen: Boolean = false,
)

object ProviderHealth {

    /**
     * Status from the input: no keys → UNCONFIGURED; circuit open or every key cooling → DOWN;
     * some keys cooling → DEGRADED; all keys healthy → HEALTHY.
     */
    fun status(input: ProviderHealthInput): HealthStatus = when {
        input.totalKeys <= 0 -> HealthStatus.UNCONFIGURED
        input.circuitOpen || input.coolingKeys >= input.totalKeys -> HealthStatus.DOWN
        input.coolingKeys > 0 -> HealthStatus.DEGRADED
        else -> HealthStatus.HEALTHY
    }

    /** "3/4 keys" style summary for the row subtitle. */
    fun keySummary(input: ProviderHealthInput): String {
        if (input.totalKeys <= 0) return "no keys"
        val healthy = (input.totalKeys - input.coolingKeys).coerceAtLeast(0)
        return "$healthy/${input.totalKeys} keys ready"
    }
}
