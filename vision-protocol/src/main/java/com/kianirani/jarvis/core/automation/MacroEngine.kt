package com.kianirani.jarvis.core.automation

import kotlinx.serialization.Serializable

/**
 * DS-W4 (Automation mini-panel) — user-recorded macros: a named, ordered sequence of actions
 * the user can replay with one tap ("every morning: wifi off, DND on, open Calendar"). This is
 * the pure model + validation + replay-planning; the device layer records real actions and
 * executes the planned steps through the tool layer (with the SAFE trust gate). Serializable so
 * macros persist and sync. Pure → JVM-tested.
 */
@Serializable
data class MacroStep(val action: String, val argsJson: String = "{}")

@Serializable
data class Macro(val id: String, val name: String, val steps: List<MacroStep> = emptyList(), val enabled: Boolean = true)

object MacroEngine {

    fun record(id: String, name: String, steps: List<MacroStep>): Macro = Macro(id, name, steps)

    /** Append a step, returning a new macro (immutable). */
    fun append(macro: Macro, step: MacroStep): Macro = macro.copy(steps = macro.steps + step)

    /** Validation errors (empty = valid): name required, at least one step, no blank actions. */
    fun validate(macro: Macro): List<String> = buildList {
        if (macro.name.isBlank()) add("name is required")
        if (macro.steps.isEmpty()) add("macro has no steps")
        macro.steps.forEachIndexed { i, s -> if (s.action.isBlank()) add("step ${i + 1} has a blank action") }
    }

    /**
     * The ordered steps to replay, or empty when the macro is disabled or invalid (a disabled/
     * broken macro is a no-op, never a partial run).
     */
    fun plan(macro: Macro): List<MacroStep> =
        if (!macro.enabled || validate(macro).isNotEmpty()) emptyList() else macro.steps
}
