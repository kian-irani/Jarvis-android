package com.kianirani.jarvis.core.proactive

/**
 * PAS — the proactive suggestion engine (PRD §, "موتورِ پیشنهادِ پیش‌دستانه … بدون مزاحمت").
 * Decides **which proactive suggestions to surface right now** from a pool of candidates
 * raised by monitoring (MON / Timeline) — a better-wording nudge, a reminder, an automation
 * offer — while guaranteeing Vision never nags: per-kind cooldowns and a global rate window.
 *
 * Pure & deterministic (the caller passes "now", the last-shown times, and the policy) →
 * JVM-tested. Gathering the candidates (Accessibility/notification/usage signals) and
 * actually rendering them in the overlay are the on-device half.
 */
data class Suggestion(
    val id: String,
    val kind: String,
    val text: String,
    /** 0f..1f confidence that this is worth interrupting the user for. */
    val relevance: Float,
    val createdAtMillis: Long,
)

/** Anti-nag knobs (user-tunable sensitivity). */
data class SuggestionPolicy(
    val minRelevance: Float = 0.5f,
    /** Don't resurface the same [Suggestion.kind] within this window. */
    val perKindCooldownMillis: Long = 30 * 60_000L,
    /** At most this many suggestions inside [windowMillis]. */
    val maxPerWindow: Int = 1,
    val windowMillis: Long = 60 * 60_000L,
)

object SuggestionEngine {

    /**
     * The suggestions to show now, highest-relevance first. Candidates are dropped when below
     * [SuggestionPolicy.minRelevance] or while their kind is still in cooldown; at most one per
     * kind is offered per tick; and the result is capped so the rolling window
     * ([recentlyShownAtMillis]) never exceeds [SuggestionPolicy.maxPerWindow]. Ties break by id.
     */
    fun select(
        candidates: List<Suggestion>,
        now: Long,
        lastShownByKind: Map<String, Long> = emptyMap(),
        recentlyShownAtMillis: List<Long> = emptyList(),
        policy: SuggestionPolicy = SuggestionPolicy(),
    ): List<Suggestion> {
        val shownInWindow = recentlyShownAtMillis.count { now - it < policy.windowMillis }
        val remaining = (policy.maxPerWindow - shownInWindow).coerceAtLeast(0)
        if (remaining == 0) return emptyList()

        return candidates.asSequence()
            .filter { it.relevance >= policy.minRelevance }
            .filter { s ->
                val last = lastShownByKind[s.kind]
                last == null || now - last >= policy.perKindCooldownMillis
            }
            .sortedWith(compareByDescending<Suggestion> { it.relevance }.thenBy { it.id })
            .distinctBy { it.kind } // at most one of each kind per tick
            .take(remaining)
            .toList()
    }
}
