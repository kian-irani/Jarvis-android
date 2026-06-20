package com.kianirani.jarvis.core.monitor

/**
 * MON2 — the pure ranking core for usage monitoring. Turns raw per-app usage samples (total
 * foreground time + last-used time, from `UsageStatsManager`) into the "most used" and "recent"
 * orderings the Timeline / adaptive-home surfaces consume. Keeping the ranking pure makes it
 * deterministic and JVM-tested; querying `UsageStatsManager` is the device half (`UsageMonitor`).
 */
data class AppUsage(val pkg: String, val totalTimeMs: Long, val lastUsedMs: Long)

object UsageAggregator {

    /** Apps by total foreground time, most first; zero-time apps dropped. Ties by package. */
    fun mostUsed(usages: List<AppUsage>, limit: Int = 10): List<AppUsage> =
        usages.filter { it.totalTimeMs > 0 }
            .sortedWith(compareByDescending<AppUsage> { it.totalTimeMs }.thenBy { it.pkg })
            .take(limit.coerceAtLeast(0))

    /** Apps by last-used time, most recent first. Never-used (lastUsed ≤ 0) dropped. */
    fun recent(usages: List<AppUsage>, limit: Int = 10): List<AppUsage> =
        usages.filter { it.lastUsedMs > 0 }
            .sortedWith(compareByDescending<AppUsage> { it.lastUsedMs }.thenBy { it.pkg })
            .take(limit.coerceAtLeast(0))

    /** Total foreground time across all apps (for an at-a-glance "screen time"). */
    fun totalScreenTimeMs(usages: List<AppUsage>): Long = usages.sumOf { it.totalTimeMs.coerceAtLeast(0) }
}
