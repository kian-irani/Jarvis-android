package com.kianirani.jarvis.core.dashboard

/**
 * B5 — Personal Dashboard (PRD §, "Goals/Memory/Agents/Servers/Tasks/Automations/Plugins"). The
 * pure aggregation of Vision's at-a-glance counts into dashboard tiles the home/hub renders. Each
 * surface feeds its count; this builds the ordered, non-empty tile list + a one-line headline.
 * Pure → JVM-tested.
 */
data class DashboardCounts(
    val goals: Int = 0,
    val memories: Int = 0,
    val agents: Int = 0,
    val servers: Int = 0,
    val tasks: Int = 0,
    val automations: Int = 0,
    val plugins: Int = 0,
)

data class DashboardTile(val key: String, val label: String, val count: Int)

object DashboardModel {

    /** Tiles in display order; tiles with a zero count are dropped (no empty clutter). */
    fun tiles(c: DashboardCounts): List<DashboardTile> = listOf(
        DashboardTile("goals", "Goals", c.goals),
        DashboardTile("agents", "Agents", c.agents),
        DashboardTile("tasks", "Tasks", c.tasks),
        DashboardTile("automations", "Automations", c.automations),
        DashboardTile("memory", "Memory", c.memories),
        DashboardTile("plugins", "Plugins", c.plugins),
        DashboardTile("servers", "Servers", c.servers),
    ).filter { it.count > 0 }

    /** A one-line summary for the dashboard header. */
    fun headline(c: DashboardCounts): String {
        val active = tiles(c)
        if (active.isEmpty()) return "Nothing yet — start by asking Vision something."
        return active.joinToString(" · ") { "${it.count} ${it.label.lowercase()}" }
    }

    /** Total tracked items across all surfaces. */
    fun total(c: DashboardCounts): Int =
        c.goals + c.memories + c.agents + c.servers + c.tasks + c.automations + c.plugins
}
