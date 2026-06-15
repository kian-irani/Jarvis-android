package com.kianirani.jarvis.core.eval

import com.kianirani.jarvis.router.orchestrator.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** CF6 — golden-prompt regression for the on-device intent classifier (EN + FA + code-switch). */
class EvalHarnessTest {

    private val harness = EvalHarness()

    @Test
    fun `the golden corpus passes 100 percent`() {
        val report = harness.run()
        assertTrue(
            "intent-routing regression:\n${report.describeFailures()}",
            report.failures.isEmpty(),
        )
        assertEquals(1.0, report.passRate, 0.0)
    }

    @Test
    fun `corpus covers every intent and both languages plus code-switch`() {
        val intents = GoldenPrompts.cases.map { it.expectedIntent }.toSet()
        // Every routed intent the classifier can emit must be represented.
        assertTrue(intents.containsAll(listOf(Intent.CODE, Intent.ACTION, Intent.REASONING, Intent.QUICK, Intent.CHAT, Intent.IMAGE)))
        assertTrue("needs a code-switch case", GoldenPrompts.cases.any { it.name.contains("switch") })
        // Persian cases present (contain Arabic-script characters).
        assertTrue("needs Persian cases", GoldenPrompts.cases.any { it.prompt.any { ch -> ch in '؀'..'ۿ' } })
    }

    @Test
    fun `a deliberately mislabelled case is reported as a failure`() {
        val bogus = listOf(EvalCase("bogus", "turn on the flashlight", Intent.CHAT))
        val report = harness.run(bogus)
        assertEquals(0, report.passed)
        assertEquals(1, report.failures.size)
        assertTrue(report.describeFailures().contains("bogus"))
    }
}
