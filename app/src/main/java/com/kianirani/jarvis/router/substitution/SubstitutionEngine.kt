package com.kianirani.jarvis.router.substitution

import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelRegistry
import com.kianirani.jarvis.router.registry.ModelSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — Module 4 (Smart Substitution Engine / VB5).
 *
 * Turns the orchestrator's ranked, reachable candidates into the concrete fallback
 * **chain** the [com.kianirani.jarvis.router.backend.BackendRouter] walks:
 * `Primary → F1 → F2 → F3 → Local → graceful`. It:
 *
 *  - **de-duplicates** by [ModelSpec.key] while preserving rank order,
 *  - **bounds cloud attempts** to [SubstitutionPolicy.maxCloudAttempts] so a long tail of
 *    cloud models can't blow up latency/cost (MESH/LOCAL are never capped),
 *  - optionally **drops MESH** (Privacy mode),
 *  - and **always appends the on-device model** as the terminal step (if one exists and
 *    isn't already present) so Vision never hard-fails while a substitute remains —
 *    the structural promise behind "it always answers".
 *
 * Pure and deterministic given the registry; VB4's [com.kianirani.jarvis.router.health.AvailabilityGraph]
 * then skips any link whose breaker is open at dispatch time.
 */
@Singleton
class SubstitutionEngine @Inject constructor(
    private val registry: ModelRegistry,
) {

    /** Build the ordered fallback chain for [ranked] under [policy]. */
    fun chain(
        ranked: List<ModelSpec>,
        policy: SubstitutionPolicy = SubstitutionPolicy.DEFAULT,
    ): List<ModelSpec> {
        val chain = LinkedHashMap<String, ModelSpec>()
        var cloudUsed = 0
        for (m in ranked) {
            if (chain.containsKey(m.key)) continue
            when (m.backend) {
                ModelBackend.CLOUD -> {
                    if (cloudUsed >= policy.maxCloudAttempts) continue
                    cloudUsed++
                }
                ModelBackend.MESH -> if (!policy.keepMesh) continue
                ModelBackend.LOCAL -> Unit // local always welcome
            }
            chain[m.key] = m
        }
        if (policy.alwaysAppendLocal && chain.values.none { it.isLocal }) {
            registry.byBackend(ModelBackend.LOCAL)
                .firstOrNull { it.key !in chain }
                ?.let { chain[it.key] = it }
        }
        return chain.values.toList()
    }
}
