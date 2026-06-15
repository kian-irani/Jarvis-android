package com.kianirani.jarvis.router.substitution

/**
 * VISION BRAIN V1 — Module 4 (Smart Substitution Engine / VB5) configuration.
 *
 * Shapes the fallback chain the [SubstitutionEngine] builds from the orchestrator's
 * ranked candidates. The defaults encode the master-spec chain
 * `Primary → F1 → F2 → F3 → Local → graceful`: try a bounded number of cloud models
 * for speed/cost, then always fall back to the on-device model so Vision still answers
 * with zero tokens or no connectivity.
 *
 * @param maxCloudAttempts how many CLOUD models the chain may include before it stops
 *        adding more (bounds latency and spend); MESH/LOCAL are never capped.
 * @param alwaysAppendLocal append the best on-device model as a last resort even if it
 *        was not among the ranked candidates — the anti-"Vision went silent" guarantee.
 * @param keepMesh include reachable MESH models in the chain (Privacy mode may drop them).
 */
data class SubstitutionPolicy(
    val maxCloudAttempts: Int = 4,
    val alwaysAppendLocal: Boolean = true,
    val keepMesh: Boolean = true,
) {
    companion object {
        val DEFAULT = SubstitutionPolicy()

        /** Privacy/Offline: on-device only, no cloud or mesh hops. */
        val LOCAL_ONLY = SubstitutionPolicy(maxCloudAttempts = 0, alwaysAppendLocal = true, keepMesh = false)
    }
}
