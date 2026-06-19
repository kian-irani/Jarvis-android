package com.kianirani.jarvis.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-L2 acceptance: the predictor surfaces apps whose past launches cluster around the
 * current hour, still respects overall recency/frequency, excludes pinned apps, and wraps
 * around midnight. Pure, no device.
 */
class AdaptiveHomePredictorTest {

    private val now = 1_000_000_000_000L
    private val dayMs = 24L * 60 * 60 * 1000

    private fun launch(id: String, hour: Int, daysAgo: Long = 0) =
        AppLaunch(id, hour, now - daysAgo * dayMs)

    @Test fun `an app used at this hour outranks an equally-frequent app used at another hour`() {
        val launches = listOf(
            launch("morning", 8), launch("morning", 8),
            launch("evening", 20), launch("evening", 20),
        )
        val ranked = AdaptiveHomePredictor.suggest(launches, now, currentHour = 8)
        assertEquals("morning", ranked.first())
    }

    @Test fun `a frequently used app still appears even when no launch matches the hour`() {
        val launches = listOf(
            launch("daily", 9), launch("daily", 9), launch("daily", 9),
            launch("rare", 14),
        )
        // 3am matches neither app's hour → only the recency base remains; the frequent app wins.
        val ranked = AdaptiveHomePredictor.suggest(launches, now, currentHour = 3)
        assertEquals(listOf("daily", "rare"), ranked)
    }

    @Test fun `excluded (pinned) apps are not suggested`() {
        val launches = listOf(launch("pinned", 10), launch("free", 10))
        val ranked = AdaptiveHomePredictor.suggest(launches, now, currentHour = 10, exclude = setOf("pinned"))
        assertEquals(listOf("free"), ranked)
    }

    @Test fun `time-of-day proximity wraps around midnight`() {
        val launches = listOf(launch("night", 23), launch("noon", 12))
        // 01:00 is 2h from 23:00 (within window) but 11h from 12:00 → night is boosted.
        val ranked = AdaptiveHomePredictor.suggest(launches, now, currentHour = 1)
        assertEquals("night", ranked.first())
    }

    @Test fun `recency decay lets a fresh app overtake a stale but matching one`() {
        val launches = listOf(
            launch("stale", 10, daysAgo = 60),
            launch("fresh", 10, daysAgo = 0),
        )
        val scores = AdaptiveHomePredictor.score(launches, now, currentHour = 10)
        assertTrue(scores.getValue("fresh") > scores.getValue("stale"))
    }

    @Test fun `topN bounds the suggestion row and never-used apps are absent`() {
        val launches = (1..6).map { launch("app$it", 10) }
        val ranked = AdaptiveHomePredictor.suggest(launches, now, currentHour = 10, topN = 4)
        assertEquals(4, ranked.size)
        assertFalse(AdaptiveHomePredictor.score(launches, now, 10).containsKey("never-launched"))
    }
}
