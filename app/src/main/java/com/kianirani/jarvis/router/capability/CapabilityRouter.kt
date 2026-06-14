package com.kianirani.jarvis.router.capability

import com.kianirani.jarvis.data.ai.AiProviderStore
import com.kianirani.jarvis.router.registry.Capability
import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelRegistry
import com.kianirani.jarvis.router.registry.ModelSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What a task needs, expressed in capabilities — never in providers. The
 * Orchestrator (VB3) derives this from the user's request (intent + modality);
 * the router turns it into a ranked, *reachable* candidate list.
 */
data class CapabilityRequest(
    val primary: Capability = Capability.REASONING,
    val needsTools: Boolean = false,
    val needsVision: Boolean = false,
    val needsAudio: Boolean = false,
    /** Privacy/Offline mode forces on-device only (LM5). */
    val localOnly: Boolean = false,
)

/**
 * VISION BRAIN V1 — Module 2 (Capability Router).
 *
 * Ranks the [ModelRegistry] catalogue by the capability a task demands and
 * keeps only models that are actually reachable right now, instead of walking
 * a fixed provider enum. Reachability:
 *  - CLOUD: the provider must have at least one configured/baked token.
 *  - LOCAL: always offered here; LM6 guards whether the model is downloaded.
 *  - MESH:  offered if present in the registry; VB4 health refines further.
 *
 * The result is the ordered candidate list the Substitution Engine (VB5) walks
 * as its fallback chain.
 */
@Singleton
class CapabilityRouter @Inject constructor(
    private val registry: ModelRegistry,
    private val providerStore: AiProviderStore,
) {

    fun candidates(req: CapabilityRequest): List<ModelSpec> {
        if (req.localOnly) {
            return registry.byBackend(ModelBackend.LOCAL)
                .sortedByDescending { it.scores.score(req.primary) }
        }
        val configured = providerStore.configured().map { it.name }.toSet()
        return registry
            .rankedFor(req.primary, req.needsTools, req.needsVision, req.needsAudio)
            .filter { reachable(it, configured) }
    }

    /** The single best model for a request, or null if nothing is reachable. */
    fun best(req: CapabilityRequest): ModelSpec? = candidates(req).firstOrNull()

    private fun reachable(m: ModelSpec, configuredProviders: Set<String>): Boolean = when (m.backend) {
        ModelBackend.CLOUD -> m.provider in configuredProviders
        ModelBackend.LOCAL -> true
        ModelBackend.MESH -> true
    }
}
