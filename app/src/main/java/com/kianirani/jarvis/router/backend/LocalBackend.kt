package com.kianirani.jarvis.router.backend

import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — on-device [Backend]. Placeholder until the LM phase wires a
 * real `LocalModelEngine` (llama.cpp / MediaPipe over a downloaded GGUF). Until
 * then it fails cleanly so the [BackendRouter] substitutes to the next candidate
 * (LM6 graceful-no-model), never hanging or crashing.
 */
@Singleton
class LocalBackend @Inject constructor() : Backend {
    override val kind = ModelBackend.LOCAL

    @Volatile var engine: LocalEngine? = null

    override suspend fun generate(spec: ModelSpec, message: String): Result<BackendReply> {
        val e = engine ?: return Result.failure(
            IllegalStateException("LOCAL_MODEL_UNAVAILABLE — on-device model not downloaded yet"),
        )
        return e.generate(message).map { BackendReply(it, spec) }
    }

    /** Minimal seam the LM phase implements; kept here so wiring lands without churn. */
    fun interface LocalEngine {
        suspend fun generate(message: String): Result<String>
    }
}
