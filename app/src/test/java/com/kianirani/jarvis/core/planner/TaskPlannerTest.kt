package com.kianirani.jarvis.core.planner

import com.kianirani.jarvis.router.orchestrator.Intent
import com.kianirani.jarvis.router.orchestrator.IntentClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** CF3 — TaskPlanner: clause splitting, TOOL/MODEL tagging, EN/FA sequences. */
class TaskPlannerTest {

    private val planner = TaskPlanner(IntentClassifier())

    @Test
    fun `a single request is one model step`() {
        val plan = planner.plan("explain why the sky is blue in detail")
        assertEquals(1, plan.steps.size)
        assertFalse(plan.isMultiStep)
        assertEquals(StepKind.MODEL, plan.steps[0].kind)
        assertEquals(Intent.REASONING, plan.steps[0].intent)
    }

    @Test
    fun `a single device action is one tool step`() {
        val plan = planner.plan("turn on the flashlight")
        assertEquals(1, plan.steps.size)
        assertEquals(StepKind.TOOL, plan.steps[0].kind)
        assertEquals(Intent.ACTION, plan.steps[0].intent)
    }

    @Test
    fun `then splits a goal into ordered steps with correct kinds`() {
        val plan = planner.plan("turn on the flashlight then what time is it")
        assertTrue(plan.isMultiStep)
        assertEquals(listOf("turn on the flashlight", "what time is it"), plan.steps.map { it.instruction })
        assertEquals(listOf(StepKind.TOOL, StepKind.MODEL), plan.steps.map { it.kind })
        assertEquals(listOf(0, 1), plan.steps.map { it.index })
    }

    @Test
    fun `and then is treated as a single sequence connector`() {
        val plan = planner.plan("open settings and then turn off wifi")
        assertEquals(listOf("open settings", "turn off wifi"), plan.steps.map { it.instruction })
        assertTrue(plan.steps.all { it.kind == StepKind.TOOL })
    }

    @Test
    fun `persian sepas connector splits and tags a mixed plan`() {
        val plan = planner.plan("چراغ قوه را روشن کن سپس ساعت چنده")
        assertEquals(listOf("چراغ قوه را روشن کن", "ساعت چنده"), plan.steps.map { it.instruction })
        assertEquals(StepKind.TOOL, plan.steps[0].kind)   // action
        assertEquals(StepKind.MODEL, plan.steps[1].kind)  // quick lookup
    }

    @Test
    fun `punctuation connectors split clauses`() {
        val plan = planner.plan("explain recursion؛ give an example")
        assertEquals(2, plan.steps.size)
        assertTrue(plan.steps.all { it.kind == StepKind.MODEL })
    }

    @Test
    fun `a plain conjunction is not over-split`() {
        // No sequence connector → stays one step (avoids splitting "salt and pepper").
        val plan = planner.plan("add salt and pepper to the list")
        assertEquals(1, plan.steps.size)
        assertEquals("add salt and pepper to the list", plan.steps[0].instruction)
    }

    @Test
    fun `blank goal yields an empty plan`() {
        val plan = planner.plan("   ")
        assertTrue(plan.isEmpty)
        assertFalse(plan.isMultiStep)
    }
}
