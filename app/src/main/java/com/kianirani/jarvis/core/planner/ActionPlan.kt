package com.kianirani.jarvis.core.planner

import com.kianirani.jarvis.router.orchestrator.Intent

/**
 * CF3 — a multi-step plan for one user goal.
 *
 * The [TaskPlanner] breaks a goal into ordered [PlanStep]s; each step is either a
 * device [StepKind.TOOL] action or a [StepKind.MODEL] turn. CF1 (AgentEngine)
 * will later walk these steps (Goal → Plan → Act → Observe), but the plan itself
 * is a pure, inspectable value — no execution, no side effects.
 */
enum class StepKind {
    /** A device action handled by a tool (CF2/ToolCaller). */
    TOOL,

    /** A turn answered by a model via the router. */
    MODEL,
}

data class PlanStep(
    val index: Int,
    val kind: StepKind,
    val instruction: String,
    val intent: Intent,
)

data class ActionPlan(
    val goal: String,
    val steps: List<PlanStep>,
) {
    val isMultiStep: Boolean get() = steps.size > 1
    val isEmpty: Boolean get() = steps.isEmpty()
}
