package com.kianirani.jarvis.router.registry

import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — Module 3 (Model Registry).
 *
 * The single catalogue of every model Vision can route to: cloud provider
 * models, the on-device local model, and mesh-node models. The Capability
 * Router (VB2) and Orchestrator (VB3) query this instead of iterating a fixed
 * provider enum, which is the whole point — "Vision depends on capabilities,
 * not providers."
 *
 * Seeded from [ModelSeed]; mesh models are added/removed at runtime as nodes
 * appear (MX phase); the catalogue is also patchable from a remote JSON
 * document so a new model needs no app update.
 *
 * Thread-safe: backed by a [ConcurrentHashMap] keyed by [ModelSpec.key].
 */
@Singleton
class ModelRegistry @Inject constructor() {

    private val models = ConcurrentHashMap<String, ModelSpec>()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init {
        ModelSeed.defaults().forEach { models[it.key] = it }
    }

    /** Every model in the catalogue, in stable key order. */
    fun all(): List<ModelSpec> = models.values.sortedBy { it.key }

    /** Enabled models only — what the router actually considers. */
    fun enabled(): List<ModelSpec> = models.values.filter { it.enabled }.sortedBy { it.key }

    fun byKey(key: String): ModelSpec? = models[key]

    /** Models on a given backend (e.g. only LOCAL when in Privacy Mode). */
    fun byBackend(backend: ModelBackend): List<ModelSpec> =
        enabled().filter { it.backend == backend }

    /**
     * Enabled models that satisfy a hard requirement, ranked by [primary]
     * capability (desc), then by the rest of the scorecard, then by id for
     * determinism. This is the raw candidate list VB2 refines with live health.
     */
    fun rankedFor(
        primary: Capability,
        requireTools: Boolean = false,
        requireVision: Boolean = false,
        requireAudio: Boolean = false,
    ): List<ModelSpec> = enabled()
        .filter { (!requireTools || it.supportsTools) }
        .filter { (!requireVision || it.supportsVision) }
        .filter { (!requireAudio || it.supportsAudio) }
        .sortedWith(
            compareByDescending<ModelSpec> { it.scores.score(primary) }
                .thenByDescending { it.scores.reasoning + it.scores.coding }
                .thenByDescending { it.scores.speed }
                .thenBy { it.id },
        )

    /** Add or replace a model (mesh advertisement, local download completing, etc.). */
    fun upsert(spec: ModelSpec) { models[spec.key] = spec }

    /** Remove a model by key (mesh node went offline). Returns true if present. */
    fun remove(key: String): Boolean = models.remove(key) != null

    /** Drop every mesh model for [nodeId] (node left the mesh). */
    fun removeNode(nodeId: String) {
        models.values.filter { it.nodeId == nodeId }.forEach { models.remove(it.key) }
    }

    fun setEnabled(key: String, enabled: Boolean) {
        models[key]?.let { models[it.key] = it.copy(enabled = enabled) }
    }

    /**
     * Merge a remote catalogue. Each [ModelSpec] in the JSON array is upserted by
     * [ModelSpec.key], so the document can add new models or override the scores
     * of seeded ones without an app release. Malformed JSON is ignored (the seed
     * stays intact) and the parse outcome is returned for telemetry.
     */
    fun applyRemote(jsonText: String): Result<Int> = runCatching {
        val specs = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(ModelSpec.serializer()),
            jsonText,
        )
        specs.forEach { models[it.key] = it }
        specs.size
    }
}
