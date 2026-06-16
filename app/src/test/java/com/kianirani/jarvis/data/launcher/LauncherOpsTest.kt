package com.kianirani.jarvis.data.launcher

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherOpsTest {

    private fun app(id: String, c: Container = Container.WORKSPACE, p: Int = 0, x: Int = 0, y: Int = 0) =
        LauncherItem(id = id, type = ItemType.APP, container = c, page = p, cellX = x, cellY = y, packageName = "pkg.$id")

    private val base = LauncherLayout(gridCols = 5, gridRows = 5, dockCount = 5, pageCount = 2)

    @Test
    fun `add then remove`() {
        var l = LauncherOps.add(base, app("a"))
        assertEquals(1, l.items.size)
        l = LauncherOps.remove(l, "a")
        assertTrue(l.items.isEmpty())
    }

    @Test
    fun `move updates container page and cell and clears parent`() {
        var l = LauncherOps.add(base, app("a").copy(parentId = "f"))
        l = LauncherOps.move(l, "a", Container.HOTSEAT, 0, 2, 0)
        val it = l.items.single()
        assertEquals(Container.HOTSEAT, it.container)
        assertEquals(2, it.cellX)
        assertNull(it.parentId)
    }

    @Test
    fun `dock count is clamped to 3 6`() {
        assertEquals(6, LauncherOps.setDockCount(base, 9).dockCount)
        assertEquals(3, LauncherOps.setDockCount(base, 1).dockCount)
    }

    @Test
    fun `removePage drops its items and shifts later pages down`() {
        var l = base
        l = LauncherOps.add(l, app("p0", p = 0))
        l = LauncherOps.add(l, app("p1", p = 1, x = 1))
        l = LauncherOps.removePage(l, 0)
        assertEquals(1, l.pageCount)
        assertNull(l.items.find { it.id == "p0" })          // page 0 items gone
        assertEquals(0, l.items.single { it.id == "p1" }.page) // page 1 -> 0
    }

    @Test
    fun `removePage keeps at least one page`() {
        assertEquals(1, LauncherOps.removePage(base.copy(pageCount = 1), 0).pageCount)
    }

    @Test
    fun `createFolder merges two apps into a folder at the target cell`() {
        var l = LauncherOps.add(base, app("a", x = 1, y = 1))
        l = LauncherOps.add(l, app("b", x = 3, y = 2))
        l = LauncherOps.createFolder(l, "a", "b", folderId = "fold", title = "Stuff")
        val folder = l.items.single { it.type == ItemType.FOLDER }
        assertEquals("fold", folder.id)
        assertEquals(1, folder.cellX); assertEquals(1, folder.cellY) // target's cell
        val kids = l.folderChildren("fold")
        assertEquals(2, kids.size)
        assertTrue(kids.all { it.parentId == "fold" })
        assertTrue(l.items.none { it.id == "a" && it.parentId == null }) // a/b no longer top-level
    }

    @Test
    fun `createFolder is a no-op for the same item`() {
        val l = LauncherOps.add(base, app("a"))
        assertEquals(l, LauncherOps.createFolder(l, "a", "a", "fold"))
    }

    @Test
    fun `addToFolder then removeFromFolder`() {
        var l = LauncherOps.add(base, app("a"))
        l = LauncherOps.add(l, app("b"))
        l = LauncherOps.createFolder(l, "a", "b", "fold")
        l = LauncherOps.add(l, app("c", x = 4, y = 4))
        l = LauncherOps.addToFolder(l, "fold", "c")
        assertEquals(3, l.folderChildren("fold").size)
        l = LauncherOps.removeFromFolder(l, "c", Container.WORKSPACE, 0, 0, 3)
        assertEquals(2, l.folderChildren("fold").size)
        assertNull(l.items.single { it.id == "c" }.parentId)
    }

    @Test
    fun `firstFreeCell finds the next row-major gap and null when full`() {
        var l = base
        // fill (0,0) and (1,0)
        l = LauncherOps.add(l, app("a", x = 0, y = 0))
        l = LauncherOps.add(l, app("b", x = 1, y = 0))
        assertEquals(2 to 0, LauncherOps.firstFreeCell(l, Container.WORKSPACE, 0))
        // fill the whole dock (count 5)
        var d = base
        for (i in 0 until d.dockCount) d = LauncherOps.add(d, app("d$i", c = Container.HOTSEAT, x = i))
        assertNull(LauncherOps.firstFreeCell(d, Container.HOTSEAT, 0))
    }

    @Test
    fun `layout round-trips through json`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        var l = LauncherOps.add(base, app("a", x = 2, y = 3))
        l = LauncherOps.add(l, app("b", c = Container.HOTSEAT, x = 1))
        val text = json.encodeToString(LauncherLayout.serializer(), l)
        val back = json.decodeFromString(LauncherLayout.serializer(), text)
        assertEquals(l, back)
    }
}
