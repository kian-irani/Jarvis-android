package com.kianirani.jarvis.router.orchestrator

import com.kianirani.jarvis.router.capability.CapabilityRequest
import com.kianirani.jarvis.router.capability.CapabilityRouter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — Module 1 (Orchestrator Brain).
 *
 * The "think before act" stage. Sits between the user's request and the model
 * backends: classifies the request, decides what capability it needs, asks the
 * [CapabilityRouter] for reachable candidates, and emits a [DecisionObject].
 *
 * It deliberately does NOT call a model — that is VB8's job. Keeping the
 * decision pure makes it unit-testable and lets the HUD show the reasoning
 * (VB9) before any token is spent.
 */
@Singleton
class VisionOrchestrator @Inject constructor(
    private val classifier: IntentClassifier,
    private val router: CapabilityRouter,
) {

    fun decide(
        message: String,
        hasImage: Boolean = false,
        isVoice: Boolean = false,
        privacyMode: Boolean = false,
    ): DecisionObject {
        val (intent, modality) = classifier.classify(message, hasImage, isVoice)
        val request = CapabilityRequest(
            primary = intent.capability,
            needsTools = intent == Intent.ACTION,
            needsVision = intent == Intent.IMAGE,
            needsAudio = intent == Intent.AUDIO,
            localOnly = privacyMode,
        )
        val candidates = router.candidates(request)
        val chosen = candidates.firstOrNull()
        return DecisionObject(
            intent = intent,
            modality = modality,
            request = request,
            candidates = candidates,
            chosen = chosen,
            reason = buildReason(intent, modality, privacyMode, chosen, candidates.size),
        )
    }

    private fun buildReason(
        intent: Intent,
        modality: Modality,
        privacyMode: Boolean,
        chosen: com.kianirani.jarvis.router.registry.ModelSpec?,
        count: Int,
    ): String {
        val mode = if (privacyMode) " [privacy: local-only]" else ""
        val pick = chosen?.let { "→ ${it.displayName} (${it.backend})" }
            ?: "→ no reachable model (add a token or download the local model)"
        return "intent=$intent modality=$modality need=${intent.capability}$mode " +
            "$pick from $count candidate(s)"
    }
}
