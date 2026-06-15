package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.planner.ActionPlan
import com.kianirani.jarvis.core.planner.PlanStep
import com.kianirani.jarvis.core.planner.TaskPlanner
import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * CF1 — the agent loop.
 *
 * Turns a goal into a [com.kianirani.jarvis.core.planner.ActionPlan] (CF3) and
 * walks it step by step (Goal → Plan → Act → Observe), accumulating an [AgentRun]
 * trace. The loop is **bounded** ([maxSteps]) so a runaway plan can't spin, and
 * **cooperatively cancellable** (it checks the coroutine before each step).
 *
 * Execution of a step is delegated to a [StepExecutor] — the seam where CF2
 * (real device tools) and the model router plug in. Keeping the loop independent
 * of *how* a step runs makes it pure and unit-testable, and means the engine
 * never itself triggers a device action (that stays behind the Trust gate when
 * the real executor is wired on-device).
 */
@Singleton
class AgentEngine @Inject constructor(
    private val planner: TaskPlanner,
) {

    /**
     * Plan [goal] and execute its steps with [executor].
     *
     * @param maxSteps hard cap on executed steps (bounds runaway plans).
     * @param stopOnFailure stop at the first failing step (else run the whole plan).
     */
    suspend fun run(
        goal: String,
        executor: StepExecutor,
        maxSteps: Int = DEFAULT_MAX_STEPS,
        stopOnFailure: Boolean = true,
    ): AgentRun {
        val plan = planner.plan(goal)
        val capped = plan.steps.take(maxSteps)
        val results = ArrayList<AgentStepResult>(capped.size)
        var stoppedOnFailure = false
        for (step in capped) {
            coroutineContext.ensureActive() // cooperative cancellation between steps
            val outcome = executor.execute(step)
            results += AgentStepResult(step, outcome)
            if (stopOnFailure && !outcome.ok) {
                stoppedOnFailure = true
                break
            }
        }
        // "completed" = every planned step actually ran (not truncated by the cap or a failure stop).
        val completed = !stoppedOnFailure && results.size == plan.steps.size
        val success = completed && results.isNotEmpty() && results.all { it.outcome.ok }
        return AgentRun(goal = plan.goal, plan = plan, results = results, completed = completed, success = success)
    }

    companion object {
        const val DEFAULT_MAX_STEPS = 8
    }
}

/** Executes one [PlanStep] (a tool action or a model turn) and reports the outcome. */
fun interface StepExecutor {
    suspend fun execute(step: PlanStep): StepOutcome
}

/** Result of running one step: whether it succeeded and the text it produced. */
data class StepOutcome(val ok: Boolean, val text: String)

data class AgentStepResult(val step: PlanStep, val outcome: StepOutcome)

/** The full trace of one agent run. */
data class AgentRun(
    val goal: String,
    val plan: ActionPlan,
    val results: List<AgentStepResult>,
    val completed: Boolean,
    val success: Boolean,
) {
    /** The last step's text — what Vision says back to the user. */
    val finalText: String get() = results.lastOrNull()?.outcome?.text.orEmpty()

    /** Human-readable step-by-step trace for telemetry / the HUD. */
    val transcript: String
        get() = results.joinToString("\n") {
            "${it.step.index}. [${it.step.kind}/${it.step.intent}] ${it.step.instruction} → " +
                (if (it.outcome.ok) "ok" else "fail") + ": ${it.outcome.text}"
        }
}
