package com.kianirani.jarvis.core.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Behavioral baseline acceptance: mean/stddev, z-score, anomaly threshold + min-samples. Pure. */
class AnomalyDetectorTest {

    @Test fun `baseline computes mean and stddev`() {
        val b = AnomalyDetector.baseline(listOf(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0))
        assertEquals(5.0, b.mean, 1e-9)
        assertEquals(2.0, b.stdDev, 1e-9)
        assertEquals(8, b.count)
    }

    @Test fun `z-score measures deviation`() {
        val b = Baseline(mean = 10.0, stdDev = 2.0, count = 10)
        assertEquals(2.0, AnomalyDetector.zScore(14.0, b), 1e-9)
        assertEquals(0.0, AnomalyDetector.zScore(99.0, Baseline(0.0, 0.0, 10)), 1e-9) // zero stddev
    }

    @Test fun `a far sample is anomalous, a near one is not`() {
        val b = Baseline(mean = 10.0, stdDev = 2.0, count = 30)
        assertTrue(AnomalyDetector.isAnomaly(20.0, b)) // z=5 > 3
        assertFalse(AnomalyDetector.isAnomaly(13.0, b)) // z=1.5
    }

    @Test fun `not enough history is never an anomaly`() {
        val b = Baseline(mean = 10.0, stdDev = 2.0, count = 3)
        assertFalse(AnomalyDetector.isAnomaly(100.0, b, minSamples = 5))
    }
}
