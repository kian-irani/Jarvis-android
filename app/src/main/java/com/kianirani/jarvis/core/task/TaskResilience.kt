package com.kianirani.jarvis.core.task

import kotlinx.coroutines.delay

/**
 * DS-B4 — Task engine resilience: a pure [RetryPolicy] (decide + back off) plus thin
 * suspend executors ([withRetry], [withFallback]) that make a chained workflow robust —
 * a transient tool/model failure is retried with exponential backoff, and an exhausted
 * step can fall back to an alternative (e.g. cloud → local, primary tool → degraded tool).
 *
 * The policy is pure and unit-testable on its own; the executors use `delay`, so they're
 * tested with `runTest`'s virtual clock (no real waiting, no device). This is the generic
 * spine the agent/`ToolCaller` layers build on — it deliberately knows nothing about HTTP,
 * tools, or models.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val baseDelayMillis: Long = 200L,
    val multiplier: Double = 2.0,
    val maxDelayMillis: Long = 10_000L,
    /** Which errors are worth retrying; by default everything (a transient call). */
    val retryOn: (Throwable) -> Boolean = { true },
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
    }

    /**
     * Should a failed [attempt] (1-based) be retried for [error]? False once attempts are
     * exhausted or the error isn't retryable.
     */
    fun shouldRetry(attempt: Int, error: Throwable): Boolean =
        attempt < maxAttempts && retryOn(error)

    /** Exponential backoff before the *next* attempt (1-based), capped at [maxDelayMillis]. */
    fun backoffMillis(attempt: Int): Long {
        if (attempt < 1) return 0L
        val raw = baseDelayMillis * Math.pow(multiplier, (attempt - 1).toDouble())
        return raw.toLong().coerceIn(0L, maxDelayMillis)
    }
}

/**
 * Run [block], retrying per [policy] with exponential backoff. Returns the first success;
 * rethrows the last error once retries are exhausted (or the error isn't retryable).
 */
suspend fun <T> withRetry(policy: RetryPolicy = RetryPolicy(), block: suspend (attempt: Int) -> T): T {
    var attempt = 1
    while (true) {
        try {
            return block(attempt)
        } catch (e: Throwable) {
            if (!policy.shouldRetry(attempt, e)) throw e
            delay(policy.backoffMillis(attempt))
            attempt++
        }
    }
}

/**
 * Try [alternatives] in order, returning the first success. An empty list is a programming
 * error; if every alternative fails the last error is rethrown (with earlier ones suppressed).
 * Use for graceful degradation: `withFallback(listOf({ cloud() }, { local() }))`.
 */
suspend fun <T> withFallback(alternatives: List<suspend () -> T>): T {
    require(alternatives.isNotEmpty()) { "withFallback needs at least one alternative" }
    var last: Throwable? = null
    for (alt in alternatives) {
        try {
            return alt()
        } catch (e: Throwable) {
            last?.let { e.addSuppressed(it) }
            last = e
        }
    }
    throw last!!
}
