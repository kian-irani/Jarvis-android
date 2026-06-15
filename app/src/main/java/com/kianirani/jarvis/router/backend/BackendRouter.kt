package com.kianirani.jarvis.router.backend

import com.kianirani.jarvis.router.health.AvailabilityGraph
import com.kianirani.jarvis.router.health.RateLimited
import com.kianirani.jarvis.router.orchestrator.DecisionObject
import com.kianirani.jarvis.router.registry.ModelBackend
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — executes an orchestrator [DecisionObject].
 *
 * Walks the ranked candidate list and dispatches each model to the [Backend] for
 * its kind, returning the first success. A failing candidate (no token, 429,
 * local model not downloaded, …) falls through to the next — this is the seed of
 * the Smart Substitution Engine (VB5): Vision never hard-fails while any
 * alternative remains.
 *
 * VB4: every candidate is first checked against the [AvailabilityGraph] and skipped
 * while its breaker is open, and each outcome (latency on success, `Retry-After` on a
 * [RateLimited] failure) is recorded — so a model that is rate-limited or repeatedly
 * failing is not hammered, and recovers automatically once its cooldown elapses.
 */
@Singleton
class BackendRouter internal constructor(
    private val backends: Map<ModelBackend, Backend>,
    private val graph: AvailabilityGraph = AvailabilityGraph(),
    private val now: () -> Long = { System.currentTimeMillis() },
) {
    @Inject constructor(
        cloud: CloudBackend,
        local: LocalBackend,
        mesh: MeshBackend,
        graph: AvailabilityGraph,
    ) : this(listOf<Backend>(cloud, local, mesh).associateBy { it.kind }, graph)

    suspend fun execute(decision: DecisionObject, message: String): Result<BackendReply> {
        if (decision.candidates.isEmpty()) {
            return Result.failure(IllegalStateException("NO_CANDIDATE — no reachable model for this request"))
        }
        var last: Throwable = IllegalStateException("all candidates failed")
        var skipped = false
        for (spec in decision.candidates) {
            val backend = backends[spec.backend] ?: continue
            if (!graph.isAvailable(spec)) {
                skipped = true
                continue // breaker open — cooling down, try the next candidate
            }
            val start = now()
            backend.generate(spec, message)
                .onSuccess {
                    graph.recordSuccess(spec, now() - start)
                    return Result.success(it)
                }
                .onFailure {
                    graph.recordFailure(spec, retryAfterMs = (it as? RateLimited)?.retryAfterMs)
                    last = it
                }
        }
        if (skipped) {
            last = IllegalStateException("ALL_COOLING_DOWN — every candidate's breaker is open", last)
        }
        return Result.failure(last)
    }
}
