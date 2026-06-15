package com.kianirani.jarvis.router.health

import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** VB4 — Availability Graph: circuit breaker, cooldown, Retry-After, EWMA. */
class AvailabilityGraphTest {

    private var clock = 0L
    private fun graph(config: AvailabilityGraph.Config = AvailabilityGraph.Config()) =
        AvailabilityGraph(now = { clock }, config = config)

    private fun spec(id: String) =
        ModelSpec(id = id, displayName = id, backend = ModelBackend.CLOUD, provider = "GROQ")

    private val m = spec("groq-1")

    /** Jump to the model's cooldown edge, take the half-open probe, and fail it. */
    private fun advanceToProbeAndFail(g: AvailabilityGraph) {
        clock = g.health(m).cooldownUntil
        g.isAvailable(m)
        g.recordFailure(m)
    }

    @Test
    fun `unseen model is available and closed`() {
        val g = graph()
        assertTrue(g.isAvailable(m))
        assertEquals(AvailabilityGraph.Circuit.CLOSED, g.health(m).circuit)
    }

    @Test
    fun `single failures below threshold do not open the breaker`() {
        val g = graph(AvailabilityGraph.Config(failureThreshold = 3))
        g.recordFailure(m)
        g.recordFailure(m)
        assertTrue(g.isAvailable(m))
        assertEquals(AvailabilityGraph.Circuit.CLOSED, g.health(m).circuit)
        assertEquals(2, g.health(m).consecutiveFailures)
    }

    @Test
    fun `breaker opens after threshold consecutive failures and refuses calls`() {
        val g = graph(AvailabilityGraph.Config(failureThreshold = 3, baseCooldownMs = 15_000))
        repeat(3) { g.recordFailure(m) }
        assertEquals(AvailabilityGraph.Circuit.OPEN, g.health(m).circuit)
        assertFalse(g.isAvailable(m))
        assertEquals(15_000L, g.health(m).cooldownUntil)
    }

    @Test
    fun `open breaker allows one half-open probe after cooldown elapses`() {
        val g = graph(AvailabilityGraph.Config(failureThreshold = 1, baseCooldownMs = 10_000))
        g.recordFailure(m) // opens immediately (threshold 1)
        assertFalse(g.isAvailable(m))

        clock = 9_999
        assertFalse(g.isAvailable(m)) // still cooling

        clock = 10_000
        assertTrue(g.isAvailable(m)) // probe allowed
        assertEquals(AvailabilityGraph.Circuit.HALF_OPEN, g.health(m).circuit)
    }

    @Test
    fun `successful probe closes the breaker and clears cooldown`() {
        val g = graph(AvailabilityGraph.Config(failureThreshold = 1, baseCooldownMs = 10_000))
        g.recordFailure(m)
        clock = 10_000
        g.isAvailable(m) // → HALF_OPEN
        g.recordSuccess(m, latencyMs = 120)
        assertEquals(AvailabilityGraph.Circuit.CLOSED, g.health(m).circuit)
        assertEquals(0L, g.health(m).cooldownUntil)
        assertTrue(g.isAvailable(m))
    }

    @Test
    fun `failed probe reopens the breaker with exponential backoff`() {
        val g = graph(AvailabilityGraph.Config(failureThreshold = 1, baseCooldownMs = 10_000))
        g.recordFailure(m) // open #1 → cooldown 10_000
        assertEquals(10_000L, g.health(m).cooldownUntil)

        clock = 10_000
        g.isAvailable(m) // → HALF_OPEN
        g.recordFailure(m) // probe fails → open #2 → backoff doubles to 20_000
        assertEquals(AvailabilityGraph.Circuit.OPEN, g.health(m).circuit)
        assertEquals(10_000L + 20_000L, g.health(m).cooldownUntil)
    }

    @Test
    fun `retry-after pins the cooldown exactly and opens immediately`() {
        val g = graph(AvailabilityGraph.Config(failureThreshold = 5))
        g.recordFailure(m, retryAfterMs = 42_000) // one 429 opens regardless of threshold
        assertEquals(AvailabilityGraph.Circuit.OPEN, g.health(m).circuit)
        assertEquals(42_000L, g.health(m).cooldownUntil)
        assertFalse(g.isAvailable(m))

        clock = 41_999
        assertFalse(g.isAvailable(m))
        clock = 42_000
        assertTrue(g.isAvailable(m))
    }

    @Test
    fun `backoff is capped at maxCooldownMs`() {
        val g = graph(
            AvailabilityGraph.Config(failureThreshold = 1, baseCooldownMs = 10_000, maxCooldownMs = 25_000),
        )
        // open #1 = 10k, #2 = 20k, #3 would be 40k but capped to 25k
        g.recordFailure(m)
        advanceToProbeAndFail(g)
        advanceToProbeAndFail(g)
        val last = g.health(m)
        assertEquals(25_000L, last.cooldownUntil - clock)
    }

    @Test
    fun `latency ewma seeds on first sample then smooths`() {
        val g = graph(AvailabilityGraph.Config(latencyAlpha = 0.5))
        g.recordSuccess(m, latencyMs = 100)
        assertEquals(100.0, g.health(m).latencyMs, 0.001) // seeded
        g.recordSuccess(m, latencyMs = 200)
        assertEquals(150.0, g.health(m).latencyMs, 0.001) // 0.5*200 + 0.5*100
    }

    @Test
    fun `error rate rises on failure and falls on success`() {
        val g = graph(AvailabilityGraph.Config(failureThreshold = 99, errorAlpha = 0.5))
        g.recordSuccess(m, latencyMs = 50) // seed errorRate = 0
        g.recordFailure(m) // 0.5*1 + 0.5*0 = 0.5
        assertEquals(0.5, g.health(m).errorRate, 0.001)
        g.recordSuccess(m, latencyMs = 50) // 0.5*0 + 0.5*0.5 = 0.25
        assertEquals(0.25, g.health(m).errorRate, 0.001)
    }

    @Test
    fun `filterAvailable drops cooling models but keeps healthy ones in order`() {
        val g = graph(AvailabilityGraph.Config(failureThreshold = 1, baseCooldownMs = 10_000))
        val a = spec("a")
        val b = spec("b")
        val c = spec("c")
        g.recordFailure(b) // b's breaker opens
        assertEquals(listOf(a, c), g.filterAvailable(listOf(a, b, c)))
    }

    @Test
    fun `success after some failures resets the consecutive counter`() {
        val g = graph(AvailabilityGraph.Config(failureThreshold = 3))
        g.recordFailure(m)
        g.recordFailure(m)
        g.recordSuccess(m, latencyMs = 80)
        assertEquals(0, g.health(m).consecutiveFailures)
        // two more failures should NOT open (counter was reset)
        g.recordFailure(m)
        g.recordFailure(m)
        assertEquals(AvailabilityGraph.Circuit.CLOSED, g.health(m).circuit)
    }
}
