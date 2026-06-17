package com.kianirani.jarvis.data.launcher

/**
 * Pure, deterministic layout mutations (LR1). No Android, no IO, no id
 * generation (ids are passed in by the caller / [LauncherStore]) — so every rule
 * here is unit-testable on the JVM. [LauncherStore] wraps these with persistence
 * and id minting; the DragController calls them on drop.
 */
object LauncherOps {

    fun add(layout: LauncherLayout, item: LauncherItem): LauncherLayout =
        layout.copy(items = layout.items.filterNot { it.id == item.id } + item)

    /** Remove an item (and, if it's a folder, all its children). */
    fun remove(layout: LauncherLayout, id: String): LauncherLayout =
        layout.copy(items = layout.items.filterNot { it.id == id || it.parentId == id })

    /** Move a top-level item to a new container/page/cell (clears any parent). */
    fun move(
        layout: LauncherLayout,
        id: String,
        container: Container,
        page: Int,
        cellX: Int,
        cellY: Int,
    ): LauncherLayout = layout.copy(
        items = layout.items.map {
            if (it.id == id) it.copy(container = container, page = page, cellX = cellX, cellY = cellY, parentId = null) else it
        },
    )

    fun setDockCount(layout: LauncherLayout, count: Int): LauncherLayout =
        layout.copy(dockCount = count.coerceIn(3, 6))

    fun setGrid(layout: LauncherLayout, cols: Int, rows: Int): LauncherLayout =
        layout.copy(gridCols = cols.coerceIn(3, 8), gridRows = rows.coerceIn(3, 9))

    /**
     * Change the grid density and re-flow every top-level workspace item into the
     * new grid (row-major, preserving order, spilling onto fresh pages). Folder
     * children and dock items keep their place. Page count is recomputed. Pure, so
     * changing grid size never leaves icons stranded off the new bounds.
     */
    fun reflowWorkspace(layout: LauncherLayout, cols: Int, rows: Int): LauncherLayout {
        val c = cols.coerceIn(3, 8)
        val r = rows.coerceIn(3, 9)
        val per = c * r
        val tops = layout.items
            .filter { it.parentId == null && it.container == Container.WORKSPACE }
            .sortedWith(compareBy({ it.page }, { it.cellY }, { it.cellX }))
        val others = layout.items.filter { !(it.parentId == null && it.container == Container.WORKSPACE) }
        val replaced = tops.mapIndexed { i, item ->
            val slot = i % per
            item.copy(page = i / per, cellX = slot % c, cellY = slot / c)
        }
        val pageCount = if (tops.isEmpty()) 1 else (tops.size + per - 1) / per
        return layout.copy(gridCols = c, gridRows = r, pageCount = pageCount, items = others + replaced)
    }

    fun addPage(layout: LauncherLayout): LauncherLayout =
        layout.copy(pageCount = layout.pageCount + 1)

    /** Delete a page: drop its items, shift later pages down. Always keep ≥1 page. */
    fun removePage(layout: LauncherLayout, page: Int): LauncherLayout {
        if (layout.pageCount <= 1) return layout
        val kept = layout.items
            .filterNot { it.parentId == null && it.container == Container.WORKSPACE && it.page == page }
            .map { if (it.container == Container.WORKSPACE && it.parentId == null && it.page > page) it.copy(page = it.page - 1) else it }
        return layout.copy(pageCount = layout.pageCount - 1, items = kept)
    }

    /**
     * Combine two top-level app items into a new folder at the target's cell.
     * [folderId] is supplied by the caller (Store mints a UUID; tests pass a
     * fixed id). No-op unless both are apps.
     */
    fun createFolder(
        layout: LauncherLayout,
        targetId: String,
        draggedId: String,
        folderId: String,
        title: String = "Folder",
    ): LauncherLayout {
        val target = layout.items.find { it.id == targetId && it.type == ItemType.APP } ?: return layout
        val dragged = layout.items.find { it.id == draggedId && it.type == ItemType.APP } ?: return layout
        if (targetId == draggedId) return layout
        val folder = LauncherItem(
            id = folderId, type = ItemType.FOLDER, title = title,
            container = target.container, page = target.page, cellX = target.cellX, cellY = target.cellY,
        )
        val children = listOf(target, dragged).mapIndexed { i, app ->
            app.copy(parentId = folderId, container = Container.WORKSPACE, page = 0, cellX = i, cellY = 0)
        }
        return layout.copy(items = layout.items.filterNot { it.id == targetId || it.id == draggedId } + folder + children)
    }

    /** Drop an app into an existing folder (appended after current children). */
    fun addToFolder(layout: LauncherLayout, folderId: String, itemId: String): LauncherLayout {
        if (layout.items.none { it.id == folderId && it.type == ItemType.FOLDER }) return layout
        val next = layout.folderChildren(folderId).size
        return layout.copy(
            items = layout.items.map {
                if (it.id == itemId) it.copy(parentId = folderId, cellX = next % layout.gridCols, cellY = next / layout.gridCols) else it
            },
        )
    }

    /** Pull an app out of its folder back onto a workspace/dock cell. */
    fun removeFromFolder(
        layout: LauncherLayout,
        itemId: String,
        container: Container,
        page: Int,
        cellX: Int,
        cellY: Int,
    ): LauncherLayout = move(layout, itemId, container, page, cellX, cellY)

    fun renameFolder(layout: LauncherLayout, folderId: String, title: String): LauncherLayout =
        layout.copy(items = layout.items.map { if (it.id == folderId) it.copy(title = title) else it })

    fun isOccupied(layout: LauncherLayout, container: Container, page: Int, cellX: Int, cellY: Int): Boolean =
        layout.cells(container, page).any { it.cellX == cellX && it.cellY == cellY }

    /** First free cell in row-major order, or null if the page/dock is full. */
    fun firstFreeCell(layout: LauncherLayout, container: Container, page: Int = 0): Pair<Int, Int>? {
        val cols = if (container == Container.HOTSEAT) layout.dockCount else layout.gridCols
        val rows = if (container == Container.HOTSEAT) 1 else layout.gridRows
        for (y in 0 until rows) for (x in 0 until cols) {
            if (!isOccupied(layout, container, page, x, y)) return x to y
        }
        return null
    }
}
