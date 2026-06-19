package com.kianirani.jarvis.core.timeline

/**
 * MON1 — the activity log / Vision Timeline (PRD Phase 6). An in-memory, bounded record of
 * what happened on the device — apps opened, calls, messages, notifications — with structured
 * [query] over type, time range, source, and text. This is the pure core (newest-first ring +
 * filtering) → JVM-tested; persisting to Room and feeding it from the real
 * NotificationListener/UsageStats/CallLog observers (MON2) is the on-device half. Privacy
 * (MON3) gates whether anything is recorded at all.
 */
enum class ActivityType { APP_OPEN, CALL, MESSAGE, NOTIFICATION, CUSTOM }

data class TimelineEvent(
    val id: String,
    val type: ActivityType,
    val atMillis: Long,
    val title: String,
    /** App package or contact the event came from. */
    val source: String = "",
    val detail: String = "",
)

class Timeline(private val capacity: Int = DEFAULT_CAPACITY) {

    private val events = ArrayDeque<TimelineEvent>() // newest at index 0

    /** Record an event. Keeps newest-first order and caps at [capacity] (oldest dropped). */
    fun record(event: TimelineEvent) {
        events.addFirst(event)
        while (events.size > capacity) events.removeLast()
    }

    /** All events, newest first. */
    fun all(): List<TimelineEvent> = events.toList()

    /** The [n] most recent events. */
    fun recent(n: Int): List<TimelineEvent> = events.take(n.coerceAtLeast(0))

    /** Per-type counts (for an at-a-glance summary). */
    fun countByType(): Map<ActivityType, Int> = events.groupingBy { it.type }.eachCount()

    /**
     * Filtered, newest-first query. Any argument left null is unconstrained; [text] matches the
     * title or detail case-insensitively; [since]/[until] bound the time range (inclusive).
     * [limit] ≤ 0 returns all matches.
     */
    fun query(
        type: ActivityType? = null,
        since: Long? = null,
        until: Long? = null,
        source: String? = null,
        text: String? = null,
        limit: Int = 0,
    ): List<TimelineEvent> {
        val needle = text?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val matches = events.filter { e ->
            (type == null || e.type == type) &&
                (since == null || e.atMillis >= since) &&
                (until == null || e.atMillis <= until) &&
                (source == null || e.source == source) &&
                (needle == null || e.title.lowercase().contains(needle) || e.detail.lowercase().contains(needle))
        }
        return if (limit > 0) matches.take(limit) else matches
    }

    companion object {
        const val DEFAULT_CAPACITY = 500
    }
}
