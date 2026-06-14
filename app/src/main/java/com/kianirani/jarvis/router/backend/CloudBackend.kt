package com.kianirani.jarvis.router.backend

import com.kianirani.jarvis.data.ai.AiProvider
import com.kianirani.jarvis.data.ai.CloudChatRouter
import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — cloud [Backend]. Runs a [ModelSpec] whose provider has a
 * configured/baked token by delegating to [CloudChatRouter.chatWith]. Because
 * this goes through `AiProviderStore`, the user's own tokens are finally used —
 * fixing the bug where the brain-first gate only ever hit Brain-Lite's Groq key.
 */
@Singleton
class CloudBackend @Inject constructor(
    private val cloud: CloudChatRouter,
) : Backend {
    override val kind = ModelBackend.CLOUD

    override suspend fun generate(spec: ModelSpec, message: String): Result<BackendReply> {
        val providerName = spec.provider
            ?: return Result.failure(IllegalArgumentException("cloud model ${spec.id} has no provider"))
        val provider = runCatching { AiProvider.valueOf(providerName) }
            .getOrElse { return Result.failure(IllegalArgumentException("unknown provider $providerName")) }
        return cloud.chatWith(provider, message).map { BackendReply(it.text, spec) }
    }
}
