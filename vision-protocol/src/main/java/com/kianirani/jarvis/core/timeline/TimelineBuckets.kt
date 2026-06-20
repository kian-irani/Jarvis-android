package com.kianirani.jarvis.core.timeline

/**
 * A5 — Agent Timeline buckets (Agent-OS §, "Time-Machine رویدادها (Today/Yesterday/Last Week/
 * Month)"). Groups [TimelineEvent]s (MON1) into human time buckets for a "what happened when"
 * view. Pure date math over an injected day-start (so it's timezone-correct without pulling in
 * `java.time`/Android) → JVM-tested.
 */
enum class TimeBucket { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, OLDER }

object TimelineBuckets {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /**
     * Which bucket [atMillis] falls in, relative to [now] and the start of the current local day
     * [dayStartMillis]. TODAY = on/after today's start; YESTERDAY = the day before; THIS_WEEK =
     * within 7 days; THIS_MONTH = within 30 days; else OLDER. Future timestamps count as TODAY.
     */
    fun bucketOf(atMillis: Long, now: Long, dayStartMillis: Long): TimeBucket = when {
        atMillis >= dayStartMillis -> TimeBucket.TODAY
        atMillis >= dayStartMillis - DAY_MS -> TimeBucket.YESTERDAY
        atMillis >= now - 7 * DAY_MS -> TimeBucket.THIS_WEEK
        atMillis >= now - 30 * DAY_MS -> TimeBucket.THIS_MONTH
        else -> TimeBucket.OLDER
    }

    /**
     * Group [events] into ordered buckets (TODAY→OLDER); each bucket's events stay newest-first.
     * Empty buckets are omitted.
     */
    fun group(events: List<TimelineEvent>, now: Long, dayStartMillis: Long): Map<TimeBucket, List<TimelineEvent>> {
        val grouped = events
            .sortedByDescending { it.atMillis }
            .groupBy { bucketOf(it.atMillis, now, dayStartMillis) }
        // preserve enum order, drop empties
        return TimeBucket.entries.mapNotNull { b -> grouped[b]?.let { b to it } }.toMap()
    }
}
