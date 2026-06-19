package com.kianirani.jarvis.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-L1 acceptance: the grouper clusters by category, drops singleton categories, orders
 * folders by aggregate usage, and sorts members by usage. Pure, no device.
 */
class AppGrouperTest {

    private fun app(id: String, cat: AppGroupCategory, usage: Float = 0f) =
        GroupableApp(id, id, cat, usage)

    @Test fun `apps cluster into named context folders by category`() {
        val groups = AppGrouper.group(
            listOf(
                app("whatsapp", AppGroupCategory.COMMUNICATION),
                app("telegram", AppGroupCategory.COMMUNICATION),
                app("docs", AppGroupCategory.PRODUCTIVITY),
                app("sheets", AppGroupCategory.PRODUCTIVITY),
            ),
        )
        assertEquals(setOf("Social", "Work"), groups.map { it.name }.toSet())
        assertEquals(setOf("whatsapp", "telegram"), groups.first { it.name == "Social" }.appIds.toSet())
    }

    @Test fun `a category with fewer than minGroupSize apps is not foldered`() {
        val groups = AppGrouper.group(
            listOf(
                app("spotify", AppGroupCategory.MEDIA),
                app("camera", AppGroupCategory.TOOLS),
                app("files", AppGroupCategory.TOOLS),
            ),
        )
        assertEquals(listOf("Tools"), groups.map { it.name })
        assertEquals(listOf("spotify"), AppGrouper.ungrouped(
            listOf(
                app("spotify", AppGroupCategory.MEDIA),
                app("camera", AppGroupCategory.TOOLS),
                app("files", AppGroupCategory.TOOLS),
            ),
        ))
    }

    @Test fun `folders are ordered by aggregate usage descending`() {
        val groups = AppGrouper.group(
            listOf(
                app("a", AppGroupCategory.TOOLS, 1f),
                app("b", AppGroupCategory.TOOLS, 1f),
                app("c", AppGroupCategory.COMMUNICATION, 50f),
                app("d", AppGroupCategory.COMMUNICATION, 40f),
            ),
        )
        assertEquals(listOf("Social", "Tools"), groups.map { it.name }) // Social usage 90 > Tools 2
    }

    @Test fun `apps within a folder are sorted by usage descending`() {
        val groups = AppGrouper.group(
            listOf(
                app("low", AppGroupCategory.MEDIA, 2f),
                app("high", AppGroupCategory.MEDIA, 99f),
                app("mid", AppGroupCategory.MEDIA, 50f),
            ),
        )
        assertEquals(listOf("high", "mid", "low"), groups.single().appIds)
    }

    @Test fun `empty input yields no groups and no ungrouped`() {
        assertTrue(AppGrouper.group(emptyList()).isEmpty())
        assertTrue(AppGrouper.ungrouped(emptyList()).isEmpty())
    }

    @Test fun `equal aggregate usage keeps category declaration order`() {
        val groups = AppGrouper.group(
            listOf(
                app("m1", AppGroupCategory.MEDIA, 5f),
                app("m2", AppGroupCategory.MEDIA, 5f),
                app("c1", AppGroupCategory.COMMUNICATION, 5f),
                app("c2", AppGroupCategory.COMMUNICATION, 5f),
            ),
        )
        // COMMUNICATION is declared before MEDIA -> Social before Media on a tie
        assertEquals(listOf("Social", "Media"), groups.map { it.name })
    }
}
