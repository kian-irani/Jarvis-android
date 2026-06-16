package com.kianirani.jarvis.ui.screen.workspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    modifier: Modifier = Modifier,
) {
    val layout by vm.layout.collectAsStateWithLifecycle()
    val visuals by vm.visuals.collectAsStateWithLifecycle()
    val totalPages = (1 + layout.pageCount).coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { totalPages })
    var openFolder by remember { mutableStateOf<String?>(null) }

    Box(modifier.fillMaxSize()) {
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
    }

    // Folder peek — open the tapped folder's children in a glass dialog (real,
    // tap-to-launch). Full folder management arrives in LR5.
    openFolder?.let { id ->
        val folder = layout.items.firstOrNull { it.id == id }
        FolderDialog(
            title = folder?.title ?: "Folder",
            children = layout.folderChildren(id),
            visualFor = visuals::get,
            onLaunch = { pkg -> vm.launch(pkg); openFolder = null },
            onDismiss = { openFolder = null },
        )
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
    modifier: Modifier = Modifier,
) {
    val cols = layout.gridCols
    val rows = layout.gridRows
    val cells = layout.cells(Container.WORKSPACE, page)
    val byPos = remember(cells) { cells.associateBy { it.cellY * cols + it.cellX } }
    var gridSize by remember { mutableStateOf(IntSize.Zero) }
    var dragging by remember { mutableStateOf<LauncherItem?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    // Drop strip at the top removes the dragged item from home (shares this Box's
    // coordinate space with the gesture, so a hit-test on dragPos.y is exact).
    val removeStripPx = with(androidx.compose.ui.platform.LocalDensity.current) { 64.dp.toPx() }

    Box(
        modifier.systemBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)
            .onSizeChanged { gridSize = it }
            .pointerInput(cells, gridSize, cols, rows) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { off ->
                        if (gridSize.width > 0 && gridSize.height > 0) {
                            val (cx, cy) = LauncherGeometry.cellAt(off.x, off.y, gridSize.width.toFloat(), gridSize.height.toFloat(), cols, rows)
                            dragging = byPos[cy * cols + cx]
                            dragPos = off
                        }
                    },
                    onDrag = { change, amount -> change.consume(); dragPos += amount },
                    onDragEnd = {
                        val d = dragging
                        if (d != null && gridSize.width > 0) {
                            if (dragPos.y <= removeStripPx) {
                                onRemove(d.id)
                            } else {
                                val (tx, ty) = LauncherGeometry.cellAt(dragPos.x, dragPos.y, gridSize.width.toFloat(), gridSize.height.toFloat(), cols, rows)
                                val target = byPos[ty * cols + tx]
                                when {
                                    target == null -> onMove(d.id, tx, ty)
                                    target.id == d.id -> Unit // dropped back on itself
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
                                Box(Modifier.fillMaxSize().alpha(if (item.id == dragging?.id) 0.3f else 1f), contentAlignment = Alignment.Center) {
                                    WorkspaceCell(item, visualFor, onLaunch, onOpenFolder)
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
    }
}

/** A single grid cell — an app shortcut, or a folder showing a 2×2 icon preview. */
@Composable
private fun WorkspaceCell(
    item: LauncherItem,
    visualFor: (String?) -> AppVisual?,
    onLaunch: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
) {
    when (item.type) {
        ItemType.FOLDER -> FolderTile(item, visualFor, onClick = { onOpenFolder(item.id) })
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
@Composable
private fun AppTile(label: String, icon: AppVisual?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
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
private fun FolderTile(item: LauncherItem, visualFor: (String?) -> AppVisual?, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).glassPanel(radius = 15.dp).padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Empty preview is fine for an empty folder — still a real, openable folder.
            Icon(VisionIcons.Apps, item.title, tint = VisionColors.CyanPrimary, modifier = Modifier.size(22.dp))
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

/** Glass dialog listing a folder's apps (LR2 peek; LR5 brings full management). */
@Composable
private fun FolderDialog(
    title: String,
    children: List<LauncherItem>,
    visualFor: (String?) -> AppVisual?,
    onLaunch: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).glassPanel(radius = 24.dp).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = VisionColors.TextPrimary)
            if (children.isEmpty()) {
                Text("Empty folder", style = MaterialTheme.typography.bodyMedium, color = VisionColors.TextDim)
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(children, key = { it.id }) { child ->
                        val v = visualFor(child.packageName)
                        AppTile(label = v?.label ?: child.label.orEmpty(), icon = v, onClick = { child.packageName?.let(onLaunch) })
                    }
                }
            }
        }
    }
}
