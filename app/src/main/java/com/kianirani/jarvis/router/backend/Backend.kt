package com.kianirani.jarvis.router.backend

import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelSpec

/** A successful generation, tagged with the model that produced it (for VB9 telemetry). */
data class BackendReply(val text: String, val model: ModelSpec)

/**
 * VISION BRAIN V1 — Module 5 (Provider Router / backend abstraction).
 *
 * One uniform way to run a model regardless of where it lives. Cloud, on-device
 * and mesh each implement this; the orchestrator decides *which* [ModelSpec] to
 * run and the [BackendRouter] dispatches it here. This is what lets Vision
 * "depend on capabilities, not providers" — and it is the structural fix for the
 * old brain-first gate that bypassed the user's tokens (BUG-AI).
 */
interface Backend {
    /** Which backend kind this implementation serves. */
    val kind: ModelBackend

    /** Generate a reply for [message] using [spec]; failure lets the router substitute. */
    suspend fun generate(spec: ModelSpec, message: String): Result<BackendReply>
}
