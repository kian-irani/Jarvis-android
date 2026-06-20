package com.kianirani.jarvis.core.monitor

import org.junit.Assert.assertEquals
import org.junit.Test

/** MON2 acceptance: most-used ranks by foreground time, recent by last-used, screen time sums. Pure. */
class UsageAggregatorTest {

    private val data = listOf(
        AppUsage("browser", totalTimeMs = 3_600_000, lastUsedMs = 100),
        AppUsage("music", totalTimeMs = 1_800_000, lastUsedMs = 500),
        AppUsage("idle", totalTimeMs = 0, lastUsedMs = 0),
        AppUsage("maps", totalTimeMs = 600_000, lastUsedMs = 900),
    )

    @Test fun `mostUsed ranks by foreground time, drops zero`() {
        assertEquals(listOf("browser", "music", "maps"), UsageAggregator.mostUsed(data).map { it.pkg })
    }

    @Test fun `recent ranks by last-used`() {
        assertEquals(listOf("maps", "music", "browser"), UsageAggregator.recent(data).map { it.pkg })
    }

    @Test fun `limit caps output`() {
        assertEquals(2, UsageAggregator.mostUsed(data, limit = 2).size)
    }

    @Test fun `total screen time sums foreground time`() {
        assertEquals(6_000_000L, UsageAggregator.totalScreenTimeMs(data))
    }
}
