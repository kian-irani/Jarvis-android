package com.kianirani.jarvis.core.planner

import com.kianirani.jarvis.router.orchestrator.Intent
import com.kianirani.jarvis.router.orchestrator.IntentClassifier
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CF3 — breaks a goal into an ordered [ActionPlan].
 *
 * A deliberately cheap, on-device first pass (no model call): it splits the goal
 * on sequence connectors ("then", "؛/،", "سپس", …) into clauses, classifies each
 * with the [IntentClassifier], and marks it a [StepKind.TOOL] step when it is a
 * device action, else a [StepKind.MODEL] step. A goal with no connector becomes a
 * single step. CF1 (AgentEngine) executes the plan; a wrong split only changes
 * step granularity, never correctness (the engine re-plans / falls back).
 *
 * Connectors are intentionally conservative — only clear *sequence* words — so
 * everyday "and" inside a single request ("salt and pepper") is not over-split.
 */
@Singleton
class TaskPlanner @Inject constructor(
    private val classifier: IntentClassifier,
) {

    fun plan(goal: String): ActionPlan {
        val clauses = split(goal)
        val steps = clauses.mapIndexed { i, clause ->
            val (intent, _) = classifier.classify(clause)
            PlanStep(
                index = i,
                kind = if (intent == Intent.ACTION) StepKind.TOOL else StepKind.MODEL,
                instruction = clause,
                intent = intent,
            )
        }
        return ActionPlan(goal = goal.trim(), steps = steps)
    }

    /** Split a goal into clauses on sequence connectors; trims and drops blanks. */
    private fun split(goal: String): List<String> =
        goal.split(CONNECTORS)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private companion object {
        // " and then ", " then ", "؛/،/;", " سپس " — case-insensitive for the English words.
        val CONNECTORS = Regex(
            "\\s+and\\s+then\\s+|\\s+then\\s+|\\s*[؛،;]\\s*|\\s+سپس\\s+",
            RegexOption.IGNORE_CASE,
        )
    }
}
