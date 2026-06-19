package com.kianirani.jarvis.core.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AGT-SCHED acceptance: the parser extracts absolute, relative, and repeating times, strips
 * the time phrase + leading filler from the action, handles am/pm and Persian digits, and
 * returns null when there is no time. Pure, no clock.
 */
class ScheduledCommandParserTest {

    @Test fun `absolute time with a multi-clause action`() {
        val c = ScheduledCommandParser.parse("today at 16:00 call Mr X and say don't forget the meeting")!!
        assertEquals(ParsedSchedule.AtClock(16 * 60), c.schedule)
        assertEquals("call Mr X and say don't forget the meeting", c.action)
    }

    @Test fun `relative minutes`() {
        val c = ScheduledCommandParser.parse("in 30 minutes remind me to drink water")!!
        assertEquals(ParsedSchedule.After(30 * 60_000L), c.schedule)
        assertTrue(c.action.contains("drink water"))
    }

    @Test fun `relative hours with no action`() {
        val c = ScheduledCommandParser.parse("in 2 hours")!!
        assertEquals(ParsedSchedule.After(2 * 3_600_000L), c.schedule)
        assertEquals("", c.action)
    }

    @Test fun `repeating interval`() {
        val c = ScheduledCommandParser.parse("every 2 hours check the server")!!
        assertEquals(ParsedSchedule.EveryInterval(2 * 3_600_000L), c.schedule)
        assertEquals("check the server", c.action)
    }

    @Test fun `am pm conversion`() {
        assertEquals(ParsedSchedule.AtClock(9 * 60), ScheduledCommandParser.parse("at 9am call mom")!!.schedule)
        assertEquals(ParsedSchedule.AtClock(21 * 60), ScheduledCommandParser.parse("at 9 pm lock the door")!!.schedule)
        assertEquals(ParsedSchedule.AtClock(0), ScheduledCommandParser.parse("at 12am reboot")!!.schedule)
        assertEquals(ParsedSchedule.AtClock(12 * 60), ScheduledCommandParser.parse("at 12pm lunch reminder")!!.schedule)
    }

    @Test fun `persian digits normalize with an english keyword`() {
        val c = ScheduledCommandParser.parse("at ۱۶:۳۰ call dad")!!
        assertEquals(ParsedSchedule.AtClock(16 * 60 + 30), c.schedule)
        assertEquals("call dad", c.action)
    }

    @Test fun `short unit aliases`() {
        assertEquals(ParsedSchedule.After(90 * 60_000L), ScheduledCommandParser.parse("in 90 m ping")!!.schedule)
        assertEquals(ParsedSchedule.After(3 * 3_600_000L), ScheduledCommandParser.parse("in 3 h backup")!!.schedule)
    }

    @Test fun `no time phrase yields null`() {
        assertNull(ScheduledCommandParser.parse("call mom"))
    }

    @Test fun `an out-of-range clock is rejected`() {
        assertNull(ScheduledCommandParser.parse("at 25:00 do something"))
    }
}
