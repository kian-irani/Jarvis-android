package com.kianirani.jarvis.core.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** B5 acceptance: tiles drop zero counts, ordered; headline + total summarize. Pure. */
class DashboardModelTest {

    @Test fun `tiles drop zero counts and keep order`() {
        val tiles = DashboardModel.tiles(DashboardCounts(goals = 2, tasks = 5, plugins = 1))
        assertEquals(listOf("goals", "tasks", "plugins"), tiles.map { it.key })
    }

    @Test fun `headline lists active tiles`() {
        val h = DashboardModel.headline(DashboardCounts(agents = 3, memories = 10))
        assertTrue(h.contains("3 agents"))
        assertTrue(h.contains("10 memory"))
    }

    @Test fun `empty dashboard has a friendly headline`() {
        assertTrue(DashboardModel.headline(DashboardCounts()).contains("Nothing yet"))
        assertTrue(DashboardModel.tiles(DashboardCounts()).isEmpty())
    }

    @Test fun `total sums everything`() {
        assertEquals(11, DashboardModel.total(DashboardCounts(goals = 1, tasks = 4, agents = 2, plugins = 4)))
    }
}
