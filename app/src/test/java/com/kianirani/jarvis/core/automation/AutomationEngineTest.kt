package com.kianirani.jarvis.core.automation

import com.kianirani.jarvis.core.event.EventKind
import com.kianirani.jarvis.core.event.VisionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CF5 automation acceptance: event rules fire on matching kind + condition, schedule rules fire
 * when due, disabled rules never fire, and actions flatten in order. Pure.
 */
class AutomationEngineTest {

    private val dayStart = 1_700_000_000_000L
    private val hour = 3_600_000L

    @Test fun `event rule fires on matching kind`() {
        val rule = AutomationRule("r1", AutomationTrigger.OnEvent(EventKind.APP_OPENED), listOf("greet"))
        val fired = AutomationEngine.firedByEvent(listOf(rule), VisionEvent.AppOpened("com.maps"))
        assertEquals(listOf("r1"), fired.map { it.id })
    }

    @Test fun `event rule respects its condition`() {
        val rule = AutomationRule(
            "r1", AutomationTrigger.OnEvent(EventKind.APP_OPENED), listOf("x"),
            condition = { (it as? VisionEvent.AppOpened)?.packageName == "com.bank" },
        )
        assertTrue(AutomationEngine.firedByEvent(listOf(rule), VisionEvent.AppOpened("com.maps")).isEmpty())
        assertEquals(1, AutomationEngine.firedByEvent(listOf(rule), VisionEvent.AppOpened("com.bank")).size)
    }

    @Test fun `event rule does not fire on a different kind`() {
        val rule = AutomationRule("r1", AutomationTrigger.OnEvent(EventKind.WAKE_WORD), listOf("x"))
        assertTrue(AutomationEngine.firedByEvent(listOf(rule), VisionEvent.AppOpened("a")).isEmpty())
    }

    @Test fun `schedule rule fires when due`() {
        val rule = AutomationRule("r1", AutomationTrigger.OnSchedule(Schedule.DailyAt(16 * 60)), listOf("remind"))
        val before = AutomationEngine.firedBySchedule(listOf(rule), Clock(dayStart + 15 * hour, dayStart))
        val after = AutomationEngine.firedBySchedule(listOf(rule), Clock(dayStart + 16 * hour, dayStart))
        assertTrue(before.isEmpty())
        assertEquals(listOf("r1"), after.map { it.id })
    }

    @Test fun `disabled rules never fire`() {
        val ev = AutomationRule("e", AutomationTrigger.OnEvent(EventKind.WAKE_WORD), listOf("x"), enabled = false)
        val sc = AutomationRule("s", AutomationTrigger.OnSchedule(Schedule.DailyAt(0)), listOf("y"), enabled = false)
        assertTrue(AutomationEngine.firedByEvent(listOf(ev), VisionEvent.WakeWord).isEmpty())
        assertTrue(AutomationEngine.firedBySchedule(listOf(sc), Clock(dayStart + hour, dayStart)).isEmpty())
    }

    @Test fun `actions flatten in rule order`() {
        val rules = listOf(
            AutomationRule("a", AutomationTrigger.OnEvent(EventKind.CUSTOM), listOf("one", "two")),
            AutomationRule("b", AutomationTrigger.OnEvent(EventKind.CUSTOM), listOf("three")),
        )
        assertEquals(listOf("one", "two", "three"), AutomationEngine.actions(rules))
    }
}
