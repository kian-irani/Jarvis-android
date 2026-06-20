package com.kianirani.jarvis.core.timeline

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A5 acceptance: timestamps map to the right bucket relative to the day start, and grouping is
 * ordered Today→Older with newest-first within and no empty buckets. Pure.
 */
class TimelineBucketsTest {

    private val day = 24L * 60 * 60 * 1000
    private val dayStart = 1_700_000_000_000L
    private val now = dayStart + 10 * 60 * 60 * 1000 // 10:00 today

    private fun ev(id: String, at: Long) = TimelineEvent(id, ActivityType.APP_OPEN, at, id)

    @Test fun `bucketOf classifies relative to the day start`() {
        assertEquals(TimeBucket.TODAY, TimelineBuckets.bucketOf(dayStart + 5 * 60 * 1000, now, dayStart))
        assertEquals(TimeBucket.YESTERDAY, TimelineBuckets.bucketOf(dayStart - 2 * 60 * 60 * 1000, now, dayStart))
        assertEquals(TimeBucket.THIS_WEEK, TimelineBuckets.bucketOf(now - 3 * day, now, dayStart))
        assertEquals(TimeBucket.THIS_MONTH, TimelineBuckets.bucketOf(now - 20 * day, now, dayStart))
        assertEquals(TimeBucket.OLDER, TimelineBuckets.bucketOf(now - 60 * day, now, dayStart))
    }

    @Test fun `a future timestamp counts as today`() {
        assertEquals(TimeBucket.TODAY, TimelineBuckets.bucketOf(now + day, now, dayStart))
    }

    @Test fun `group orders buckets and keeps newest first within`() {
        val events = listOf(
            ev("today1", dayStart + 1 * 60 * 60 * 1000),
            ev("today2", dayStart + 8 * 60 * 60 * 1000),
            ev("yest", dayStart - 3 * 60 * 60 * 1000),
            ev("old", now - 50 * day),
        )
        val grouped = TimelineBuckets.group(events, now, dayStart)
        assertEquals(listOf(TimeBucket.TODAY, TimeBucket.YESTERDAY, TimeBucket.OLDER), grouped.keys.toList())
        assertEquals(listOf("today2", "today1"), grouped.getValue(TimeBucket.TODAY).map { it.id }) // newest first
    }

    @Test fun `empty buckets are omitted`() {
        val grouped = TimelineBuckets.group(listOf(ev("t", dayStart + 60_000)), now, dayStart)
        assertEquals(listOf(TimeBucket.TODAY), grouped.keys.toList())
    }
}
