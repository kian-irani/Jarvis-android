package com.kianirani.jarvis.core.launcher

import kotlin.math.abs
import kotlin.math.pow

/**
 * DS-L2 — adaptive home (PRD §, "پیشنهادِ پویای اپ بر اساسِ الگوی استفاده"). Predicts which
 * apps the user wants **right now** so the orb-centred home can surface a small, live
 * suggestion row.
 *
 * This is a higher-order signal than [AppUsageRanker] (DS-L5), which ranks globally by raw
 * frequency×recency. Here each launch also carries the **hour of day** it happened, so the
 * predictor learns time-of-day habits — a music app every morning commute, an email app at
 * 9am — and boosts apps whose past launches cluster around the current hour. An app you use
 * a lot still scores (the recency-weighted base is always there); the time match only tilts
 * the order, so the row never becomes empty just because nothing matches the clock exactly.
 *
 * Pure & deterministic (hours are supplied by the caller, decay is closed-form) → JVM-tested.
 * Reading real launch history (UsageStats) and rendering the row are the on-device half.
 */
data class AppLaunch(val appId: String, val hourOfDay: Int, val atMillis: Long)

object AdaptiveHomePredictor {

    /** Recency half-life in days: a launch this old contributes half as much. */
    const val HALF_LIFE_DAYS: Double = 7.0

    /** Hours on either side of "now" still counted as the same time-of-day habit. */
    const val HOUR_WINDOW: Int = 2

    /** How strongly a perfect time-of-day match boosts an app over its recency base. */
    const val TIME_BOOST: Double = 2.0

    private const val DAY_MS: Double = 24 * 60 * 60 * 1000.0

    /**
     * Circular distance between two clock hours (0..23), so 23:00 and 01:00 are 2 apart, not
     * 22. A launch within [HOUR_WINDOW] of [currentHour] earns a triangular proximity weight
     * in (0..1]; further away earns 0 (only the recency base remains).
     */
    private fun proximity(launchHour: Int, currentHour: Int): Double {
        val raw = abs(launchHour - currentHour)
        val dist = minOf(raw, 24 - raw)
        if (dist > HOUR_WINDOW) return 0.0
        return 1.0 - dist.toDouble() / (HOUR_WINDOW + 1)
    }

    private fun recency(launch: AppLaunch, now: Long): Double {
        val ageDays = (now - launch.atMillis).coerceAtLeast(0L) / DAY_MS
        return 0.5.pow(ageDays / HALF_LIFE_DAYS)
    }

    /**
     * Relevance score per app for [currentHour] at time [now]. Each launch contributes its
     * recency weight, amplified by `1 + TIME_BOOST × proximity` when it falls near the current
     * hour. Apps with no launches are absent (score treated as 0).
     */
    fun score(launches: List<AppLaunch>, now: Long, currentHour: Int): Map<String, Double> {
        val acc = HashMap<String, Double>()
        for (l in launches) {
            val contribution = recency(l, now) * (1.0 + TIME_BOOST * proximity(l.hourOfDay, currentHour))
            acc[l.appId] = (acc[l.appId] ?: 0.0) + contribution
        }
        return acc
    }

    /**
     * The [topN] app ids to suggest for [currentHour], most-relevant first. Apps in [exclude]
     * (e.g. already pinned to the dock) are dropped so the row only proposes things not already
     * one tap away. Ties break by app id for determinism.
     */
    fun suggest(
        launches: List<AppLaunch>,
        now: Long,
        currentHour: Int,
        topN: Int = 4,
        exclude: Set<String> = emptySet(),
    ): List<String> {
        if (topN <= 0) return emptyList()
        return score(launches, now, currentHour)
            .asSequence()
            .filter { it.key !in exclude && it.value > 0.0 }
            .sortedWith(compareByDescending<Map.Entry<String, Double>> { it.value }.thenBy { it.key })
            .map { it.key }
            .take(topN)
            .toList()
    }
}
