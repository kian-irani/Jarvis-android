package com.kianirani.jarvis.core.notif

/**
 * NEO12 — notification dots on the home grid. Turns per-app unread counts (from the
 * NotificationListener, MON2) into the badge model the launcher draws on each app icon: a dot
 * when there's anything unread and a capped count label. Pure → JVM-tested; the listener that
 * feeds the counts and the Compose overlay that paints the dot are the on-device half.
 */
data class NotificationBadge(val packageName: String, val count: Int)

object NotificationBadges {

    const val DEFAULT_MAX = 99

    /** Badges for apps with at least one unread, most unread first (ties by package). */
    fun from(counts: Map<String, Int>): List<NotificationBadge> =
        counts.filter { it.value > 0 }
            .map { NotificationBadge(it.key, it.value) }
            .sortedWith(compareByDescending<NotificationBadge> { it.count }.thenBy { it.packageName })

    /** Whether a given package should show a dot at all. */
    fun hasDot(packageName: String, counts: Map<String, Int>): Boolean = (counts[packageName] ?: 0) > 0

    /** The label drawn in the badge: "" when zero, the number, or "<max>+" when over [max]. */
    fun display(count: Int, max: Int = DEFAULT_MAX): String = when {
        count <= 0 -> ""
        count > max -> "$max+"
        else -> count.toString()
    }

    /** Total unread across all apps (for an at-a-glance summary). */
    fun total(counts: Map<String, Int>): Int = counts.values.filter { it > 0 }.sum()
}
