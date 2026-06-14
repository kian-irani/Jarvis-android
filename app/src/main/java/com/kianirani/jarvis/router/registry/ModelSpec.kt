package com.kianirani.jarvis.router.registry

import kotlinx.serialization.Serializable

/**
 * VISION BRAIN V1 — Module 3 (Model Registry).
 *
 * Where a model physically runs. Vision depends on *capabilities*, not on any
 * one provider; the backend just says how to reach a model that has them.
 */
@Serializable
enum class ModelBackend {
    /** Reached over the internet via a token in [com.kianirani.jarvis.data.ai.AiProviderStore]. */
    CLOUD,

    /** Runs on this phone (LM phase — llama.cpp / MediaPipe GGUF). */
    LOCAL,

    /** Runs on another mesh node (server / peer device) — reached over the LAN/WAN brain. */
    MESH,
}

/** A single capability axis a task can demand. Scores are 0..10 (higher = stronger). */
@Serializable
enum class Capability { REASONING, CODING, VISION, AUDIO, SPEED, COST }

/**
 * Capability scorecard for a model. [cost] is *inverted* — 10 means "essentially
 * free", 0 means "very expensive" — so the router can treat every axis as
 * "higher is better" when ranking.
 */
@Serializable
data class CapabilityScores(
    val reasoning: Int = 5,
    val coding: Int = 5,
    val vision: Int = 0,
    val audio: Int = 0,
    val speed: Int = 5,
    val cost: Int = 5,
) {
    fun score(c: Capability): Int = when (c) {
        Capability.REASONING -> reasoning
        Capability.CODING -> coding
        Capability.VISION -> vision
        Capability.AUDIO -> audio
        Capability.SPEED -> speed
        Capability.COST -> cost
    }
}

/**
 * Metadata for one model the router can choose. Seeded for the current cloud
 * providers, the on-device local model, and mesh models; the whole list is
 * also patchable from a remote JSON document so a new model can be added
 * without shipping an app update (master-spec requirement).
 *
 * @param id          backend-specific model id (e.g. "claude-sonnet-4-6", "qwen2.5-0.5b").
 * @param backend     where it runs ([ModelBackend]).
 * @param provider    [com.kianirani.jarvis.data.ai.AiProvider] name when [backend] == CLOUD; null otherwise.
 * @param nodeId      mesh node id when [backend] == MESH; null otherwise.
 * @param contextTokens approximate usable context window.
 * @param scores      capability scorecard used for ranking.
 * @param supportsTools function/tool calling.
 * @param enabled     a model the user/health-graph disabled is kept but skipped.
 */
@Serializable
data class ModelSpec(
    val id: String,
    val displayName: String,
    val backend: ModelBackend,
    val provider: String? = null,
    val nodeId: String? = null,
    val contextTokens: Int = 8_192,
    val scores: CapabilityScores = CapabilityScores(),
    val supportsTools: Boolean = false,
    val supportsVision: Boolean = false,
    val supportsAudio: Boolean = false,
    val enabled: Boolean = true,
) {
    val isLocal: Boolean get() = backend == ModelBackend.LOCAL

    /** Stable key (backend + id + node) so the same cloud model on two nodes can't collide. */
    val key: String get() = "${backend.name}:${nodeId ?: provider ?: "-"}:$id"
}
