package com.kianirani.jarvis.ui.screen.workspace

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.window.Dialog
import com.kianirani.jarvis.data.launcher.Container
import com.kianirani.jarvis.data.launcher.ItemType
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
 */
@Composable
private fun WorkspacePage(
    layout: LauncherLayout,
    page: Int,
    visualFor: (String?) -> AppVisual?,
    onLaunch: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells = layout.cells(Container.WORKSPACE, page)
    val byPos = remember(cells) { cells.associateBy { it.cellY * layout.gridCols + it.cellX } }
    Column(
        modifier.systemBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        for (y in 0 until layout.gridRows) {
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (x in 0 until layout.gridCols) {
                    val item = byPos[y * layout.gridCols + x]
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        if (item != null) {
                            WorkspaceCell(item, visualFor, onLaunch, onOpenFolder)
                        }
                    }
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
