package com.kianirani.jarvis.core.eval

import com.kianirani.jarvis.core.graph.GraphEvent
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * VCF-E1 — an observable record of one agent run (PRD §12): the ordered [GraphEvent]s plus
 * derived views (which nodes ran, how it ended, the final state, how many tools actually
 * executed). Feeds the HUD/VB9 trace view and time-travel debugging. Pure → unit-tested.
 */
data class RunTrace(val threadId: String, val events: List<GraphEvent>) {
    val nodeVisits: List<String> get() = events.filterIsInstance<GraphEvent.NodeStart>().map { it.node }
    val stepCount: Int get() = events.count { it is GraphEvent.NodeEnd }
    val completed: Boolean get() = events.lastOrNull() is GraphEvent.Done
    val interrupted: Boolean get() = events.lastOrNull() is GraphEvent.Interrupted
    val failed: Boolean get() = events.lastOrNull() is GraphEvent.Failed
    val finalState: GraphState? get() = (events.lastOrNull() as? GraphEvent.Done)?.state
    val toolRuns: Int get() = finalState?.messages?.count { it.role == Role.TOOL } ?: 0
}

/** Collects a run's event stream into a [RunTrace] for inspection/eval. */
object TraceRecorder {
    suspend fun record(threadId: String, events: Flow<GraphEvent>): RunTrace =
        RunTrace(threadId, events.toList())

    fun of(threadId: String, events: List<GraphEvent>): RunTrace = RunTrace(threadId, events)
}
