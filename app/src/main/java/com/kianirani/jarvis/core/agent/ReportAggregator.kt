package com.kianirani.jarvis.core.agent

/**
 * AGT-DELEGATE — the structured-report core (PRD example: *"review all of mom's messages across
 * every app and give a full report"*). The agent collects raw items from many apps
 * (NotificationListener history, messengers via Accessibility, the Timeline/MON); this pure
 * step **groups and orders** them — by sender, across sources, newest-first — into a skeleton
 * the model then narrates. Keeping the structure pure makes the delegated report deterministic
 * and JVM-tested; gathering the items and writing the prose are the on-device/model half.
 */
data class CollectedItem(
    val source: String, // app/package, e.g. "whatsapp"
    val sender: String, // contact/handle, e.g. "mom"
    val text: String,
    val atMillis: Long,
)

/** Everything from one sender, across apps. */
data class SenderGroup(
    val sender: String,
    val count: Int,
    val sources: Set<String>,
    val items: List<CollectedItem>, // newest first
    val latestMillis: Long,
)

data class AggregatedReport(
    val totalItems: Int,
    val sources: Set<String>,
    val groups: List<SenderGroup>, // busiest sender first
)

object ReportAggregator {

    /**
     * Aggregate [items] into a report. When [filterSender] is given (case-insensitive), only
     * that person's items are included (the "all of mom's messages" case). Groups are ordered
     * by item count desc (then sender), and each group's items are newest-first.
     */
    fun aggregate(items: List<CollectedItem>, filterSender: String? = null): AggregatedReport {
        val needle = filterSender?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val scoped = if (needle == null) items else items.filter { it.sender.lowercase() == needle }

        val groups = scoped.groupBy { it.sender }
            .map { (sender, list) ->
                val sorted = list.sortedByDescending { it.atMillis }
                SenderGroup(
                    sender = sender,
                    count = list.size,
                    sources = list.map { it.source }.toSet(),
                    items = sorted,
                    latestMillis = sorted.firstOrNull()?.atMillis ?: 0L,
                )
            }
            .sortedWith(compareByDescending<SenderGroup> { it.count }.thenBy { it.sender })

        return AggregatedReport(
            totalItems = scoped.size,
            sources = scoped.map { it.source }.toSet(),
            groups = groups,
        )
    }
}
