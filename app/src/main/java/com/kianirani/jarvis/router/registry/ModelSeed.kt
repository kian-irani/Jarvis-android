package com.kianirani.jarvis.router.registry

import com.kianirani.jarvis.data.ai.AiProvider

/**
 * VISION BRAIN V1 — built-in seed for the [ModelRegistry].
 *
 * Conservative capability estimates for the providers Vision already supports,
 * plus the on-device local model. Scores are rough priors used only for
 * ranking ties; the AvailabilityGraph (VB4) corrects them with live health.
 * Override or extend any of these at runtime via [ModelRegistry.applyRemote].
 */
object ModelSeed {

    /** Local model recommended in the LM phase: Qwen2.5-0.5B-Instruct Q4 (Apache-2.0). */
    const val LOCAL_DEFAULT_ID = "qwen2.5-0.5b-instruct-q4"

    val LOCAL: ModelSpec = ModelSpec(
        id = LOCAL_DEFAULT_ID,
        displayName = "Vision Local (Qwen2.5 0.5B)",
        backend = ModelBackend.LOCAL,
        contextTokens = 4_096,
        scores = CapabilityScores(reasoning = 3, coding = 3, vision = 0, audio = 0, speed = 6, cost = 10),
        supportsTools = false,
        // Present even before download so routing can offer it; LM6 guards execution.
        enabled = true,
    )

    private fun cloud(
        provider: AiProvider,
        scores: CapabilityScores,
        contextTokens: Int,
        tools: Boolean = true,
        vision: Boolean = false,
    ) = ModelSpec(
        id = provider.defaultModel,
        displayName = "${provider.displayName} (${provider.defaultModel})",
        backend = ModelBackend.CLOUD,
        provider = provider.name,
        contextTokens = contextTokens,
        scores = scores,
        supportsTools = tools,
        supportsVision = vision,
    )

    /** Default catalogue: every cloud provider's default model + the local model. */
    fun defaults(): List<ModelSpec> = listOf(
        cloud(
            AiProvider.ANTHROPIC,
            CapabilityScores(reasoning = 9, coding = 9, vision = 8, audio = 0, speed = 6, cost = 4),
            contextTokens = 200_000, vision = true,
        ),
        cloud(
            AiProvider.OPENAI,
            CapabilityScores(reasoning = 8, coding = 8, vision = 7, audio = 0, speed = 7, cost = 6),
            contextTokens = 128_000, vision = true,
        ),
        cloud(
            AiProvider.GEMINI,
            CapabilityScores(reasoning = 8, coding = 7, vision = 8, audio = 6, speed = 8, cost = 7),
            contextTokens = 1_000_000, vision = true,
        ),
        cloud(
            AiProvider.XAI,
            CapabilityScores(reasoning = 7, coding = 7, vision = 0, audio = 0, speed = 7, cost = 6),
            contextTokens = 131_072,
        ),
        cloud(
            AiProvider.GROQ,
            CapabilityScores(reasoning = 7, coding = 7, vision = 0, audio = 0, speed = 10, cost = 8),
            contextTokens = 128_000,
        ),
        cloud(
            AiProvider.OPENROUTER,
            CapabilityScores(reasoning = 8, coding = 8, vision = 6, audio = 0, speed = 7, cost = 6),
            contextTokens = 128_000, vision = true,
        ),
        LOCAL,
    )
}
