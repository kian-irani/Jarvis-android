package com.kianirani.jarvis.core.memory

import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage

/**
 * DS-B3 — the short-term half of "memory deepen" (PRD §11). Holds the live conversation as
 * a bounded window of turns and, once it grows past [maxTurns], hands the oldest turns off
 * to be compressed into a running summary — keeping the prompt small without losing the
 * gist of a long session. This is the stateful window manager; the actual compression is
 * done by [ConversationSummarizer] (model-backed) and the durable long-term store is
 * [MemoryEngine]. Splitting them keeps this class pure and deterministic → JVM-tested.
 *
 * Flow the caller runs after each turn:
 * ```
 * session.add(turn)
 * if (session.needsCompaction()) {
 *     val summary = summarizer.summarize(session.compactable())   // model call
 *     session.applySummary(summary)                               // drops compacted turns
 *     summary.takeIf { it.isNotBlank() }?.let { memoryEngine.remember(it, MemoryType.SEMANTIC) }
 * }
 * val prompt = session.window()   // running summary + recent turns
 * ```
 *
 * Graceful: a blank summary (the summarizer returns "" on failure or when not needed) is a
 * no-op — turns are never dropped without a summary that preserves them.
 */
class SessionMemory(
    val maxTurns: Int = DEFAULT_MAX_TURNS,
    val keepRecent: Int = DEFAULT_KEEP_RECENT,
) {
    init {
        require(maxTurns > keepRecent) { "maxTurns ($maxTurns) must exceed keepRecent ($keepRecent)" }
        require(keepRecent > 0) { "keepRecent must be positive" }
    }

    private val turns = ArrayDeque<VisionMessage>()
    private var runningSummary: String? = null

    /** Append a turn to the live window. */
    fun add(message: VisionMessage) {
        turns.addLast(message)
    }

    /** Number of turns currently held in the live window (excludes the summary). */
    fun size(): Int = turns.size

    /** The running summary of compacted (older) turns, or null until the first compaction. */
    fun summary(): String? = runningSummary

    /** True once the window has grown past [maxTurns] and should be compacted. */
    fun needsCompaction(): Boolean = turns.size > maxTurns

    /**
     * The oldest turns eligible for summarization — everything beyond the most recent
     * [keepRecent] — but only when [needsCompaction]; otherwise empty. Pass this to the
     * summarizer.
     */
    fun compactable(): List<VisionMessage> =
        if (!needsCompaction()) emptyList() else turns.take(turns.size - keepRecent)

    /**
     * Fold [newSummary] into the running summary and drop the compacted turns, leaving the
     * most recent [keepRecent]. A blank summary is ignored (turns are preserved) so a failed
     * or skipped summarization never loses context.
     */
    fun applySummary(newSummary: String) {
        if (newSummary.isBlank() || !needsCompaction()) return
        repeat(turns.size - keepRecent) { turns.removeFirst() }
        val trimmed = newSummary.trim()
        runningSummary = if (runningSummary.isNullOrBlank()) trimmed else "$runningSummary\n$trimmed"
    }

    /**
     * The prompt window: the running summary as a leading SYSTEM message (when present)
     * followed by the recent turns. This is what gets sent to the model.
     */
    fun window(): List<VisionMessage> {
        val s = runningSummary
        val head = if (!s.isNullOrBlank()) {
            listOf(VisionMessage.text(Role.SYSTEM, "Earlier in this conversation: $s"))
        } else {
            emptyList()
        }
        return head + turns
    }

    /** Reset the session (e.g. user cleared the conversation). */
    fun clear() {
        turns.clear()
        runningSummary = null
    }

    companion object {
        const val DEFAULT_MAX_TURNS = 20
        const val DEFAULT_KEEP_RECENT = 8
    }
}
