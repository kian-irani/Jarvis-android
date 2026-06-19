package com.kianirani.jarvis.core.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AGT-DELEGATE acceptance: items group by sender across apps, newest-first, busiest sender
 * first, and a sender filter scopes the report to one person across every source. Pure.
 */
class ReportAggregatorTest {

    private fun item(source: String, sender: String, text: String, at: Long) =
        CollectedItem(source, sender, text, at)

    private val items = listOf(
        item("whatsapp", "mom", "are you coming?", 100),
        item("sms", "mom", "call me", 300),
        item("telegram", "boss", "report due", 200),
        item("whatsapp", "mom", "ok", 50),
    )

    @Test fun `groups by sender across sources, newest first`() {
        val report = ReportAggregator.aggregate(items)
        val mom = report.groups.first { it.sender == "mom" }
        assertEquals(3, mom.count)
        assertEquals(setOf("whatsapp", "sms"), mom.sources)
        assertEquals(listOf(300L, 100L, 50L), mom.items.map { it.atMillis }) // newest first
        assertEquals(300L, mom.latestMillis)
    }

    @Test fun `busiest sender is first`() {
        val report = ReportAggregator.aggregate(items)
        assertEquals("mom", report.groups.first().sender) // 3 > 1
    }

    @Test fun `sender filter scopes to one person across all apps`() {
        val report = ReportAggregator.aggregate(items, filterSender = "MOM")
        assertEquals(1, report.groups.size)
        assertEquals(3, report.totalItems)
        assertEquals(setOf("whatsapp", "sms"), report.sources)
    }

    @Test fun `totals and sources reflect the whole set`() {
        val report = ReportAggregator.aggregate(items)
        assertEquals(4, report.totalItems)
        assertEquals(setOf("whatsapp", "sms", "telegram"), report.sources)
    }

    @Test fun `empty input yields an empty report`() {
        val report = ReportAggregator.aggregate(emptyList())
        assertEquals(0, report.totalItems)
        assertTrue(report.groups.isEmpty())
    }

    @Test fun `count ties break by sender name`() {
        val tie = listOf(
            item("a", "zoe", "x", 1),
            item("b", "amy", "y", 2),
        )
        assertEquals(listOf("amy", "zoe"), ReportAggregator.aggregate(tie).groups.map { it.sender })
    }
}
