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

    private fun load(): LauncherLayout =
        prefs.getString(KEY, null)?.let { runCatching { json.decodeFromString<LauncherLayout>(it) }.getOrNull() }
            ?: LauncherLayout()

    private fun update(next: LauncherLayout) {
        _layout.value = next
        prefs.edit().putString(KEY, json.encodeToString(LauncherLayout.serializer(), next)).apply()
    }

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
     * Seed a sensible first-run layout from the installed apps: the dock filled
     * with the first [LauncherLayout.dockCount]−1 apps (centre slot reserved for
     * Vision), the rest flowing onto page-0 cells row-major. Only runs when empty.
     */
    fun seedDefault(apps: List<AppRef>) {
        if (!isEmpty || apps.isEmpty()) return
        var l = LauncherLayout()
        val dockSlots = (l.dockCount - 1).coerceAtLeast(0) // reserve centre for Vision
        apps.take(dockSlots).forEachIndexed { i, a ->
            l = LauncherOps.add(l, item(a, Container.HOTSEAT, 0, i, 0))
        }
        val rest = apps.drop(dockSlots)
        var x = 0; var y = 0
        for (a in rest) {
            if (y >= l.gridRows) break
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
