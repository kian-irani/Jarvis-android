package com.kianirani.jarvis.core.automation

import com.kianirani.jarvis.core.event.EventKind
import com.kianirani.jarvis.core.event.VisionEvent

/**
 * CF5 / DS-BG2 — the automation rule engine (PRD §10): the layer that ties VCF-R2 **event**
 * triggers and the CF5 **schedule** triggers into one "when X, do Y" model. A rule fires when
 * its trigger matches and its condition holds; the engine returns the rules/actions to run.
 * Pure (it composes [ScheduleEvaluator] and event matching) → JVM-tested; running each action
 * (a prompt, a macro, a workflow) through the AgentEngine is the on-device half.
 */
sealed interface AutomationTrigger {
    /** Fire on a Vision event of this kind (e.g. APP_OPENED, WAKE_WORD). */
    data class OnEvent(val kind: EventKind) : AutomationTrigger

    /** Fire on a time schedule (delegates to [ScheduleEvaluator]). */
    data class OnSchedule(val schedule: Schedule) : AutomationTrigger
}

data class AutomationRule(
    val id: String,
    val trigger: AutomationTrigger,
    val actions: List<String>,
    val enabled: Boolean = true,
    val lastRunMillis: Long? = null,
    /** Extra guard for event rules (e.g. only for a specific app). Ignored for schedule rules. */
    val condition: (VisionEvent) -> Boolean = { true },
)

object AutomationEngine {

    /** Enabled [AutomationTrigger.OnEvent] rules whose kind matches [event] and condition holds. */
    fun firedByEvent(rules: List<AutomationRule>, event: VisionEvent): List<AutomationRule> =
        rules.filter { rule ->
            rule.enabled &&
                rule.trigger is AutomationTrigger.OnEvent &&
                rule.trigger.kind == event.kind &&
                rule.condition(event)
        }

    /** Enabled [AutomationTrigger.OnSchedule] rules that are due at [clock]. */
    fun firedBySchedule(rules: List<AutomationRule>, clock: Clock): List<AutomationRule> =
        rules.filter { rule ->
            val t = rule.trigger
            rule.enabled && t is AutomationTrigger.OnSchedule &&
                ScheduleEvaluator.isDue(ScheduledTask(rule.id, t.schedule, rule.enabled, rule.lastRunMillis), clock)
        }

    /** The flattened actions of the given [rules], in rule order. */
    fun actions(rules: List<AutomationRule>): List<String> = rules.flatMap { it.actions }
}
