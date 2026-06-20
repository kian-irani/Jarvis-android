package com.kianirani.jarvis.core.automation

import com.kianirani.jarvis.core.event.EventKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** B2 acceptance: workflows validate, lower to automation rules, and skip invalid/disabled. Pure. */
class WorkflowTest {

    private val wf = Workflow(
        id = "w1", name = "Morning brief",
        trigger = WorkflowTrigger.OnDailyAt(8 * 60),
        actions = listOf("read calendar", "summarize"),
    )

    @Test fun `a valid workflow lowers to a schedule rule`() {
        val rule = WorkflowBuilder.toRule(wf)!!
        assertEquals("w1", rule.id)
        assertEquals(listOf("read calendar", "summarize"), rule.actions)
        assertTrue(rule.trigger is AutomationTrigger.OnSchedule)
    }

    @Test fun `an event workflow lowers to an event rule`() {
        val rule = WorkflowBuilder.toRule(wf.copy(trigger = WorkflowTrigger.OnEventKind(EventKind.APP_OPENED)))!!
        assertTrue(rule.trigger is AutomationTrigger.OnEvent)
    }

    @Test fun `validation flags missing name actions and bad time`() {
        assertTrue(WorkflowBuilder.validate(wf.copy(name = "")).contains("name is required"))
        assertTrue(WorkflowBuilder.validate(wf.copy(actions = emptyList())).contains("workflow has no actions"))
        assertTrue(WorkflowBuilder.validate(wf.copy(trigger = WorkflowTrigger.OnDailyAt(2000))).contains("invalid time of day"))
    }

    @Test fun `invalid or disabled workflow yields no rule`() {
        assertNull(WorkflowBuilder.toRule(wf.copy(actions = emptyList())))
        assertNull(WorkflowBuilder.toRule(wf.copy(enabled = false)))
    }
}
