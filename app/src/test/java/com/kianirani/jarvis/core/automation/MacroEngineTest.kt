package com.kianirani.jarvis.core.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-W4 macro acceptance: macros record/append steps, validate name/steps/actions, and plan
 * replays only valid+enabled macros. Pure.
 */
class MacroEngineTest {

    private val morning = MacroEngine.record(
        "m1", "Morning",
        listOf(MacroStep("wifi_off"), MacroStep("dnd_on"), MacroStep("open_app", """{"pkg":"calendar"}""")),
    )

    @Test fun `a valid enabled macro plans all its steps in order`() {
        assertEquals(listOf("wifi_off", "dnd_on", "open_app"), MacroEngine.plan(morning).map { it.action })
    }

    @Test fun `append adds a step immutably`() {
        val extended = MacroEngine.append(morning, MacroStep("brightness_low"))
        assertEquals(4, extended.steps.size)
        assertEquals(3, morning.steps.size) // original unchanged
    }

    @Test fun `validation flags a blank name and empty steps`() {
        assertTrue(MacroEngine.validate(Macro("x", "", emptyList())).contains("name is required"))
        assertTrue(MacroEngine.validate(Macro("x", "Named", emptyList())).contains("macro has no steps"))
    }

    @Test fun `validation flags a blank action`() {
        val errs = MacroEngine.validate(Macro("x", "Named", listOf(MacroStep("ok"), MacroStep("  "))))
        assertTrue(errs.any { it.contains("step 2") })
    }

    @Test fun `a disabled macro plans nothing`() {
        assertTrue(MacroEngine.plan(morning.copy(enabled = false)).isEmpty())
    }

    @Test fun `an invalid macro plans nothing`() {
        assertTrue(MacroEngine.plan(Macro("x", "", emptyList())).isEmpty())
    }
}
