package com.kianirani.jarvis.core.sdk

import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.protocol.VisionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

/**
 * DS-F3 — projects the runtime [GraphEvent] stream (VCF-G1, deliberately *not*
 * serializable — it carries live state) into the protocol [VisionResponse] (DS-F2,
 * wire-ready). This is the seam that makes the SDK transport-agnostic: an in-process
 * surface and a remote/desktop surface over the network plane (DS-C2) both observe the
 * same protocol responses.
 *
 * Pure & deterministic — [fold] over a completed event list is the unit-testable core;
 * [asResponses] applies the exact same step logic as a streaming Flow operator (fresh
 * accumulator per collection, so it's safe to collect more than once).
 *
 * Mapping: model token deltas accumulate into cumulative [VisionResponse.text]
 * (`finished=false`); `Interrupted` → an unfinished response `awaitingConfirmation` (HIL);
 * `Done` → the final response (streamed text, else the last assistant message); `Failed`
 * → a finished response carrying the error. Internal node/tool events emit nothing.
 */
object ResponseProjector {

    private class Acc(val sessionId: String) {
        private val buf = StringBuilder()

        fun step(event: GraphEvent): VisionResponse? = when (event) {
            is GraphEvent.Token -> {
                buf.append(event.delta)
                VisionResponse(sessionId, text = buf.toString(), finished = false)
            }
            is GraphEvent.Interrupted ->
                VisionResponse(sessionId, text = buf.toString(), finished = false, awaitingConfirmation = true)
            is GraphEvent.Done ->
                VisionResponse(sessionId, text = buf.toString().ifEmpty { lastAssistantText(event.state) }, finished = true)
            is GraphEvent.Failed ->
                VisionResponse(sessionId, text = buf.toString(), finished = true, error = event.message)
            else -> null // NodeStart / NodeEnd / ToolStart / ToolEnd are internal
        }
    }

    /** Deterministic projection of a completed event list (the unit-test core). */
    fun fold(sessionId: String, events: List<GraphEvent>): List<VisionResponse> {
        val acc = Acc(sessionId)
        return events.mapNotNull { acc.step(it) }
    }

    /** The same projection as a streaming Flow operator (fresh state per collection). */
    fun Flow<GraphEvent>.asResponses(sessionId: String): Flow<VisionResponse> {
        val upstream = this
        return flow {
            val acc = Acc(sessionId)
            upstream.collect { event -> acc.step(event)?.let { emit(it) } }
        }
    }

    private fun lastAssistantText(state: GraphState): String =
        state.messages.lastOrNull { it.role == Role.ASSISTANT }?.text() ?: ""
}
