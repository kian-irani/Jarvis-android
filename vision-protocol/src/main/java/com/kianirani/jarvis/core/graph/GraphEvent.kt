package com.kianirani.jarvis.core.graph

import kotlinx.serialization.json.JsonObject

/**
 * VCF-G1 — the streamed lifecycle of a graph run (PRD §4.4, openclaw-style).
 *
 * The runner (VCF-G2) emits these as a `Flow<GraphEvent>` so any surface — HUD,
 * widget, remote desktop — observes node transitions, token deltas, tool traffic,
 * human-in-the-loop pauses, completion, and failure without coupling to internals.
 * Runtime-only (carries live state objects), so not serializable.
 */
sealed interface GraphEvent {
    /** A node is about to run. */
    data class NodeStart(val node: String) : GraphEvent

    /** A node finished and produced this update (pre-reduce). */
    data class NodeEnd(val node: String, val update: StateUpdate) : GraphEvent

    /** A streaming text delta from a model node. */
    data class Token(val delta: String) : GraphEvent

    /** A tool is about to execute. */
    data class ToolStart(val call: ContentPart.ToolCall) : GraphEvent

    /** A tool produced a result. */
    data class ToolEnd(val result: ContentPart.ToolResult) : GraphEvent

    /** The run paused for human input (HIL); [payload] carries the prompt context. */
    data class Interrupted(val reason: String, val payload: JsonObject) : GraphEvent

    /** The run completed; [state] is the final reduced state. */
    data class Done(val state: GraphState) : GraphEvent

    /** The run failed; [aborted] is true when cancelled rather than errored. */
    data class Failed(val message: String, val aborted: Boolean = false) : GraphEvent
}
