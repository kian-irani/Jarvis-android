package com.kianirani.jarvis.core.automation

import com.kianirani.jarvis.core.event.EventKind
import kotlinx.serialization.Serializable

/**
 * B2 — the AI Workflow Builder model (PRD §, "When→Then بدون کد"). A user-authored "when X then
 * do Y" workflow — the no-code layer over the [AutomationEngine]: a trigger (event kind or
 * schedule), an optional condition expression, and an ordered list of actions. This is the pure,
 * serializable model + validation + lowering to an [AutomationRule] the engine already runs. The
 * visual builder UI is the surface half; this keeps authoring deterministic and JVM-tested.
 */
@Serializable
sealed interface WorkflowTrigger {
    @Serializable
    data class OnEventKind(val kind: EventKind) : WorkflowTrigger

    @Serializable
    data class OnDailyAt(val minuteOfDay: Int) : WorkflowTrigger
}

@Serializable
data class Workflow(
    val id: String,
    val name: String,
    val trigger: WorkflowTrigger,
    val actions: List<String> = emptyList(),
    val enabled: Boolean = true,
    /** Optional plain condition key the engine evaluates (e.g. "battery<20"); blank = always. */
    val condition: String = "",
)

object WorkflowBuilder {

    /** Validation errors (empty = valid): name + at least one action; valid daily minute. */
    fun validate(w: Workflow): List<String> = buildList {
        if (w.name.isBlank()) add("name is required")
        if (w.actions.isEmpty()) add("workflow has no actions")
        val t = w.trigger
        if (t is WorkflowTrigger.OnDailyAt && t.minuteOfDay !in 0..1439) add("invalid time of day")
    }

    fun isValid(w: Workflow): Boolean = validate(w).isEmpty()

    /**
     * Lower a valid workflow to an [AutomationRule] the [AutomationEngine] runs. Event triggers
     * map to `OnEvent`; daily triggers map to a `Schedule.DailyAt`. Returns null for an invalid
     * or disabled workflow (no rule to register).
     */
    fun toRule(w: Workflow): AutomationRule? {
        if (!w.enabled || !isValid(w)) return null
        val trigger = when (val t = w.trigger) {
            is WorkflowTrigger.OnEventKind -> AutomationTrigger.OnEvent(t.kind)
            is WorkflowTrigger.OnDailyAt -> AutomationTrigger.OnSchedule(Schedule.DailyAt(t.minuteOfDay))
        }
        return AutomationRule(id = w.id, trigger = trigger, actions = w.actions, enabled = true)
    }
}
