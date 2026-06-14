package com.kianirani.jarvis.router.orchestrator

import com.kianirani.jarvis.router.capability.CapabilityRequest
import com.kianirani.jarvis.router.registry.Capability
import com.kianirani.jarvis.router.registry.ModelSpec

/**
 * VISION BRAIN V1 — Module 1 (Orchestrator) types.
 *
 * Vision *thinks before it acts*: every request is classified into an [Intent]
 * and an input [Modality], turned into a capability requirement, and only then
 * routed to a model. The output of that reasoning is a [DecisionObject], which
 * the HUD can also surface ("which model, and why" — VB9).
 */

/** The kind of work a message asks for. Drives which capability matters most. */
enum class Intent(val capability: Capability) {
    /** General conversation / Q&A. */
    CHAT(Capability.REASONING),

    /** Programming, debugging, config, regex, shell. */
    CODE(Capability.CODING),

    /** Hard multi-step reasoning / analysis / planning. */
    REASONING(Capability.REASONING),

    /** Short factual lookups where latency matters more than depth. */
    QUICK(Capability.SPEED),

    /** Understanding an image / screenshot. */
    IMAGE(Capability.VISION),

    /** Transcribing or reasoning over audio. */
    AUDIO(Capability.AUDIO),

    /** A request that should drive a device tool (needs tool calling). */
    ACTION(Capability.REASONING),
}

/** The form of the user's input. */
enum class Modality { TEXT, IMAGE, AUDIO }

/**
 * The result of the orchestrator's reasoning: what Vision concluded the task
 * is, what it needs, the reachable candidates in priority order, the model it
 * picked, and a human-readable [reason] for telemetry/debugging.
 */
data class DecisionObject(
    val intent: Intent,
    val modality: Modality,
    val request: CapabilityRequest,
    val candidates: List<ModelSpec>,
    val chosen: ModelSpec?,
    val reason: String,
) {
    val hasAnswerer: Boolean get() = chosen != null
}
