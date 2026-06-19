package com.kianirani.jarvis.core.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CF5 acceptance: the scheduler core fires once-tasks once, intervals on cadence without
 * catch-up storms, and daily tasks at their local minute exactly once per day — and reports
 * the correct next-fire time. Pure, no clock, no device.
 */
class ScheduleEvaluatorTest {

    private val dayStart = 1_700_000_000_000L // arbitrary local midnight
    private val minute = 60_000L
    private val hour = 60 * minute

    private fun clock(offsetFromMidnight: Long) = Clock(dayStart + offsetFromMidnight, dayStart)

    @Test fun `Once fires when its time arrives and not before`() {
        val task = ScheduledTask("t", Schedule.Once(dayStart + 10 * hour))
        assertFalse(ScheduleEvaluator.isDue(task, clock(9 * hour)))
        assertTrue(ScheduleEvaluator.isDue(task, clock(10 * hour)))
    }

    @Test fun `Once does not re-fire after it has run`() {
        val s = Schedule.Once(dayStart + 10 * hour)
        val ran = ScheduledTask("t", s, lastRunMillis = dayStart + 10 * hour)
        assertFalse(ScheduleEvaluator.isDue(ran, clock(11 * hour)))
        assertNull(ScheduleEvaluator.nextFire(ran, clock(11 * hour)))
    }

    @Test fun `Interval becomes due after one period from its anchor`() {
        val task = ScheduledTask("t", Schedule.Interval(everyMillis = 2 * hour, anchorMillis = dayStart))
        assertFalse(ScheduleEvaluator.isDue(task, clock(1 * hour)))
        assertTrue(ScheduleEvaluator.isDue(task, clock(2 * hour)))
    }

    @Test fun `Interval next fire skips missed slots instead of stacking catch-ups`() {
        val task = ScheduledTask("t", Schedule.Interval(everyMillis = 1 * hour, anchorMillis = dayStart))
        // now is 5.5h in; last run never → next slot strictly after now is 6h
        val next = ScheduleEvaluator.nextFire(task, clock(5 * hour + 30 * minute))
        assertEquals(dayStart + 6 * hour, next)
    }

    @Test fun `Interval counts from last run when present`() {
        val task = ScheduledTask(
            "t",
            Schedule.Interval(everyMillis = 1 * hour, anchorMillis = dayStart),
            lastRunMillis = dayStart + 3 * hour,
        )
        assertFalse(ScheduleEvaluator.isDue(task, clock(3 * hour + 30 * minute)))
        assertTrue(ScheduleEvaluator.isDue(task, clock(4 * hour)))
        assertEquals(dayStart + 4 * hour, ScheduleEvaluator.nextFire(task, clock(3 * hour + 30 * minute)))
    }

    @Test fun `DailyAt fires at its minute and only once per day`() {
        val task = ScheduledTask("t", Schedule.DailyAt(minuteOfDay = 16 * 60)) // 16:00
        assertFalse(ScheduleEvaluator.isDue(task, clock(15 * hour)))
        assertTrue(ScheduleEvaluator.isDue(task, clock(16 * hour)))
        val ran = task.copy(lastRunMillis = dayStart + 16 * hour)
        assertFalse(ScheduleEvaluator.isDue(ran, clock(17 * hour)))
        // next fire rolls to tomorrow once it has run today
        assertEquals(dayStart + 16 * hour + 24 * hour, ScheduleEvaluator.nextFire(ran, clock(17 * hour)))
    }

    @Test fun `disabled tasks are never due but keep their natural next fire`() {
        val task = ScheduledTask("t", Schedule.DailyAt(9 * 60), enabled = false)
        assertFalse(ScheduleEvaluator.isDue(task, clock(10 * hour)))
        assertTrue(ScheduleEvaluator.due(listOf(task), clock(10 * hour)).isEmpty())
        assertEquals(dayStart + 9 * hour + 24 * hour, ScheduleEvaluator.nextFire(task, clock(10 * hour)))
    }

    @Test fun `due returns only the firing tasks in input order`() {
        val a = ScheduledTask("a", Schedule.Once(dayStart + 1 * hour))
        val b = ScheduledTask("b", Schedule.Once(dayStart + 20 * hour))
        val c = ScheduledTask("c", Schedule.DailyAt(0))
        assertEquals(listOf("a", "c"), ScheduleEvaluator.due(listOf(a, b, c), clock(2 * hour)).map { it.id })
    }
}
