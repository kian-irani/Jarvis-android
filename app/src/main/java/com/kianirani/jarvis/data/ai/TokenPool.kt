package com.kianirani.jarvis.data.ai

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * VISION BRAIN V1 — Module 6 (Token Pool Manager / VB6).
 *
 * Upgrades the old round-robin offset into a health-aware key pool. For every API
 * key of every provider it tracks failures, a cooldown, call count (quota), and last
 * use, then [order]s the keys so the router tries the **healthiest, least-recently-used**
 * key first and skips a key that is cooling down (401/429) until it auto-recovers.
 *
 * Behaviour vs. the old offset:
 *  - **Load-balance**: ties broken by least-recently-used, so traffic spreads across keys.
 *  - **Cooldown**: a failing key is parked (exponential backoff, longer for 401 auth
 *    errors than 429 rate limits) instead of being retried every request.
 *  - **Auto-recovery**: a parked key re-enters the order the moment its cooldown elapses.
 *  - **Never stuck**: if *all* keys are cooling they are still returned (soonest-first) so
 *    the router always has something to probe — it never hard-fails for lack of a key.
 *
 * Secret-safe: state is keyed by a non-reversible hash of the token, never the token
 * itself, so nothing here can leak a key. Pure/deterministic via an injected clock.
 */
@Singleton
class TokenPool internal constructor(
    private val now: () -> Long,
    private val config: Config = Config(),
) {
    @Inject constructor() : this({ System.currentTimeMillis() })

    data class Config(
        /** First cooldown for a rate-limited (429) key, doubled per consecutive failure. */
        val rateLimitBaseMs: Long = 20_000,
        /** Cooldown for an auth-rejected (401) key — likely bad, parked much longer. */
        val authFailMs: Long = 10 * 60_000,
        /** Cooldown for a generic failure (network, 5xx). */
        val genericBaseMs: Long = 10_000,
        /** Upper bound for any backoff. */
        val maxCooldownMs: Long = 15 * 60_000,
    )

    /** Why a key failed — shapes how long it is parked. */
    enum class FailureKind { RATE_LIMIT, AUTH, GENERIC }

    data class KeyHealth(
        val failures: Int = 0,
        val cooldownUntil: Long = 0L,
        val calls: Long = 0L,
        val lastUsed: Long = 0L,
    )

    private val states = ConcurrentHashMap<String, KeyHealth>()

    private fun id(provider: AiProvider, token: String): String =
        "${provider.name}#${Integer.toHexString(token.hashCode())}"

    /**
     * Reorder [keys] best-first: eligible (not cooling) keys ordered by least-recently-used,
     * then any cooling keys soonest-available-first so the router can still probe them.
     */
    @Synchronized
    fun order(provider: AiProvider, keys: List<String>): List<String> {
        val t = now()
        val eligible = ArrayList<Pair<String, KeyHealth>>()
        val cooling = ArrayList<Pair<String, KeyHealth>>()
        keys.forEach { k ->
            val h = states[id(provider, k)] ?: KeyHealth()
            if (t >= h.cooldownUntil) eligible += k to h else cooling += k to h
        }
        val eligibleOrdered = eligible
            .sortedWith(compareBy({ it.second.lastUsed }, { keys.indexOf(it.first) }))
            .map { it.first }
        val coolingOrdered = cooling
            .sortedBy { it.second.cooldownUntil }
            .map { it.first }
        return eligibleOrdered + coolingOrdered
    }

    /** Record a key that answered: clears failures/cooldown, bumps quota + last-used. */
    @Synchronized
    fun recordSuccess(provider: AiProvider, token: String) {
        val key = id(provider, token)
        val h = states[key] ?: KeyHealth()
        states[key] = h.copy(failures = 0, cooldownUntil = 0L, calls = h.calls + 1, lastUsed = now())
    }

    /** Record a failed key and park it per [kind]'s policy (auto-recovers after the cooldown). */
    @Synchronized
    fun recordFailure(provider: AiProvider, token: String, kind: FailureKind) {
        val key = id(provider, token)
        val h = states[key] ?: KeyHealth()
        val failures = h.failures + 1
        states[key] = h.copy(
            failures = failures,
            cooldownUntil = now() + cooldownFor(kind, failures),
            calls = h.calls + 1,
            lastUsed = now(),
        )
    }

    /** True if [token] is ready to use now (not cooling). */
    fun isReady(provider: AiProvider, token: String): Boolean {
        val h = states[id(provider, token)] ?: return true
        return now() >= h.cooldownUntil
    }

    fun health(provider: AiProvider, token: String): KeyHealth = states[id(provider, token)] ?: KeyHealth()

    fun reset() = states.clear()

    /** Map an exception message from the provider call to a [FailureKind]. */
    fun classify(message: String?): FailureKind = when {
        message == null -> FailureKind.GENERIC
        "429" in message || "Too Many Requests" in message -> FailureKind.RATE_LIMIT
        "401" in message || "403" in message || "Unauthorized" in message -> FailureKind.AUTH
        else -> FailureKind.GENERIC
    }

    private fun cooldownFor(kind: FailureKind, failures: Int): Long {
        val base = when (kind) {
            FailureKind.RATE_LIMIT -> config.rateLimitBaseMs
            FailureKind.AUTH -> config.authFailMs
            FailureKind.GENERIC -> config.genericBaseMs
        }
        val scaled = base.toDouble() * Math.pow(2.0, (failures - 1).toDouble())
        return min(scaled, config.maxCooldownMs.toDouble()).toLong()
    }
}
