package com.kianirani.jarvis.router.backend

import com.kianirani.jarvis.router.local.LocalModelCatalog
import com.kianirani.jarvis.router.local.LocalModelEngine
import com.kianirani.jarvis.router.local.LocalModelManager
import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelSpec

/**
 * LM1 — the LOCAL [Backend]: runs a request on the phone's own model via a [LocalModelEngine].
 * Finds the downloaded GGUF through [LocalModelManager], loads it on first use, and generates.
 * With the default [com.kianirani.jarvis.router.local.NoOpLocalModelEngine] (no native lib) it
 * fails fast so the [BackendRouter] substitutes to cloud/mesh — Vision is never silent (LM6).
 * Swapping in a real JNI engine makes on-device inference live with no router changes.
 */
class LocalInferenceBackend(
    private val engine: LocalModelEngine,
    private val models: LocalModelManager,
) : Backend {

    override val kind: ModelBackend = ModelBackend.LOCAL

    override suspend fun generate(spec: ModelSpec, message: String): Result<BackendReply> {
        val descriptor = LocalModelCatalog.all.firstOrNull { it.id == spec.id } ?: LocalModelCatalog.default
        val file = models.fileFor(descriptor)
        if (!file.exists()) {
            return Result.failure(IllegalStateException("on-device model '${descriptor.id}' not downloaded"))
        }
        if (!engine.isLoaded && !engine.load(file.absolutePath)) {
            return Result.failure(IllegalStateException("on-device model engine unavailable"))
        }
        return engine.generate(message).map { BackendReply(it, spec) }
    }
}
