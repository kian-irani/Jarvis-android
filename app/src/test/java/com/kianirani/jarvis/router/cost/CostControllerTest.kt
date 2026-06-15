package com.kianirani.jarvis.router.cost

import com.kianirani.jarvis.router.registry.CapabilityScores
import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** VB7 — Adaptive Cost Controller: estimate, mode gating, budget cap, 80% warning. */
class CostControllerTest {

    /** cost score is inverted: 10 = free, 1 = very expensive. */
    private fun cloud(costScore: Int) = ModelSpec(
        id = "m$costScore", displayName = "m", backend = ModelBackend.CLOUD, provider = "OPENAI",
        scores = CapabilityScores(cost = costScore),
    )

    private val local = ModelSpec(id = "qwen", displayName = "q", backend = ModelBackend.LOCAL)
    private fun controller() = CostController()

    @Test
    fun `local models are always free`() {
        assertEquals(0.0, controller().estimate(local, 10_000, 10_000), 0.0001)
    }

    @Test
    fun `estimate scales with tokens and inverse cost score`() {
        val c = controller()
        // perK = 10 - cost = 10 - 7 = 3; (1000+1000)/1000 * 3 = 6.0
        assertEquals(6.0, c.estimate(cloud(7), 1000, 1000), 0.0001)
        // cheaper model (cost 9 → perK 1): same tokens → 2.0
        assertEquals(2.0, c.estimate(cloud(9), 1000, 1000), 0.0001)
    }

    @Test
    fun `unlimited mode allows even an expensive model`() {
        val c = controller() // default UNLIMITED
        assertTrue(c.check(cloud(1), 5000, 5000).allowed)
    }

    @Test
    fun `economy mode rejects an expensive model but allows a cheap one`() {
        val c = controller().apply { mode = CostMode.ECONOMY } // cap 2.0
        assertFalse(c.check(cloud(1), 1000, 1000).allowed) // perK 9 → 18.0 > 2
        assertTrue(c.check(cloud(9), 500, 500).allowed) // perK 1 → 1.0 <= 2
    }

    @Test
    fun `economy mode always allows the free local model`() {
        val c = controller().apply { mode = CostMode.ECONOMY }
        assertTrue(c.check(local, 100_000, 100_000).allowed)
    }

    @Test
    fun `budget cap blocks once projected spend would exceed it`() {
        val c = controller().apply {
            mode = CostMode.PREMIUM
            budgetUnits = 10.0
        }
        c.record(8.0) // spent 8
        // estimate for cloud(7) over 1000+1000 = 6.0 → 8 + 6 = 14 > 10 → blocked
        val d = c.check(cloud(7), 1000, 1000)
        assertFalse(d.allowed)
        assertTrue(d.reason.contains("budget"))
    }

    @Test
    fun `budget cap allows when projected spend fits`() {
        val c = controller().apply {
            mode = CostMode.PREMIUM
            budgetUnits = 100.0
        }
        c.record(8.0)
        assertTrue(c.check(cloud(7), 1000, 1000).allowed) // 8 + 6 = 14 <= 100
    }

    @Test
    fun `unlimited mode ignores the budget`() {
        val c = controller().apply { budgetUnits = 1.0 } // mode stays UNLIMITED
        c.record(100.0)
        assertTrue(c.check(cloud(1), 5000, 5000).allowed)
    }

    @Test
    fun `budget warning trips at 80 percent`() {
        val c = controller().apply { budgetUnits = 10.0 }
        c.record(7.9)
        assertFalse(c.budgetWarning())
        c.record(0.1) // 8.0 == 80%
        assertTrue(c.budgetWarning())
    }

    @Test
    fun `no budget means no warning`() {
        val c = controller()
        c.record(1_000.0)
        assertFalse(c.budgetWarning())
    }

    @Test
    fun `resetPeriod clears accumulated spend`() {
        val c = controller().apply { budgetUnits = 10.0 }
        c.record(9.0)
        c.resetPeriod()
        assertEquals(0.0, c.spentUnits, 0.0001)
        assertFalse(c.budgetWarning())
    }
}
