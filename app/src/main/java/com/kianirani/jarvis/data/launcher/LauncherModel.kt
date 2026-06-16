package com.kianirani.jarvis.data.launcher

import kotlinx.serialization.Serializable

/**
 * Launcher layout model (LR1, 2026-06-16) — the persisted state of a REAL
 * launcher: what sits on each home cell, in the dock, and inside folders. This
 * is the single source of truth the Workspace / Hotseat / FolderManager render
 * from and the DragController mutates. Everything is [Serializable] so the whole
 * layout round-trips through JSON in [LauncherStore].
 */

@Serializable
enum class ItemType { APP, FOLDER, WIDGET }

/** Where an item lives. Folder children carry [LauncherItem.parentId] instead. */
@Serializable
enum class Container { WORKSPACE, HOTSEAT }

/**
 * One placeable thing on the launcher: an app shortcut, a folder, or a widget.
 * Position is grid-cell based ([cellX],[cellY]) with a [spanX]×[spanY] footprint
 * (widgets span; apps/folders are 1×1). Folder children have [parentId] set and
 * their cell coords are folder-local.
 */
@Serializable
data class LauncherItem(
    val id: String,
    val type: ItemType,
    val container: Container = Container.WORKSPACE,
    val page: Int = 0,
    val cellX: Int = 0,
    val cellY: Int = 0,
    val spanX: Int = 1,
    val spanY: Int = 1,
    // APP
    val packageName: String? = null,
    val className: String? = null,
    val label: String? = null,
    // FOLDER
    val title: String? = null,
    val parentId: String? = null,
    // WIDGET
    val widgetId: Int = -1,
    val widgetProvider: String? = null,
)

/**
 * The full launcher layout: grid size, dock size, page count, and every item.
 * Folder children are stored flat with [LauncherItem.parentId] pointing at their
 * folder — this keeps moves/queries simple and JSON flat.
 */
@Serializable
data class LauncherLayout(
    val gridCols: Int = 5,
    val gridRows: Int = 5,
    val dockCount: Int = 5,
    val pageCount: Int = 1,
    val items: List<LauncherItem> = emptyList(),
) {
    /** Top-level (non-folder-child) items in a given container/page. */
    fun cells(container: Container, page: Int = 0): List<LauncherItem> =
        items.filter { it.parentId == null && it.container == container && (container == Container.HOTSEAT || it.page == page) }

    /** Children of a folder, ordered by their folder-local cell index. */
    fun folderChildren(folderId: String): List<LauncherItem> =
        items.filter { it.parentId == folderId }.sortedBy { it.cellY * gridCols + it.cellX }
}
