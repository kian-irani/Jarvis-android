package com.kianirani.jarvis.core.graph

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.coroutineContext

/**
 * VCF-G2 — the Pregel-style runner. [stream] runs the graph as a [Flow] of
 * [GraphEvent]s: each node returns an update applied via the pure reducer, edges (or an
 * explicit `goto`) pick the next node, and the loop is bounded by
 * [GraphState.remainingSteps] so cycles always terminate. The run is cancellable
 * (cooperatively) and, when a [Checkpointer] is supplied, persists after every step so
 * it survives Android process death and human-in-the-loop [NodeResult.Interrupt]s.
 */
class CompiledGraph(
    private val graph: VisionGraph,
    private val checkpointer: Checkpointer? = null,
) {
    fun stream(input: GraphState, threadId: String, ctx: NodeContext = NodeContext()): Flow<GraphEvent> = flow {
        var state = checkpointer?.load(threadId) ?: input
        var current = checkpointer?.loadCursor(threadId) ?: graph.entry
        while (current != END) {
            coroutineContext.ensureActive() // cooperative cancellation
            if (state.isExhausted) { // step budget spent → stop (bounds cycles)
                emit(GraphEvent.Done(state))
                return@flow
            }
            emit(GraphEvent.NodeStart(current))
            when (val result = graph.node(current).run(state, ctx)) {
                is NodeResult.Continue -> {
                    state = state.reduce(result.update)
                    val next = result.goto ?: graph.next(current, state)
                    checkpointer?.save(threadId, state, next) // durable: survive process death
                    emit(GraphEvent.NodeEnd(current, result.update))
                    current = next
                }
                is NodeResult.Interrupt -> {
                    checkpointer?.save(threadId, state, current) // resume re-enters the SAME node
                    emit(GraphEvent.Interrupted(result.reason, result.payload))
                    return@flow
                }
            }
        }
        emit(GraphEvent.Done(state))
    }.catch { e -> emit(GraphEvent.Failed(e.message ?: "graph failed", aborted = e is CancellationException)) }

    /**
     * Resume a run paused by an [NodeResult.Interrupt], feeding the user's [answer] into
     * state and marking the thread pre-approved so the re-entered gate proceeds.
     */
    fun resume(threadId: String, answer: VisionMessage, ctx: NodeContext = NodeContext()): Flow<GraphEvent> {
        val cp = requireNotNull(checkpointer) { "resume requires a checkpointer" }
        return flow {
            val cursor = cp.loadCursor(threadId) ?: graph.entry
            val base = requireNotNull(cp.load(threadId)) { "no checkpoint for thread '$threadId'" }
            val resumed = base.reduce(StateUpdate(appendMessages = listOf(answer)))
            cp.save(threadId, resumed, cursor) // persist the answer so stream picks it up
            emitAll(stream(resumed, threadId, ctx.copy(preApproved = ctx.preApproved + threadId)))
        }
    }
}
