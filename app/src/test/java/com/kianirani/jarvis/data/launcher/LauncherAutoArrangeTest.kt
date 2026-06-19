package com.kianirani.jarvis.data.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-L3 "Optimize home" acceptance: autoArrange groups top-level workspace apps into
 * category folders (≥2 per category), leaving the rest untouched. Pure, no device.
 */
class LauncherAutoArrangeTest {

    private fun app(id: String, cat: String, x: Int, y: Int) =
        LauncherItem(id = id, type = ItemType.APP, container = Container.WORKSPACE, page = 0, cellX = x, cellY = y, packageName = "pkg.$id", label = id)

    private val layout = LauncherLayout(gridCols = 5, gridRows = 5, pageCount = 1)
    private val catOf: Map<String, String> = mapOf(
        "wa" to "Social", "tg" to "Social", "docs" to "Work", "sheets" to "Work", "cam" to "Media",
    )
    private fun categoryOf(it: LauncherItem) = catOf[it.id]
    private fun folderId(name: String) = "folder_$name"

    @Test fun `apps cluster into one folder per category with two or more apps`() {
        val items = listOf(app("wa", "Social", 0, 0), app("tg", "Social", 1, 0), app("docs", "Work", 2, 0), app("sheets", "Work", 3, 0), app("cam", "Media", 4, 0))
        val out = LauncherOps.autoArrange(layout.copy(items = items), ::categoryOf, ::folderId)

        val folders = out.items.filter { it.type == ItemType.FOLDER }
        assertEquals(setOf("Social", "Work"), folders.map { it.title }.toSet()) // Media has only 1 -> no folder
        // Social folder holds wa + tg as children
        val socialId = folders.first { it.title == "Social" }.id
        assertEquals(setOf("wa", "tg"), out.items.filter { it.parentId == socialId }.map { it.id }.toSet())
    }

    @Test fun `a single-app category is left loose on the workspace`() {
        val items = listOf(app("wa", "Social", 0, 0), app("tg", "Social", 1, 0), app("cam", "Media", 2, 0))
        val out = LauncherOps.autoArrange(layout.copy(items = items), ::categoryOf, ::folderId)
        val cam = out.items.first { it.id == "cam" }
        assertNull(cam.parentId) // still a top-level app
        assertEquals(ItemType.APP, cam.type)
    }

    @Test fun `folders take freed workspace cells and children are folder-local`() {
        val items = listOf(app("wa", "Social", 0, 0), app("tg", "Social", 1, 0))
        val out = LauncherOps.autoArrange(layout.copy(items = items), ::categoryOf, ::folderId)
        val folder = out.items.first { it.type == ItemType.FOLDER }
        assertEquals(Container.WORKSPACE, folder.container)
        assertEquals(0 to 0, folder.cellX to folder.cellY) // first free cell after the apps left
        val children = out.items.filter { it.parentId == folder.id }.sortedBy { it.cellX }
        assertEquals(listOf(0, 1), children.map { it.cellX }) // folder-local 0,1
    }

    @Test fun `no qualifying category returns the layout unchanged`() {
        val items = listOf(app("cam", "Media", 0, 0)) // only one Media app
        val src = layout.copy(items = items)
        assertEquals(src, LauncherOps.autoArrange(src, ::categoryOf, ::folderId))
    }

    @Test fun `uncategorized apps are ignored`() {
        val items = listOf(app("wa", "Social", 0, 0), app("tg", "Social", 1, 0), app("x", "?", 2, 0))
        val out = LauncherOps.autoArrange(layout.copy(items = items), { catOf[it.id] }, ::folderId)
        val x = out.items.first { it.id == "x" }
        assertNull(x.parentId)
        assertTrue(out.items.any { it.type == ItemType.FOLDER && it.title == "Social" })
    }

    @Test fun `existing folders and their children are untouched`() {
        val existingFolder = LauncherItem(id = "f0", type = ItemType.FOLDER, title = "Tools", container = Container.WORKSPACE, page = 0, cellX = 4, cellY = 4)
        val child = LauncherItem(id = "old", type = ItemType.APP, parentId = "f0", cellX = 0, cellY = 0, packageName = "pkg.old")
        val items = listOf(app("wa", "Social", 0, 0), app("tg", "Social", 1, 0), existingFolder, child)
        val out = LauncherOps.autoArrange(layout.copy(items = items), ::categoryOf, ::folderId)
        assertTrue(out.items.any { it.id == "f0" && it.title == "Tools" })
        assertEquals("f0", out.items.first { it.id == "old" }.parentId)
    }
}
