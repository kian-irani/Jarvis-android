package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.planner.PlanStep
import com.kianirani.jarvis.core.planner.TaskPlanner
import com.kianirani.jarvis.router.orchestrator.IntentClassifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** CF1 — AgentEngine: plan walking, bounded steps, stop-on-failure, cancellation. */
class AgentEngineTest {

    private val engine = AgentEngine(TaskPlanner(IntentClassifier()))

    /** Executor that records the steps it ran and replies per-step from a script. */
    private class RecordingExecutor(private val fail: Set<String> = emptySet()) : StepExecutor {
        val ran = mutableListOf<String>()
        override suspend fun execute(step: PlanStep): StepOutcome {
            ran += step.instruction
            val ok = step.instruction !in fail
            return StepOutcome(ok = ok, text = if (ok) "did: ${step.instruction}" else "could not: ${step.instruction}")
        }
    }

    @Test
    fun `runs every step of a multi-step plan in order`() = runTest {
        val exec = RecordingExecutor()
        val run = engine.run("turn on the flashlight then what time is it", exec)
        assertEquals(listOf("turn on the flashlight", "what time is it"), exec.ran)
        assertTrue(run.completed)
        assertTrue(run.success)
        assertEquals("did: what time is it", run.finalText)
    }

    @Test
    fun `stops at the first failing step by default`() = runTest {
        val exec = RecordingExecutor(fail = setOf("turn on the flashlight"))
        val run = engine.run("turn on the flashlight then what time is it", exec)
        assertEquals(listOf("turn on the flashlight"), exec.ran) // second step never reached
        assertFalse(run.completed)
        assertFalse(run.success)
    }

    @Test
    fun `stopOnFailure false runs the whole plan despite a failure`() = runTest {
        val exec = RecordingExecutor(fail = setOf("turn on the flashlight"))
        val run = engine.run("turn on the flashlight then what time is it", exec, stopOnFailure = false)
        assertEquals(2, exec.ran.size)
        assertTrue(run.completed)
        assertFalse(run.success) // completed but not all ok
    }

    @Test
    fun `maxSteps caps execution and marks the run incomplete`() = runTest {
        val exec = RecordingExecutor()
        val run = engine.run("a then b then c", exec, maxSteps = 2)
        assertEquals(2, exec.ran.size)
        assertFalse(run.completed)
    }

    @Test
    fun `an empty goal produces an empty, unsuccessful run`() = runTest {
        val run = engine.run("   ", RecordingExecutor())
        assertTrue(run.plan.isEmpty)
        assertTrue(run.results.isEmpty())
        assertFalse(run.success)
        assertEquals("", run.finalText)
    }

    @Test
    fun `is cooperatively cancellable mid-run`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val exec = StepExecutor {
            entered.complete(Unit)
            awaitCancellation() // hang on the first step until cancelled
        }
        val job = launch { engine.run("a then b", exec) }
        entered.await()
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
    }
}
