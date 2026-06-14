package com.kianirani.jarvis.router.backend

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
 */
@Singleton
class BackendRouter internal constructor(
    private val backends: Map<ModelBackend, Backend>,
) {
    @Inject constructor(
        cloud: CloudBackend,
        local: LocalBackend,
        mesh: MeshBackend,
    ) : this(listOf<Backend>(cloud, local, mesh).associateBy { it.kind })

    suspend fun execute(decision: DecisionObject, message: String): Result<BackendReply> {
        if (decision.candidates.isEmpty()) {
            return Result.failure(IllegalStateException("NO_CANDIDATE — no reachable model for this request"))
        }
        var last: Throwable = IllegalStateException("all candidates failed")
        for (spec in decision.candidates) {
            val backend = backends[spec.backend] ?: continue
            backend.generate(spec, message)
                .onSuccess { return Result.success(it) }
                .onFailure { last = it }
        }
        return Result.failure(last)
    }
}
