package com.kianirani.jarvis.data.launcher

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** A launchable app, used to seed the default layout. */
data class AppRef(val packageName: String, val className: String?, val label: String)

/**
 * Persists + mutates the [LauncherLayout] (LR1). Plain SharedPreferences + JSON
 * (matching the other stores). All edits go through [LauncherOps] (pure) then
 * persist and re-emit, so the Workspace/Hotseat/DragController observe one
 * reactive source of truth. Ids are minted here; [LauncherOps] stays pure.
 */
@Singleton
class LauncherStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("vision_launcher", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _layout = MutableStateFlow(load())
    val layout: StateFlow<LauncherLayout> = _layout.asStateFlow()

    // DS-L3 — undo/redo over every edit. All mutations funnel through update(), so the
    // history records there; undo()/redo() re-apply without re-recording.
    private val history = LayoutHistory(_layout.value)

    private fun load(): LauncherLayout =
        prefs.getString(KEY, null)?.let { runCatching { json.decodeFromString<LauncherLayout>(it) }.getOrNull() }
            ?: LauncherLayout()

    private fun update(next: LauncherLayout) {
        history.record(next)
        apply(next)
    }

    /** Persist + emit without touching the undo history (used by [undo]/[redo]). */
    private fun apply(next: LauncherLayout) {
        _layout.value = next
        prefs.edit().putString(KEY, json.encodeToString(LauncherLayout.serializer(), next)).apply()
    }

    /** DS-L3 — whether an undo/redo is available right now (drives edit-mode buttons). */
    val canUndo: Boolean get() = history.canUndo
    val canRedo: Boolean get() = history.canRedo

    /** DS-L3 — step the layout back/forward one edit; false when there's nothing to do. */
    fun undo(): Boolean = history.undo()?.also { apply(it) } != null
    fun redo(): Boolean = history.redo()?.also { apply(it) } != null

    /** True until the user (or [seedDefault]) has placed anything. */
    val isEmpty: Boolean get() = _layout.value.items.isEmpty()

    fun newId(prefix: String = "i"): String = "${prefix}_${UUID.randomUUID().toString().take(8)}"

    // ── edits (delegate to pure ops) ─────────────────────────────────────────
    fun addApp(app: AppRef, container: Container, page: Int, cellX: Int, cellY: Int) = update(
        LauncherOps.add(
            _layout.value,
            LauncherItem(
                id = newId("app"), type = ItemType.APP, container = container, page = page,
                cellX = cellX, cellY = cellY, packageName = app.packageName, className = app.className, label = app.label,
            ),
        ),
    )

    fun remove(id: String) = update(LauncherOps.remove(_layout.value, id))
    fun move(id: String, container: Container, page: Int, cellX: Int, cellY: Int) =
        update(LauncherOps.move(_layout.value, id, container, page, cellX, cellY))
    fun setDockCount(count: Int) = update(LauncherOps.setDockCount(_layout.value, count))
    fun setGrid(cols: Int, rows: Int) = update(LauncherOps.setGrid(_layout.value, cols, rows))
    /** Change grid density and re-flow items so none are stranded (LR10). */
    fun setGridReflow(cols: Int, rows: Int) = update(LauncherOps.reflowWorkspace(_layout.value, cols, rows))
    fun addPage() = update(LauncherOps.addPage(_layout.value))
    fun removePage(page: Int) = update(LauncherOps.removePage(_layout.value, page))
    fun createFolder(targetId: String, draggedId: String, title: String = "Folder") =
        update(LauncherOps.createFolder(_layout.value, targetId, draggedId, newId("folder"), title))
    fun addToFolder(folderId: String, itemId: String) = update(LauncherOps.addToFolder(_layout.value, folderId, itemId))
    fun removeFromFolder(itemId: String, container: Container, page: Int, cellX: Int, cellY: Int) =
        update(LauncherOps.removeFromFolder(_layout.value, itemId, container, page, cellX, cellY))
    fun renameFolder(folderId: String, title: String) = update(LauncherOps.renameFolder(_layout.value, folderId, title))

    fun reset() = update(LauncherLayout())

    /**
     * DS-L3 "Optimize home": group top-level workspace apps into category folders.
     * [categoryOf] (supplied by the view-model from PackageManager) names each app's
     * folder, or null to leave it loose. Pure [LauncherOps.autoArrange] does the work;
     * this only mints folder ids and persists. Undoable like any other edit.
     */
    fun autoArrange(categoryOf: (LauncherItem) -> String?) =
        update(LauncherOps.autoArrange(_layout.value, categoryOf) { newId("folder") })

    /**
     * LR5 — pull a folder child back onto the home workspace at the first free
     * cell (adds a page if every page is full). False when the id isn't a child.
     */
    fun pullFromFolder(itemId: String): Boolean {
        val l = _layout.value
        l.items.firstOrNull { it.id == itemId && it.parentId != null } ?: return false
        for (p in 0 until l.pageCount) {
            LauncherOps.firstFreeCell(l, Container.WORKSPACE, p)?.let { (x, y) ->
                update(LauncherOps.removeFromFolder(l, itemId, Container.WORKSPACE, p, x, y)); return true
            }
        }
        val grown = LauncherOps.addPage(l)
        update(LauncherOps.removeFromFolder(grown, itemId, Container.WORKSPACE, grown.pageCount - 1, 0, 0))
        return true
    }

    /** LR11 — serialize the whole layout for backup (clipboard / file). */
    fun exportJson(): String = json.encodeToString(LauncherLayout.serializer(), _layout.value)

    /** LR11 — restore a layout from a backup string. False when it doesn't parse. */
    fun importJson(text: String): Boolean =
        runCatching { json.decodeFromString<LauncherLayout>(text) }.getOrNull()
            ?.also { update(it) } != null

    /**
     * Pin an app to the home workspace at the first free cell, scanning pages in
     * order and adding a fresh page if every page is full. No-op (returns false)
     * when the app is already pinned on the workspace, so taps don't duplicate it.
     */
    fun addAppToHome(app: AppRef): Boolean {
        val l = _layout.value
        if (l.items.any { it.parentId == null && it.container == Container.WORKSPACE && it.packageName == app.packageName }) return false
        for (p in 0 until l.pageCount) {
            LauncherOps.firstFreeCell(l, Container.WORKSPACE, p)?.let { (x, y) ->
                addApp(app, Container.WORKSPACE, p, x, y); return true
            }
        }
        update(LauncherOps.addPage(l))
        addApp(app, Container.WORKSPACE, _layout.value.pageCount - 1, 0, 0)
        return true
    }

    /**
     * Seed a curated first-run home (Neo/Pixel-style): only the first page worth
     * of apps is pinned to the workspace — the rest live in the app drawer, not on
     * endless home pages. The dock (hotseat) is left for the user to populate
     * (LR6). Only runs when empty.
     */
    fun seedDefault(apps: List<AppRef>) {
        if (!isEmpty || apps.isEmpty()) return
        var l = LauncherLayout()
        var x = 0; var y = 0
        for (a in apps.take(l.gridCols * l.gridRows)) {
            l = LauncherOps.add(l, item(a, Container.WORKSPACE, 0, x, y))
            x++; if (x >= l.gridCols) { x = 0; y++ }
        }
        update(l)
    }

    private fun item(a: AppRef, c: Container, page: Int, x: Int, y: Int) = LauncherItem(
        id = newId("app"), type = ItemType.APP, container = c, page = page, cellX = x, cellY = y,
        packageName = a.packageName, className = a.className, label = a.label,
    )

    private companion object { const val KEY = "layout_json" }
}
