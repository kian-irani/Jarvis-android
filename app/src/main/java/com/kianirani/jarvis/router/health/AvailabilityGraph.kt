package com.kianirani.jarvis.router.health

import com.kianirani.jarvis.router.registry.ModelSpec
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * VISION BRAIN V1 — Module 8 (Availability Graph / VB4).
 *
 * Live health for every model the router can reach, so the orchestrator never
 * sends a request to a backend that is rate-limited, erroring, or dead. For each
 * [ModelSpec.key] it tracks:
 *
 *  - **latency** as an EWMA (smoothed round-trip ms) — input to future cost/speed ranking,
 *  - **error rate** as an EWMA in 0..1,
 *  - a **circuit breaker** (`CLOSED → OPEN → HALF_OPEN`) that trips after
 *    [failureThreshold] consecutive failures and re-probes once its cooldown elapses,
 *  - a **cooldown** that honours a server `Retry-After` (429) exactly, and otherwise
 *    backs off exponentially each time the breaker re-opens.
 *
 * It supersedes the count-only [com.kianirani.jarvis.data.ai.AiUsageStore] for routing
 * decisions: the [com.kianirani.jarvis.router.backend.BackendRouter] calls
 * [isAvailable] before dispatching and records the outcome, so an unhealthy model is
 * skipped and the Substitution Engine (VB5) moves to the next candidate — Vision keeps
 * answering while any healthy alternative remains.
 *
 * Pure and deterministic: the clock is injected so the breaker/cooldown logic is unit
 * testable without real time.
 */
@Singleton
class AvailabilityGraph internal constructor(
    private val now: () -> Long,
    private val config: Config = Config(),
) {
    @Inject constructor() : this({ System.currentTimeMillis() })

    /** Tunables for the breaker and EWMA smoothing. */
    data class Config(
        /** Consecutive failures (from CLOSED) that trip the breaker open. */
        val failureThreshold: Int = 3,
        /** First cooldown when the breaker opens, in ms. Doubles on each re-open. */
        val baseCooldownMs: Long = 15_000,
        /** Upper bound on the exponential cooldown, in ms. */
        val maxCooldownMs: Long = 5 * 60_000,
        /** EWMA weight for the newest latency sample (0..1). */
        val latencyAlpha: Double = 0.3,
        /** EWMA weight for the newest success/failure sample (0..1). */
        val errorAlpha: Double = 0.3,
    )

    enum class Circuit { CLOSED, OPEN, HALF_OPEN }

    /** Immutable view of one model's health, surfaced to VB9 telemetry. */
    data class Health(
        val circuit: Circuit = Circuit.CLOSED,
        val latencyMs: Double = 0.0,
        val errorRate: Double = 0.0,
        val consecutiveFailures: Int = 0,
        val cooldownUntil: Long = 0L,
        val opens: Int = 0,
        val calls: Long = 0L,
        val lastOutcomeAt: Long = 0L,
    )

    private val states = ConcurrentHashMap<String, Health>()

    /**
     * Whether [spec] may be dispatched right now. A model whose breaker is OPEN is
     * refused until its cooldown elapses, at which point it is allowed exactly one
     * HALF_OPEN probe. This is the call the router makes to reject unhealthy models.
     */
    @Synchronized
    fun isAvailable(spec: ModelSpec): Boolean {
        val h = states[spec.key] ?: return true
        return when (h.circuit) {
            Circuit.CLOSED, Circuit.HALF_OPEN -> true
            Circuit.OPEN -> {
                if (now() >= h.cooldownUntil) {
                    // Cooldown elapsed → allow a single probe.
                    states[spec.key] = h.copy(circuit = Circuit.HALF_OPEN)
                    true
                } else {
                    false
                }
            }
        }
    }

    /** Record a successful call of [latencyMs]; closes the breaker and clears cooldown. */
    @Synchronized
    fun recordSuccess(spec: ModelSpec, latencyMs: Long) {
        val h = states[spec.key] ?: Health()
        states[spec.key] = h.copy(
            circuit = Circuit.CLOSED,
            latencyMs = ewma(h.latencyMs, latencyMs.toDouble(), config.latencyAlpha, seeded = h.calls > 0),
            errorRate = ewma(h.errorRate, 0.0, config.errorAlpha, seeded = h.calls > 0),
            consecutiveFailures = 0,
            cooldownUntil = 0L,
            calls = h.calls + 1,
            lastOutcomeAt = now(),
        )
    }

    /**
     * Record a failed call. A [retryAfterMs] from a 429 response pins the cooldown to
     * exactly that window; otherwise the breaker trips once [Config.failureThreshold]
     * consecutive failures accumulate (or immediately if it was probing HALF_OPEN),
     * backing off exponentially per re-open.
     */
    @Synchronized
    fun recordFailure(spec: ModelSpec, retryAfterMs: Long? = null) {
        val h = states[spec.key] ?: Health()
        val failures = h.consecutiveFailures + 1
        val errorRate = ewma(h.errorRate, 1.0, config.errorAlpha, seeded = h.calls > 0)

        val rateLimited = retryAfterMs != null
        val tripFromClosed = failures >= config.failureThreshold
        val tripFromProbe = h.circuit == Circuit.HALF_OPEN

        if (rateLimited || tripFromClosed || tripFromProbe) {
            val opens = h.opens + 1
            val cooldown = retryAfterMs ?: backoff(opens)
            states[spec.key] = h.copy(
                circuit = Circuit.OPEN,
                errorRate = errorRate,
                consecutiveFailures = failures,
                cooldownUntil = now() + cooldown,
                opens = opens,
                calls = h.calls + 1,
                lastOutcomeAt = now(),
            )
        } else {
            states[spec.key] = h.copy(
                circuit = Circuit.CLOSED,
                errorRate = errorRate,
                consecutiveFailures = failures,
                calls = h.calls + 1,
                lastOutcomeAt = now(),
            )
        }
    }

    /** Current health for [spec] (CLOSED/empty if never seen). */
    fun health(spec: ModelSpec): Health = states[spec.key] ?: Health()

    /** Keep [specs] in their ranked order, dropping any that are not currently available. */
    fun filterAvailable(specs: List<ModelSpec>): List<ModelSpec> = specs.filter { isAvailable(it) }

    /** Forget all health (e.g. user reset / tests). */
    fun reset() = states.clear()

    private fun backoff(opens: Int): Long {
        // opens == 1 → base, then double each subsequent open, capped.
        val shifted = config.baseCooldownMs.toDouble() * Math.pow(2.0, (opens - 1).toDouble())
        return min(shifted, config.maxCooldownMs.toDouble()).toLong()
    }

    private fun ewma(prev: Double, sample: Double, alpha: Double, seeded: Boolean): Double =
        if (!seeded) sample else alpha * sample + (1 - alpha) * prev
}
