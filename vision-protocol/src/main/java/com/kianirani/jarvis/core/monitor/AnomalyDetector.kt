package com.kianirani.jarvis.core.monitor

import kotlin.math.sqrt

/**
 * Behavioral baseline (PRD §, "Behavioral baseline — تشخیص ناهنجاری"). The pure statistics that
 * learn what's "normal" for a signal (daily screen time, calls/day, a metric) and flag a sample
 * that deviates too far — Vision uses it to surface "unusual" days/events without a server. A
 * rolling mean+stddev baseline with a z-score threshold; pure and deterministic → JVM-tested.
 */
data class Baseline(val mean: Double, val stdDev: Double, val count: Int)

object AnomalyDetector {

    /** Default: a sample more than 3σ from the mean is anomalous. */
    const val DEFAULT_THRESHOLD = 3.0

    /** Mean/stddev (population) of [samples]; empty → zero baseline. */
    fun baseline(samples: List<Double>): Baseline {
        if (samples.isEmpty()) return Baseline(0.0, 0.0, 0)
        val mean = samples.average()
        val variance = samples.sumOf { (it - mean) * (it - mean) } / samples.size
        return Baseline(mean, sqrt(variance), samples.size)
    }

    /** How many standard deviations [value] is from [baseline] (0 when stddev is 0). */
    fun zScore(value: Double, baseline: Baseline): Double =
        if (baseline.stdDev == 0.0) 0.0 else (value - baseline.mean) / baseline.stdDev

    /**
     * Is [value] anomalous vs [baseline]? Needs at least [minSamples] history (else not enough to
     * judge → false). |z| beyond [threshold] = anomaly.
     */
    fun isAnomaly(value: Double, baseline: Baseline, threshold: Double = DEFAULT_THRESHOLD, minSamples: Int = 5): Boolean {
        if (baseline.count < minSamples || baseline.stdDev == 0.0) return false
        return kotlin.math.abs(zScore(value, baseline)) > threshold
    }
}
