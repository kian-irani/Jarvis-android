package com.kianirani.jarvis.data.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** DS-L5 — frequency × recency-decay scoring is pure and unit-tested. */
class AppUsageRankerTest {

    private val now = 1_000_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private fun daysAgo(d: Long) = now - d * day

    @Test fun `a never-used app scores zero`() {
        assertEquals(0.0, AppUsageRanker.score(UsageStat(0, daysAgo(0)), now), 0.0)
    }

    @Test fun `a just-used app scores its full count`() {
        // recency weight is 1.0 at age 0, so score == count
        assertEquals(5.0, AppUsageRanker.score(UsageStat(5, now), now), 1e-9)
    }

    @Test fun `one half-life of disuse halves the score`() {
        val s = AppUsageRanker.score(UsageStat(8, daysAgo(AppUsageRanker.HALF_LIFE_DAYS.toLong())), now)
        assertEquals(4.0, s, 1e-9)
    }

    @Test fun `a future timestamp is clamped, never boosting the score above count`() {
        val future = now + 10 * day
        assertEquals(3.0, AppUsageRanker.score(UsageStat(3, future), now), 1e-9)
    }

    @Test fun `a recent daily driver outranks a long-ago spammed app`() {
        data class App(val id: String, val stat: UsageStat)
        val dailyDriver = App("maps", UsageStat(count = 6, lastUsedMillis = daysAgo(0)))
        val oldSpam = App("setup", UsageStat(count = 40, lastUsedMillis = daysAgo(60)))
        val ranked = AppUsageRanker.rank(listOf(oldSpam, dailyDriver), now) { it.stat }
        assertEquals("maps", ranked.first().id)
    }

    @Test fun `equal scores keep input order (stable)`() {
        data class App(val id: String, val stat: UsageStat)
        val a = App("a", UsageStat(3, now))
        val b = App("b", UsageStat(3, now))
        val ranked = AppUsageRanker.rank(listOf(a, b), now) { it.stat }
        assertEquals(listOf("a", "b"), ranked.map { it.id })
    }

    @Test fun `higher recent count outranks lower recent count`() {
        data class App(val id: String, val stat: UsageStat)
        val more = App("more", UsageStat(10, now))
        val less = App("less", UsageStat(2, now))
        assertTrue(
            AppUsageRanker.score(more.stat, now) > AppUsageRanker.score(less.stat, now),
        )
    }
}
