package com.kianirani.jarvis.data.ai

import com.kianirani.jarvis.data.ai.TokenPool.FailureKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** VB6 — Token Pool: load-balance ordering, per-key cooldown, auto-recovery, classify. */
class TokenPoolTest {

    private var clock = 0L
    private fun pool(config: TokenPool.Config = TokenPool.Config()) =
        TokenPool(now = { clock }, config = config)

    private val p = AiProvider.GROQ
    private val keys = listOf("k1", "k2", "k3")

    @Test
    fun `fresh keys keep their original order`() {
        assertEquals(keys, pool().order(p, keys))
    }

    @Test
    fun `least-recently-used key is tried first`() {
        val pool = pool()
        clock = 100
        pool.recordSuccess(p, "k1") // k1 used most recently
        clock = 200
        // k2 and k3 never used (lastUsed 0) come before k1; tie broken by index
        assertEquals(listOf("k2", "k3", "k1"), pool.order(p, keys))
    }

    @Test
    fun `a rate-limited key is parked behind healthy keys`() {
        val pool = pool(TokenPool.Config(rateLimitBaseMs = 20_000))
        pool.recordFailure(p, "k1", FailureKind.RATE_LIMIT)
        assertFalse(pool.isReady(p, "k1"))
        // k2,k3 eligible first; k1 cooling goes last
        assertEquals(listOf("k2", "k3", "k1"), pool.order(p, keys))
    }

    @Test
    fun `a parked key auto-recovers after its cooldown`() {
        val pool = pool(TokenPool.Config(rateLimitBaseMs = 20_000))
        pool.recordFailure(p, "k1", FailureKind.RATE_LIMIT)
        clock = 19_999
        assertFalse(pool.isReady(p, "k1"))
        clock = 20_000
        assertTrue(pool.isReady(p, "k1"))
    }

    @Test
    fun `auth failures are parked much longer than rate limits`() {
        val pool = pool(TokenPool.Config(rateLimitBaseMs = 20_000, authFailMs = 600_000))
        pool.recordFailure(p, "k1", FailureKind.RATE_LIMIT)
        pool.recordFailure(p, "k2", FailureKind.AUTH)
        assertEquals(20_000L, pool.health(p, "k1").cooldownUntil)
        assertEquals(600_000L, pool.health(p, "k2").cooldownUntil)
    }

    @Test
    fun `consecutive failures back off exponentially`() {
        val pool = pool(TokenPool.Config(rateLimitBaseMs = 20_000))
        pool.recordFailure(p, "k1", FailureKind.RATE_LIMIT) // 20k
        assertEquals(20_000L, pool.health(p, "k1").cooldownUntil)
        clock = 20_000
        pool.recordFailure(p, "k1", FailureKind.RATE_LIMIT) // 2nd failure → 40k window
        assertEquals(20_000L + 40_000L, pool.health(p, "k1").cooldownUntil)
    }

    @Test
    fun `success clears cooldown and failure count`() {
        val pool = pool()
        pool.recordFailure(p, "k1", FailureKind.RATE_LIMIT)
        pool.recordSuccess(p, "k1")
        assertEquals(0, pool.health(p, "k1").failures)
        assertEquals(0L, pool.health(p, "k1").cooldownUntil)
        assertTrue(pool.isReady(p, "k1"))
    }

    @Test
    fun `when every key is cooling they are still returned soonest-first`() {
        val pool = pool(TokenPool.Config(rateLimitBaseMs = 20_000, authFailMs = 600_000))
        pool.recordFailure(p, "k1", FailureKind.AUTH) // 600k
        pool.recordFailure(p, "k2", FailureKind.RATE_LIMIT) // 20k
        pool.recordFailure(p, "k3", FailureKind.RATE_LIMIT) // 20k
        val order = pool.order(p, keys)
        assertEquals(3, order.size)
        assertEquals("k2", order.first()) // soonest to recover
        assertEquals("k1", order.last()) // longest cooldown last
    }

    @Test
    fun `quota counts every call`() {
        val pool = pool()
        pool.recordSuccess(p, "k1")
        pool.recordFailure(p, "k1", FailureKind.GENERIC)
        assertEquals(2L, pool.health(p, "k1").calls)
    }

    @Test
    fun `classify maps provider error messages to failure kinds`() {
        val pool = pool()
        assertEquals(FailureKind.RATE_LIMIT, pool.classify("GROQ 429 Too Many Requests"))
        assertEquals(FailureKind.AUTH, pool.classify("XAI 401 Unauthorized"))
        assertEquals(FailureKind.AUTH, pool.classify("OPENAI 403 Forbidden"))
        assertEquals(FailureKind.GENERIC, pool.classify("network unreachable"))
        assertEquals(FailureKind.GENERIC, pool.classify(null))
    }

    @Test
    fun `state is keyed by token not provider so two keys are independent`() {
        val pool = pool(TokenPool.Config(rateLimitBaseMs = 20_000))
        pool.recordFailure(p, "k1", FailureKind.RATE_LIMIT)
        assertFalse(pool.isReady(p, "k1"))
        assertTrue(pool.isReady(p, "k2")) // untouched key unaffected
    }
}
