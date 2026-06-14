package com.kianirani.jarvis.router.backend

import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — mesh [Backend]. Placeholder until the MX phase wires a real
 * `MeshModelClient` that posts inference to the chosen node's `/chat`. Fails
 * cleanly so the [BackendRouter] substitutes to the next candidate.
 */
@Singleton
class MeshBackend @Inject constructor() : Backend {
    override val kind = ModelBackend.MESH

    @Volatile var client: MeshClient? = null

    override suspend fun generate(spec: ModelSpec, message: String): Result<BackendReply> {
        val c = client ?: return Result.failure(
            IllegalStateException("MESH_BACKEND_UNAVAILABLE — no mesh model client wired (MX phase)"),
        )
        val node = spec.nodeId
            ?: return Result.failure(IllegalArgumentException("mesh model ${spec.id} has no nodeId"))
        return c.generate(node, spec.id, message).map { BackendReply(it, spec) }
    }

    /** Seam the MX phase implements. */
    fun interface MeshClient {
        suspend fun generate(nodeId: String, modelId: String, message: String): Result<String>
    }
}
