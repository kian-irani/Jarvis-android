package com.kianirani.jarvis.core.task

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * DS-B4 acceptance: RetryPolicy decides + backs off correctly, and the executors retry
 * transient failures then surface the final error / fall back. Pure + virtual-clock (no
 * real delay, no device).
 */
class TaskResilienceTest {

    // --- RetryPolicy (pure) ---

    @Test fun `backoff grows exponentially and caps`() {
        val p = RetryPolicy(baseDelayMillis = 100, multiplier = 2.0, maxDelayMillis = 500)
        assertEquals(100L, p.backoffMillis(1))
        assertEquals(200L, p.backoffMillis(2))
        assertEquals(400L, p.backoffMillis(3))
        assertEquals(500L, p.backoffMillis(4)) // 800 -> capped at 500
    }

    @Test fun `shouldRetry stops at maxAttempts and respects retryOn`() {
        val p = RetryPolicy(maxAttempts = 3)
        assertTrue(p.shouldRetry(1, RuntimeException()))
        assertTrue(p.shouldRetry(2, RuntimeException()))
        assertEquals(false, p.shouldRetry(3, RuntimeException())) // exhausted
        val onlyIllegalState = RetryPolicy(retryOn = { it is IllegalStateException })
        assertEquals(false, onlyIllegalState.shouldRetry(1, IllegalArgumentException()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maxAttempts below one is rejected`() {
        RetryPolicy(maxAttempts = 0)
    }

    // --- withRetry ---

    @Test fun `withRetry succeeds after transient failures`() = runTest {
        var calls = 0
        val result = withRetry(RetryPolicy(maxAttempts = 5, baseDelayMillis = 10)) { attempt ->
            calls++
            if (attempt < 3) throw RuntimeException("transient")
            "ok@$attempt"
        }
        assertEquals("ok@3", result)
        assertEquals(3, calls)
    }

    @Test fun `withRetry rethrows after exhausting attempts`() = runTest {
        var calls = 0
        try {
            withRetry(RetryPolicy(maxAttempts = 2, baseDelayMillis = 10)) {
                calls++
                throw IllegalStateException("always")
            }
            fail("expected throw")
        } catch (e: IllegalStateException) {
            assertEquals("always", e.message)
        }
        assertEquals(2, calls)
    }

    @Test fun `withRetry does not retry a non-retryable error`() = runTest {
        var calls = 0
        val policy = RetryPolicy(maxAttempts = 5, retryOn = { it !is IllegalArgumentException })
        try {
            withRetry(policy) { calls++; throw IllegalArgumentException("fatal") }
            fail("expected throw")
        } catch (_: IllegalArgumentException) {
        }
        assertEquals(1, calls)
    }

    // --- withFallback ---

    @Test fun `withFallback returns the first alternative that succeeds`() = runTest {
        val order = mutableListOf<String>()
        val result = withFallback(
            listOf(
                { order += "a"; throw RuntimeException("a fails") },
                { order += "b"; "from-b" },
                { order += "c"; "from-c" },
            ),
        )
        assertEquals("from-b", result)
        assertEquals(listOf("a", "b"), order) // c never tried
    }

    @Test fun `withFallback rethrows the last error with earlier ones suppressed`() = runTest {
        try {
            withFallback<String>(
                listOf(
                    { throw RuntimeException("first") },
                    { throw IllegalStateException("last") },
                ),
            )
            fail("expected throw")
        } catch (e: IllegalStateException) {
            assertEquals("last", e.message)
            assertEquals("first", e.suppressed.single().message)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `withFallback requires at least one alternative`() = runTest {
        withFallback<String>(emptyList())
    }
}
