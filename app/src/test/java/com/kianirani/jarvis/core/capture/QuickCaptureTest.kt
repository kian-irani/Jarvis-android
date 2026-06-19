package com.kianirani.jarvis.core.capture

import com.kianirani.jarvis.core.automation.ParsedSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-W4 acceptance: timed captures become reminders with a schedule, reminder-keyword captures
 * become scheduleless reminders, and plain text becomes a note. Pure.
 */
class QuickCaptureTest {

    @Test fun `a timed reminder captures schedule and cleaned body`() {
        val c = QuickCapture.classify("remind me at 6pm to call mom")
        assertTrue(c is Capture.Reminder)
        c as Capture.Reminder
        assertEquals(ParsedSchedule.AtClock(18 * 60), c.schedule)
        assertEquals("call mom", c.text)
    }

    @Test fun `a relative reminder captures the delay`() {
        val c = QuickCapture.classify("in 30 minutes drink water") as Capture.Reminder
        assertEquals(ParsedSchedule.After(30 * 60_000L), c.schedule)
        assertTrue(c.text.contains("drink water"))
    }

    @Test fun `a reminder keyword without a time is a scheduleless reminder`() {
        val c = QuickCapture.classify("remind me to buy milk")
        assertTrue(c is Capture.Reminder)
        c as Capture.Reminder
        assertNull(c.schedule)
        assertEquals("buy milk", c.text)
    }

    @Test fun `plain text becomes a note`() {
        val c = QuickCapture.classify("the wifi password is on the router")
        assertTrue(c is Capture.Note)
        assertEquals("the wifi password is on the router", (c as Capture.Note).text)
    }

    @Test fun `persian reminder keyword routes to a reminder`() {
        val c = QuickCapture.classify("یادم بنداز که به مادر زنگ بزنم")
        assertTrue(c is Capture.Reminder)
    }

    @Test fun `blank input is an empty note`() {
        assertEquals(Capture.Note(""), QuickCapture.classify("   "))
    }
}
