package com.kianirani.jarvis.core.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MON1 acceptance: the timeline records newest-first, caps capacity, and queries by type, time
 * range, source, and text. Pure, no Room, no device.
 */
class TimelineTest {

    private fun ev(id: String, type: ActivityType, at: Long, title: String, source: String = "", detail: String = "") =
        TimelineEvent(id, type, at, title, source, detail)

    @Test fun `events are newest first`() {
        val t = Timeline()
        t.record(ev("1", ActivityType.APP_OPEN, 10, "Maps"))
        t.record(ev("2", ActivityType.CALL, 20, "Mom"))
        assertEquals(listOf("2", "1"), t.all().map { it.id })
    }

    @Test fun `capacity drops the oldest`() {
        val t = Timeline(capacity = 2)
        t.record(ev("1", ActivityType.APP_OPEN, 1, "a"))
        t.record(ev("2", ActivityType.APP_OPEN, 2, "b"))
        t.record(ev("3", ActivityType.APP_OPEN, 3, "c"))
        assertEquals(listOf("3", "2"), t.all().map { it.id })
    }

    @Test fun `query filters by type`() {
        val t = Timeline()
        t.record(ev("1", ActivityType.APP_OPEN, 1, "Maps"))
        t.record(ev("2", ActivityType.MESSAGE, 2, "hi"))
        assertEquals(listOf("2"), t.query(type = ActivityType.MESSAGE).map { it.id })
    }

    @Test fun `query filters by time range inclusive`() {
        val t = Timeline()
        (1..5).forEach { t.record(ev("$it", ActivityType.NOTIFICATION, it.toLong(), "n$it")) }
        assertEquals(listOf("4", "3", "2"), t.query(since = 2, until = 4).map { it.id })
    }

    @Test fun `query filters by source and text`() {
        val t = Timeline()
        t.record(ev("1", ActivityType.MESSAGE, 1, "Hello from mom", source = "whatsapp", detail = "see you"))
        t.record(ev("2", ActivityType.MESSAGE, 2, "spam", source = "sms"))
        assertEquals(listOf("1"), t.query(source = "whatsapp").map { it.id })
        assertEquals(listOf("1"), t.query(text = "MOM").map { it.id })
        assertEquals(listOf("1"), t.query(text = "see you").map { it.id }) // matches detail
    }

    @Test fun `recent and countByType summarize`() {
        val t = Timeline()
        t.record(ev("1", ActivityType.APP_OPEN, 1, "a"))
        t.record(ev("2", ActivityType.APP_OPEN, 2, "b"))
        t.record(ev("3", ActivityType.CALL, 3, "c"))
        assertEquals(listOf("3", "2"), t.recent(2).map { it.id })
        assertEquals(mapOf(ActivityType.APP_OPEN to 2, ActivityType.CALL to 1), t.countByType())
    }

    @Test fun `limit caps query output`() {
        val t = Timeline()
        (1..10).forEach { t.record(ev("$it", ActivityType.APP_OPEN, it.toLong(), "a$it")) }
        assertTrue(t.query(limit = 3).size == 3)
    }
}
