package com.kianirani.jarvis.brain.score

/**
 * Device metrics snapshot used for Brain election (ROADMAP §Brain Score Algorithm, ADR-006).
 */
data class DeviceMetrics(
    val ramFreeGb: Double,
    val cpuCores: Int,
    val isVps: Boolean = false,
    val batteryPercent: Int = 100,
    val networkMbps: Double = 0.0,
    val isOnBattery: Boolean = false,
    val thermalThrottling: Boolean = false,
)

/**
 * Brain Score Calculator — real-time score for auto-election of the primary Brain.
 *
 * Score = RAM_free_GB×20 + CPU_cores×10 + (VPS? +50) + battery%×0.3 + mbps×0.1
 *         − (on battery? 20) − (thermal throttling? 30)
 *
 * Highest score wins; user can manually override; on disconnect the next
 * highest-scoring node takes over (auto-failover).
 */
object BrainScoreCalculator {

    fun score(m: DeviceMetrics): Int {
        var s = 0.0
        s += m.ramFreeGb.coerceAtLeast(0.0) * 20
        s += m.cpuCores.coerceAtLeast(0) * 10
        if (m.isVps) s += 50
        s += m.batteryPercent.coerceIn(0, 100) * 0.3
        s += m.networkMbps.coerceAtLeast(0.0) * 0.1
        if (m.isOnBattery) s -= 20
        if (m.thermalThrottling) s -= 30
        return s.toInt().coerceAtLeast(0)
    }

    /**
     * Election: pick the id of the highest-scoring node. [manualOverride] wins if
     * present in [candidates]. Ties break deterministically by id ordering.
     */
    fun elect(candidates: Map<String, DeviceMetrics>, manualOverride: String? = null): String? {
        if (manualOverride != null && manualOverride in candidates) return manualOverride
        return candidates.entries
            .sortedWith(compareByDescending<Map.Entry<String, DeviceMetrics>> { score(it.value) }.thenBy { it.key })
            .firstOrNull()?.key
    }

    /** Auto-failover: re-elect after removing the failed node. */
    fun failover(candidates: Map<String, DeviceMetrics>, failedId: String): String? =
        elect(candidates - failedId)
}
