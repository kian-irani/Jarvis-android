package com.kianirani.jarvis.core.graph

import kotlinx.serialization.Serializable

/**
 * VCF-G1 — graph state + reducers (PRD §4.2).
 *
 * LangGraph's idea: a node never mutates state, it returns a partial [StateUpdate];
 * the runner **reduces** updates per channel — messages *append*, plan *replace*,
 * observations *append*, scratch *merge*, steps *bound*. That per-channel reduce is
 * what lets a cyclic agent loop (model → tools → model …) stay coherent and
 * terminate. Everything here is a pure, serializable value (VCF-G3 checkpointing).
 */

/** What a step does — a device action via a tool, or a model turn. */
@Serializable
enum class StepKind { TOOL, MODEL }

/** One step of an [ActionPlan]. The richer planner (VCF-A3) populates these. */
@Serializable
data class PlanStep(
    val id: String,
    val instruction: String,
    val kind: StepKind = StepKind.MODEL,
    val dependsOn: List<String> = emptyList(),
    val toolHint: String? = null,
)

/** A typed, inspectable plan for one goal (REPLACE channel — VCF-A3 fills it in). */
@Serializable
data class ActionPlan(
    val goal: String,
    val steps: List<PlanStep> = emptyList(),
) {
    val isEmpty: Boolean get() = steps.isEmpty()
    val isMultiStep: Boolean get() = steps.size > 1
}

/** The "Observe" in Goal → Plan → Act → Observe: a result fed back into the loop. */
@Serializable
data class Observation(
    val source: String,
    val content: String,
    val isError: Boolean = false,
)

/**
 * The full, serializable state threaded through one graph run. Channels carry their
 * own reduce semantics (see [reduce]); [remainingSteps] bounds the loop so a cyclic
 * graph always terminates (LangGraph `remaining_steps`).
 */
@Serializable
data class GraphState(
    val messages: List<VisionMessage> = emptyList(), // reducer: APPEND
    val plan: ActionPlan? = null, // reducer: REPLACE
    val observations: List<Observation> = emptyList(), // reducer: APPEND
    val remainingSteps: Int = DEFAULT_STEP_BUDGET, // bound
    val scratch: Map<String, String> = emptyMap(), // reducer: MERGE
    val sessionId: String = "",
) {
    /** True once the step budget is spent — the runner must stop looping. */
    val isExhausted: Boolean get() = remainingSteps <= 0

    companion object {
        const val DEFAULT_STEP_BUDGET = 12
    }
}

/**
 * A node's return value. The node computes this; the runner [reduce]s it into the
 * canonical [GraphState]. A node must never mutate [GraphState] directly.
 */
data class StateUpdate(
    val appendMessages: List<VisionMessage> = emptyList(),
    val plan: ActionPlan? = null,
    val appendObservations: List<Observation> = emptyList(),
    val spendStep: Boolean = false,
    val scratch: Map<String, String> = emptyMap(),
)

/**
 * Apply one [StateUpdate], reducing per channel. Pure — returns a new state, never
 * mutates the receiver.
 */
fun GraphState.reduce(u: StateUpdate): GraphState = copy(
    messages = messages + u.appendMessages,
    plan = u.plan ?: plan,
    observations = observations + u.appendObservations,
    remainingSteps = remainingSteps - if (u.spendStep) 1 else 0,
    scratch = scratch + u.scratch,
)
