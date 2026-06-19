package com.kianirani.jarvis.data.launcher

/**
 * DS-L5 — usage-based ranking. Pure scoring so the drawer's FREQUENT row and the RECENT
 * ordering reflect both **how often** and **how recently** an app is used, instead of the
 * raw launch count (which ages badly: an app spammed once last month outranks today's
 * daily driver forever). `score = count × recencyWeight`, where the recency weight decays
 * exponentially with a half-life. No Android deps → unit-tested; the ViewModels feed it
 * (count, lastUsedMillis) tuples from the shared `vision_app_usage` store.
 */
data class UsageStat(val count: Int, val lastUsedMillis: Long)

object AppUsageRanker {
    /** Half-life of the recency weight, in days: an app unused this long counts for half. */
    const val HALF_LIFE_DAYS: Double = 7.0

    private const val DAY_MS: Double = 24 * 60 * 60 * 1000.0

    /**
     * Relevance score for one app — higher surfaces sooner. Frequency times an exponential
     * recency decay; a never-used app (count ≤ 0) scores 0. Times are epoch millis; a
     * [lastUsedMillis] in the future (clock skew) is clamped to "just now" (weight 1).
     */
    fun score(stat: UsageStat, now: Long): Double {
        if (stat.count <= 0) return 0.0
        val ageDays = (now - stat.lastUsedMillis).coerceAtLeast(0L) / DAY_MS
        val recency = Math.pow(0.5, ageDays / HALF_LIFE_DAYS)
        return stat.count * recency
    }

    /**
     * Rank [items] most-relevant first by [score]. The sort is stable, so apps with equal
     * scores keep their input order (callers pass a deterministic input order).
     */
    fun <T> rank(items: List<T>, now: Long, statOf: (T) -> UsageStat): List<T> =
        items.sortedByDescending { score(statOf(it), now) }
}
