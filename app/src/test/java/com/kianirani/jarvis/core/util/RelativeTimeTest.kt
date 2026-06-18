package com.kianirani.jarvis.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_000_000_000_000L
    private fun ago(ms: Long) = RelativeTime.ago(now - ms, now)

    private val sec = 1000L
    private val min = 60 * sec
    private val hour = 60 * min
    private val day = 24 * hour

    @Test fun `under 45 seconds is just now`() {
        assertEquals("just now", ago(0))
        assertEquals("just now", ago(44 * sec))
    }

    @Test fun `seconds past 45 round up to one minute`() {
        assertEquals("1m ago", ago(50 * sec))
    }

    @Test fun `minutes`() {
        assertEquals("5m ago", ago(5 * min))
        assertEquals("59m ago", ago(59 * min))
    }

    @Test fun `hours`() {
        assertEquals("1h ago", ago(hour))
        assertEquals("23h ago", ago(23 * hour))
    }

    @Test fun `days then weeks then years`() {
        assertEquals("3d ago", ago(3 * day))
        assertEquals("2w ago", ago(14 * day))
        assertEquals("1y ago", ago(400 * day))
    }

    @Test fun `future timestamp clamps to just now`() {
        assertEquals("just now", RelativeTime.ago(now + 5 * min, now))
    }
}
