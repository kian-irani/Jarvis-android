package com.kianirani.jarvis.ui.screen.workspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import com.kianirani.jarvis.data.launcher.Container
import com.kianirani.jarvis.data.launcher.ItemType
import com.kianirani.jarvis.data.launcher.LauncherGeometry
import com.kianirani.jarvis.data.launcher.LauncherItem
import com.kianirani.jarvis.data.launcher.LauncherLayout
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.VisionIcons
import com.kianirani.jarvis.ui.theme.glassPanel

/**
 * LR2 — the real launcher home. A [HorizontalPager] whose first page is the
 * AI-core home ([homePage] slot — the [com.kianirani.jarvis.ui.screen.home.HomeScreen]
 * orb, kept as the hero per RD11) and whose remaining pages are real workspace
 * grids that render the pinned apps/folders held by [LauncherViewModel.store].
 * Page dots float above the dock; tapping a folder opens it. Nothing is
 * hard-coded — every cell comes from the persisted `LauncherStore` layout.
 */
@Composable
fun WorkspaceHomePager(
    vm: LauncherViewModel,
    homePage: @Composable () -> Unit,
    onOpenSettings: () -> Unit = {},
    onAssistant: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onNotifications: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val layout by vm.layout.collectAsStateWithLifecycle()
    val visuals by vm.visuals.collectAsStateWithLifecycle()
    val totalPages = (1 + layout.pageCount).coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()
    var openFolder by remember { mutableStateOf<String?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // LR9 — workspace swipe gestures mapped through the user's GestureMap: swipe-up opens the
    // app drawer, swipe-down opens notifications. A deliberate fling threshold avoids accidents.
    val gestures = remember {
        com.kianirani.jarvis.core.gesture.GestureMap(
            mapOf(
                com.kianirani.jarvis.core.gesture.Gesture.SWIPE_UP to com.kianirani.jarvis.core.gesture.GestureAction.OPEN_DRAWER,
                com.kianirani.jarvis.core.gesture.Gesture.SWIPE_DOWN to com.kianirani.jarvis.core.gesture.GestureAction.NOTIFICATIONS,
            ),
        )
    }
    val swipeThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }
    val runGesture: (com.kianirani.jarvis.core.gesture.Gesture) -> Unit = { g ->
        when (gestures.actionFor(g)) {
            com.kianirani.jarvis.core.gesture.GestureAction.OPEN_DRAWER -> onOpenDrawer()
            com.kianirani.jarvis.core.gesture.GestureAction.NOTIFICATIONS -> onNotifications()
            com.kianirani.jarvis.core.gesture.GestureAction.EXPAND_PANEL -> onOpenDrawer()
            else -> Unit
        }
    }
    val openAppInfo: (String) -> Unit = { pkg ->
        runCatching {
            ctx.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:$pkg"))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    Box(
        modifier.fillMaxSize().pointerInput(Unit) {
            // LR9 — accumulate a vertical fling; horizontal paging stays with the pager.
            var dy = 0f
            detectVerticalDragGestures(
                onDragStart = { dy = 0f },
                onVerticalDrag = { _, amount -> dy += amount },
                onDragEnd = {
                    when {
                        dy <= -swipeThresholdPx -> runGesture(com.kianirani.jarvis.core.gesture.Gesture.SWIPE_UP)
                        dy >= swipeThresholdPx -> runGesture(com.kianirani.jarvis.core.gesture.Gesture.SWIPE_DOWN)
                    }
                },
            )
        },
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            if (page == 0) {
                homePage()
            } else {
                WorkspacePage(
                    layout = layout,
                    page = page - 1,
                    visualFor = visuals::get,
                    onLaunch = vm::launch,
                    onOpenFolder = { openFolder = it },
                    onMove = { id, x, y -> vm.store.move(id, Container.WORKSPACE, page - 1, x, y) },
                    onMakeFolder = { targetId, draggedId -> vm.store.createFolder(targetId, draggedId) },
                    onAddToFolder = { folderId, itemId -> vm.store.addToFolder(folderId, itemId) },
                    onRemove = { id -> vm.store.remove(id) },
                    onAppInfo = openAppInfo,
                    onEditHome = { showEdit = true },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // Page dots — floats above the dock so it reads against any page.
        PageDots(
            count = totalPages,
            current = pagerState.currentPage,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
        )

        // LR6/LR12 — the configurable dock (hotseat) with the always-present Vision
        // assistant button at its centre. Hotseat apps come from the persisted layout;
        // the centre button scrolls to the orb/command-bar home and opens the assistant.
        WorkspaceDock(
            apps = layout.cells(Container.HOTSEAT),
            dockCount = layout.dockCount,
            visualFor = visuals::get,
            onLaunch = vm::launch,
            onAssistant = {
                scope.launch { pagerState.animateScrollToPage(0) }
                onAssistant()
            },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp),
        )
    }

    // Folder peek — open the tapped folder's children in a glass dialog (real,
    // tap-to-launch). Full folder management arrives in LR5.
    openFolder?.let { id ->
        val folder = layout.items.firstOrNull { it.id == id }
        FolderDialog(
            title = folder?.title ?: "Folder",
            children = layout.folderChildren(id),
            visualFor = visuals::get,
            onRename = { newTitle -> vm.store.renameFolder(id, newTitle) },
            onLaunch = { pkg -> vm.launch(pkg); openFolder = null },
            onPullOut = { childId -> vm.store.pullFromFolder(childId); openFolder = null },
            onDismiss = { openFolder = null },
        )
    }

    if (showEdit) {
        val workspacePage = (pagerState.currentPage - 1).coerceAtLeast(0)
        HomeEditSheet(
            cols = layout.gridCols,
            rows = layout.gridRows,
            canRemovePage = layout.pageCount > 1,
            canUndo = vm.store.canUndo,
            canRedo = vm.store.canRedo,
            onUndo = { vm.store.undo() },
            onRedo = { vm.store.redo() },
            onOptimize = { vm.autoArrange(); showEdit = false },
            onSetGrid = { c, r -> vm.store.setGridReflow(c, r) },
            onWallpaper = {
                runCatching {
                    ctx.startActivity(
                        android.content.Intent.createChooser(
                            android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER), "Wallpaper",
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
                showEdit = false
            },
            onAddPage = { vm.store.addPage(); showEdit = false },
            onRemovePage = { vm.store.removePage(workspacePage); showEdit = false },
            onSettings = { showEdit = false; onOpenSettings() },
            onDismiss = { showEdit = false },
        )
    }
}

/**
 * LR6 Configurable Dock (Hotseat) + LR12 Vision Assistant button. A glass bar pinned to the
 * bottom across every page: up to [dockCount] hotseat apps with the **Vision orb button**
 * always at the centre. Tapping an app launches it; tapping the centre opens the assistant
 * (home orb + command bar). The dock count is user-configurable in Settings (4/5/6) and
 * persisted on [LauncherLayout.dockCount]; apps in the dock are the `HOTSEAT` container items.
 */
@Composable
private fun WorkspaceDock(
    apps: List<LauncherItem>,
    dockCount: Int,
    visualFor: (String?) -> AppVisual?,
    onLaunch: (String) -> Unit,
    onAssistant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slots = dockCount.coerceIn(4, 6)
    // Half the apps left of the Vision button, half right (centre is the assistant).
    val shown = apps.take(slots)
    val mid = (shown.size + 1) / 2
    val left = shown.take(mid)
    val right = shown.drop(mid)

    Row(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .glassPanel(radius = 28.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        left.forEach { DockApp(it, visualFor(it.packageName), onLaunch) }

        // LR12 — the always-present Vision assistant button (the dock's hero).
        Box(
            Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(VisionColors.PlasmaSweep)
                .clickable(onClick = onAssistant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(VisionIcons.Spark, contentDescription = "Vision assistant", tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(26.dp))
        }

        right.forEach { DockApp(it, visualFor(it.packageName), onLaunch) }
    }
}

/** A single launchable app icon in the dock. */
@Composable
private fun DockApp(item: LauncherItem, visual: AppVisual?, onLaunch: (String) -> Unit) {
    val pkg = item.packageName ?: return
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).clickable { onLaunch(pkg) },
        contentAlignment = Alignment.Center,
    ) {
        if (visual != null) {
            Image(visual.icon, contentDescription = item.label, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)))
        } else {
            Box(
                Modifier.fillMaxSize().background(VisionColors.CyanFaint, RoundedCornerShape(14.dp))
                    .border(1.dp, VisionColors.Border, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(VisionIcons.Apps, item.label, tint = VisionColors.CyanPrimary, modifier = Modifier.size(22.dp)) }
        }
    }
}

/**
 * One workspace page: a fixed [gridCols]×[gridRows] grid (no scroll — a real
 * launcher page) painted from the layout's cells. Empty cells stay blank so the
 * spatial arrangement the user/seed chose is preserved.
 *
 * LR3 DragController: a long-press picks up the cell under the finger; dragging
 * shows a floating icon; on release the target cell is resolved via
 * [LauncherGeometry] and the layout is mutated — drop on empty → move, drop on an
 * app → folder, drop on a folder → add. (Cross-page / dock drags arrive in LR6.)
 */
@Composable
private fun WorkspacePage(
    layout: LauncherLayout,
    page: Int,
    visualFor: (String?) -> AppVisual?,
    onLaunch: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onMove: (id: String, x: Int, y: Int) -> Unit,
    onMakeFolder: (targetId: String, draggedId: String) -> Unit,
    onAddToFolder: (folderId: String, itemId: String) -> Unit,
    onRemove: (id: String) -> Unit,
    onAppInfo: (pkg: String) -> Unit,
    onEditHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cols = layout.gridCols
    val rows = layout.gridRows
    val cells = layout.cells(Container.WORKSPACE, page)
    val byPos = remember(cells) { cells.associateBy { it.cellY * cols + it.cellX } }
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf<LauncherItem?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var dragStart by remember { mutableStateOf(Offset.Zero) }
    var movedFar by remember { mutableStateOf(false) }
    // Long-press without dragging opens a Neo/Pixel-style icon menu for this item.
    var menuFor by remember { mutableStateOf<LauncherItem?>(null) }
    var menuPos by remember { mutableStateOf(Offset.Zero) }
    val moveThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 24.dp.toPx() }
    val removeStripPx = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }

    Box(
        // NEO7 — leave room at the bottom so the last app row clears the dock (LR6), and
        // keep comfortable side gutters. Top/side from systemBars; bottom reserves the dock.
        modifier.systemBarsPadding().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 96.dp)
            .onSizeChanged { gridSize = it }
            .pointerInput(cells, gridSize, cols, rows) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { off ->
                        if (gridSize.width > 0 && gridSize.height > 0) {
                            val (cx, cy) = LauncherGeometry.cellAt(off.x, off.y, gridSize.width.toFloat(), gridSize.height.toFloat(), cols, rows)
                            dragging = byPos[cy * cols + cx]
                            dragPos = off; dragStart = off; movedFar = false
                        }
                    },
                    onDrag = { change, amount ->
                        change.consume(); dragPos += amount
                        if ((dragPos - dragStart).getDistance() > moveThresholdPx) movedFar = true
                    },
                    onDragEnd = {
                        val d = dragging
                        if (d == null && !movedFar) {
                            // Long-press on empty space → home edit sheet (Neo-style).
                            onEditHome()
                        } else if (d != null && gridSize.width > 0) {
                            if (!movedFar) {
                                // A long-press that didn't move → open the icon menu.
                                menuFor = d; menuPos = dragPos
                            } else if (dragPos.y <= removeStripPx) {
                                onRemove(d.id)
                            } else {
                                val (tx, ty) = LauncherGeometry.cellAt(dragPos.x, dragPos.y, gridSize.width.toFloat(), gridSize.height.toFloat(), cols, rows)
                                val target = byPos[ty * cols + tx]
                                when {
                                    target == null -> onMove(d.id, tx, ty)
                                    target.id == d.id -> Unit
                                    target.type == ItemType.FOLDER && d.type == ItemType.APP -> onAddToFolder(target.id, d.id)
                                    target.type == ItemType.APP && d.type == ItemType.APP -> onMakeFolder(target.id, d.id)
                                    else -> onMove(d.id, tx, ty)
                                }
                            }
                        }
                        dragging = null
                    },
                    onDragCancel = { dragging = null },
                )
            },
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (y in 0 until rows) {
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (x in 0 until cols) {
                        val item = byPos[y * cols + x]
                        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            if (item != null) {
                                // The picked-up tile dims in place while it floats under the finger.
                                val preview = if (item.type == ItemType.FOLDER) {
                                    layout.folderChildren(item.id).take(4).map { visualFor(it.packageName) }
                                } else {
                                    emptyList()
                                }
                                Box(Modifier.fillMaxSize().alpha(if (item.id == dragging?.id) 0.3f else 1f), contentAlignment = Alignment.Center) {
                                    WorkspaceCell(item, visualFor, preview, onLaunch, onOpenFolder)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Remove strip — appears at the top while dragging; drop here to unpin.
        if (dragging != null) {
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(VisionColors.DangerRed.copy(alpha = 0.18f))
                    .border(1.dp, VisionColors.DangerRed.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(VisionIcons.Close, "Remove", tint = VisionColors.DangerRed, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text("Remove from home", style = MaterialTheme.typography.labelLarge, color = VisionColors.DangerRed)
            }
        }

        // Floating drag image tracking the finger.
        dragging?.let { d ->
            val cw = if (cols > 0) gridSize.width / cols else 0
            val ch = if (rows > 0) gridSize.height / rows else 0
            Box(
                Modifier.offset { IntOffset((dragPos.x - cw / 2f).toInt(), (dragPos.y - ch / 2f).toInt()) }
                    .size(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                val v = visualFor(d.packageName)
                if (v != null) {
                    Image(v.icon, contentDescription = v.label, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)))
                } else {
                    Box(
                        Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).glassPanel(radius = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) { Icon(VisionIcons.Apps, d.title, tint = VisionColors.CyanPrimary, modifier = Modifier.size(26.dp)) }
                }
            }
        }

        // Neo/Pixel-style icon menu (long-press without dragging).
        menuFor?.let { item ->
            // Tap-anywhere scrim to dismiss.
            Box(Modifier.fillMaxSize().clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { menuFor = null })
            Column(
                Modifier.offset {
                    val maxX = (gridSize.width - with(this) { 200.dp.toPx() }).coerceAtLeast(0f)
                    IntOffset(menuPos.x.coerceIn(0f, maxX).toInt(), menuPos.y.toInt())
                }.width(200.dp).clip(RoundedCornerShape(16.dp)).glassPanel(radius = 16.dp).padding(vertical = 6.dp),
            ) {
                if (item.type == ItemType.APP && item.packageName != null) {
                    IconMenuItem(VisionIcons.Search, "App info") { onAppInfo(item.packageName); menuFor = null }
                }
                IconMenuItem(VisionIcons.Close, "Remove from home", danger = true) { onRemove(item.id); menuFor = null }
            }
        }
    }
}

/** Neo-style home edit sheet — long-press empty space → wallpaper / grid / pages. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HomeEditSheet(
    cols: Int,
    rows: Int,
    canRemovePage: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onOptimize: () -> Unit,
    onSetGrid: (Int, Int) -> Unit,
    onWallpaper: () -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VisionColors.Surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("Edit home", style = MaterialTheme.typography.titleMedium, color = VisionColors.TextPrimary, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
            // DS-L3 — undo/redo the layout (drag/folder/grid/page edits). Disabled when
            // there's nothing to step to; stays open so several steps are quick.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UndoRedoButton(VisionIcons.Undo, "Undo", enabled = canUndo, modifier = Modifier.weight(1f), onClick = onUndo)
                UndoRedoButton(VisionIcons.Redo, "Redo", enabled = canRedo, modifier = Modifier.weight(1f), onClick = onRedo)
            }
            IconMenuItem(VisionIcons.Spark, "Auto-organize apps", onClick = onOptimize)
            IconMenuItem(VisionIcons.Weather, "Wallpaper", onClick = onWallpaper)
            // Inline grid-size presets (re-flow safely).
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Grid size", style = MaterialTheme.typography.bodyMedium, color = VisionColors.TextPrimary)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(4 to 5, 5 to 5, 5 to 6, 6 to 6).forEach { (c, r) ->
                        val sel = c == cols && r == rows
                        Text(
                            "$c×$r", style = MaterialTheme.typography.labelMedium,
                            color = if (sel) VisionColors.CyanPrimary else VisionColors.TextDim,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { onSetGrid(c, r) }
                                .border(1.dp, if (sel) VisionColors.CyanPrimary else VisionColors.Border, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            IconMenuItem(VisionIcons.Apps, "Add a page", onClick = onAddPage)
            if (canRemovePage) IconMenuItem(VisionIcons.Close, "Remove this page", danger = true, onClick = onRemovePage)
            IconMenuItem(VisionIcons.Settings, "Launcher settings", onClick = onSettings)
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

/** A single row in the long-press icon menu. */
@Composable
private fun IconMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val tint = if (danger) VisionColors.DangerRed else VisionColors.CyanPrimary
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (danger) VisionColors.DangerRed else VisionColors.TextPrimary)
    }
}

/** DS-L3 — a glass undo/redo button; dimmed + non-interactive when [enabled] is false. */
@Composable
private fun UndoRedoButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = if (enabled) VisionColors.CyanPrimary else VisionColors.TextDim
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, if (enabled) VisionColors.Border else VisionColors.Border.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (enabled) VisionColors.TextPrimary else VisionColors.TextDim)
    }
}

/** A single grid cell — an app shortcut, or a folder showing a 2×2 icon preview. */
@Composable
private fun WorkspaceCell(
    item: LauncherItem,
    visualFor: (String?) -> AppVisual?,
    folderPreview: List<AppVisual?>,
    onLaunch: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
) {
    when (item.type) {
        ItemType.FOLDER -> FolderTile(item, folderPreview, onClick = { onOpenFolder(item.id) })
        else -> {
            val v = visualFor(item.packageName)
            AppTile(
                label = v?.label ?: item.label.orEmpty(),
                icon = v,
                onClick = { item.packageName?.let(onLaunch) },
            )
        }
    }
}

/** A pinned app: rounded icon + single-line label. */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun AppTile(label: String, icon: AppVisual?, onLongClick: (() -> Unit)? = null, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)), contentAlignment = Alignment.Center) {
            if (icon != null) {
                Image(icon.icon, contentDescription = label, modifier = Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier.fillMaxSize().background(VisionColors.CyanFaint)
                        .border(1.dp, VisionColors.Border, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(VisionIcons.Apps, label, tint = VisionColors.CyanPrimary, modifier = Modifier.size(24.dp)) }
            }
        }
        Text(
            label, style = MaterialTheme.typography.labelSmall, color = VisionColors.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/** A folder cell: a glass tile with a 2×2 preview of the first child icons + title. */
@Composable
private fun FolderTile(item: LauncherItem, preview: List<AppVisual?>, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).glassPanel(radius = 15.dp).padding(7.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (preview.isEmpty()) {
                Icon(VisionIcons.Apps, item.title, tint = VisionColors.CyanPrimary, modifier = Modifier.size(22.dp))
            } else {
                // 2×2 mini-grid of the first child icons (the real folder preview).
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (r in 0 until 2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (c in 0 until 2) {
                                val v = preview.getOrNull(r * 2 + c)
                                Box(Modifier.size(15.dp).clip(RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                                    if (v != null) Image(v.icon, contentDescription = null, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            item.title ?: "Folder", style = MaterialTheme.typography.labelSmall, color = VisionColors.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/** A row of page-position dots; the active page is the bright, wider dot. */
@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    if (count <= 1) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val active = i == current
            Box(
                Modifier.size(width = if (active) 18.dp else 7.dp, height = 7.dp)
                    .clip(CircleShape)
                    .background(if (active) VisionColors.CyanPrimary else VisionColors.TextDim.copy(alpha = 0.5f)),
            )
        }
    }
}

/** Glass dialog listing a folder's apps with an editable title (LR5 rename). */
@Composable
private fun FolderDialog(
    title: String,
    children: List<LauncherItem>,
    visualFor: (String?) -> AppVisual?,
    onRename: (String) -> Unit,
    onLaunch: (String) -> Unit,
    onPullOut: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).glassPanel(radius = 24.dp).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Editable folder name — commit on each edit so it persists (LR5).
            var name by remember(title) { mutableStateOf(title) }
            BasicTextField(
                value = name,
                onValueChange = { name = it; onRename(it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = VisionColors.TextPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(VisionColors.CyanPrimary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (name.isEmpty()) Text("Folder name", style = MaterialTheme.typography.titleMedium, color = VisionColors.TextDim)
                    inner()
                },
            )
            if (children.isEmpty()) {
                Text("Empty folder", style = MaterialTheme.typography.bodyMedium, color = VisionColors.TextDim)
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(children, key = { it.id }) { child ->
                        val v = visualFor(child.packageName)
                        AppTile(
                            label = v?.label ?: child.label.orEmpty(), icon = v,
                            onLongClick = { onPullOut(child.id) },
                            onClick = { child.packageName?.let(onLaunch) },
                        )
                    }
                }
                Text("Long-press an app to move it back to home", style = MaterialTheme.typography.labelSmall, color = VisionColors.TextDim)
            }
        }
    }
}
