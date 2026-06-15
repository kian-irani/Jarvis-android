package com.kianirani.jarvis.core.eval

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CF6 — per-turn feedback log.
 *
 * Records what the brain decided for each turn so the quality of routing can be
 * measured over time, and lets the user rate an answer 👍/👎. In-memory and
 * bounded (a ring buffer) — nothing sensitive is persisted to disk; it pairs
 * with the [EvalHarness] (CI golden-prompt regression) to close the loop on
 * "is Vision getting better or worse".
 *
 * Thread-safe (a single lock) and exposes [recent] as a [StateFlow] so a future
 * HUD panel can observe it. Wiring the live `record(...)` call into the chat
 * path is a follow-up that needs on-device verification.
 */
@Singleton
class FeedbackLog @Inject constructor() {

    /** Thumb rating for a turn. */
    enum class Rating { NONE, UP, DOWN }

    data class TurnRecord(
        val id: Long,
        val prompt: String,
        val intent: String,
        val model: String,
        val backend: String,
        val latencyMs: Long,
        val success: Boolean,
        val rating: Rating = Rating.NONE,
        val timestamp: Long = 0L,
    )

    data class Stats(
        val total: Int,
        val successes: Int,
        val thumbsUp: Int,
        val thumbsDown: Int,
    ) {
        /** Fraction of turns that produced an answer (0.0 when empty). */
        val successRate: Double get() = if (total == 0) 0.0 else successes.toDouble() / total
    }

    private val capacity = 200
    private val lock = Any()
    private val buffer = ArrayDeque<TurnRecord>()
    private var seq = 0L

    private val _recent = MutableStateFlow<List<TurnRecord>>(emptyList())
    val recent: StateFlow<List<TurnRecord>> = _recent

    /**
     * Record one completed turn. Returns the new record's id so the caller can
     * later attach a [rate] when the user taps 👍/👎.
     */
    fun record(
        prompt: String,
        intent: String,
        model: String,
        backend: String,
        latencyMs: Long,
        success: Boolean,
        now: Long = System.currentTimeMillis(),
    ): Long = synchronized(lock) {
        val id = ++seq
        buffer.addLast(
            TurnRecord(
                id = id,
                prompt = prompt,
                intent = intent,
                model = model,
                backend = backend,
                latencyMs = latencyMs,
                success = success,
                timestamp = now,
            ),
        )
        while (buffer.size > capacity) buffer.removeFirst()
        publish()
        id
    }

    /** Attach (or change) the user's rating for a recorded turn. No-op if unknown. */
    fun rate(id: Long, rating: Rating): Boolean = synchronized(lock) {
        val idx = buffer.indexOfFirst { it.id == id }
        if (idx < 0) return false
        buffer[idx] = buffer[idx].copy(rating = rating)
        publish()
        true
    }

    fun stats(): Stats = synchronized(lock) {
        Stats(
            total = buffer.size,
            successes = buffer.count { it.success },
            thumbsUp = buffer.count { it.rating == Rating.UP },
            thumbsDown = buffer.count { it.rating == Rating.DOWN },
        )
    }

    fun clear() = synchronized(lock) {
        buffer.clear()
        publish()
    }

    /** Most-recent-first snapshot for observers. */
    private fun publish() {
        _recent.value = buffer.toList().asReversed()
    }
}
