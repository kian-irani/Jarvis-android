package com.kianirani.jarvis.ui.screen.drawer

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.VisionIcons
import com.kianirani.jarvis.ui.theme.glassPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Drawer category buckets — derived from [ApplicationInfo.category] + the system flag. */
enum class AppCategory(val label: String) {
    ALL("All"), RECENT("Recent"), COMMUNICATION("Communication"),
    PRODUCTIVITY("Productivity"), MEDIA("Media"), TOOLS("Tools"), SYSTEM("System"),
}

/** Filter chips shown above the grid (Recent first, then static buckets). */
private val FILTER_CHIPS = listOf(
    AppCategory.ALL, AppCategory.RECENT, AppCategory.COMMUNICATION,
    AppCategory.PRODUCTIVITY, AppCategory.MEDIA, AppCategory.TOOLS, AppCategory.SYSTEM,
)

data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap,
    val category: AppCategory = AppCategory.TOOLS,
)

/**
 * App drawer for launcher mode (USER DIRECTIVE 2026-06-12: Vision must be a
 * real HOME launcher with a drawer of every installed app).
 */
@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatHistory: com.kianirani.jarvis.data.ai.ChatHistoryStore,
) : ViewModel() {
    /** P6 AnySearch-lite: the same query also searches conversation memory. */
    fun searchMemory(q: String): List<com.kianirani.jarvis.data.ai.ChatTurn> =
        if (q.length < 3) emptyList()
        else chatHistory.all().filter { it.text.contains(q, ignoreCase = true) }.takeLast(3)

    // P10 digital-twin-lite: launch counters drive the FREQUENT row + Recent filter.
    private val counts = context.getSharedPreferences("vision_app_usage", Context.MODE_PRIVATE)
    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps = _apps.asStateFlow()
    private val _frequent = MutableStateFlow<List<AppEntry>>(emptyList())
    val frequent = _frequent.asStateFlow()
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { loadApps() }
            _apps.value = loaded
            _frequent.value = loaded
                .filter { counts.getInt(it.packageName, 0) > 0 }
                .sortedByDescending { counts.getInt(it.packageName, 0) }
                .take(4)
        }
    }

    fun onQuery(q: String) { _query.value = q }

    /** Launch count for a package — drives the Recent category ordering. */
    fun usageCount(packageName: String): Int = counts.getInt(packageName, 0)

    fun launch(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
            counts.edit().putInt(packageName, counts.getInt(packageName, 0) + 1).apply()
        }
    }

    private fun loadApps(): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        // MATCH_ALL + QUERY_ALL_PACKAGES: some OEM builds return an empty list
        // for flag 0 once a third-party launcher holds HOME (user bug 2026-06-12).
        @Suppress("DEPRECATION")
        val resolved = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, android.content.pm.PackageManager.ResolveInfoFlags.of(android.content.pm.PackageManager.MATCH_ALL.toLong()))
        } else {
            pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_ALL)
        }
        return resolved
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                AppEntry(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
                    category = categoryOf(it.activityInfo.applicationInfo),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}

/** Bucket an app by its declared store category, falling back to system/tools. */
private fun categoryOf(ai: ApplicationInfo): AppCategory {
    val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    return when (ai.category) {
        ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_NEWS -> AppCategory.COMMUNICATION
        ApplicationInfo.CATEGORY_PRODUCTIVITY, ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.PRODUCTIVITY
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE, ApplicationInfo.CATEGORY_GAME -> AppCategory.MEDIA
        else -> if (isSystem) AppCategory.SYSTEM else AppCategory.TOOLS
    }
}

/**
 * RD3 app drawer (2026-06-16) — rebuilt to the orb-launcher reference: a clean
 * full-screen surface with a glass back chip + "Applications" title, a glass
 * search pill, a horizontal **category filter** row (Recent + buckets derived from
 * each app's store category), and a **5-column rounded-icon grid** that leads with
 * the special Vision Settings / Vision Hub destinations. Reuses the existing
 * [AppDrawerViewModel] (QUERY_ALL_PACKAGES load, usage counters, memory search).
 */
@Composable
fun AppDrawerScreen(
    vm: AppDrawerViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    showBack: Boolean = true,
    onOpenSettings: () -> Unit = {},
    onOpenHub: () -> Unit = {},
) {
    val apps by vm.apps.collectAsState()
    val frequent by vm.frequent.collectAsState()
    val query by vm.query.collectAsState()
    var category by remember { mutableStateOf(AppCategory.ALL) }

    val base = when (category) {
        AppCategory.ALL -> apps
        AppCategory.RECENT -> apps.filter { vm.usageCount(it.packageName) > 0 }
            .sortedByDescending { vm.usageCount(it.packageName) }
        else -> apps.filter { it.category == category }
    }
    val filtered = if (query.isBlank()) base else apps.filter { it.label.contains(query, ignoreCase = true) }
    val showExtras = query.isBlank() && category == AppCategory.ALL

    Column(
        Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop).systemBarsPadding().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header — glass back chip + title + live count.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showBack) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(JarvisColors.CyanFaint)
                        .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.5f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(VisionIcons.Back, "Back", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
            }
            Text("Applications", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Text("${filtered.size}", style = MaterialTheme.typography.labelMedium, color = JarvisColors.TextDim)
        }

        // Glass search pill.
        Row(
            Modifier.fillMaxWidth().glassPanel(radius = 26.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(VisionIcons.Search, null, tint = JarvisColors.TextDim, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query, onValueChange = vm::onQuery, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = JarvisColors.TextPrimary),
                cursorBrush = SolidColor(JarvisColors.CyanPrimary),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search apps…", color = JarvisColors.TextDim, style = MaterialTheme.typography.bodyLarge)
                    inner()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Category filter chips (hidden while searching).
        if (query.isBlank()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FILTER_CHIPS.forEach { c ->
                    CategoryChip(c.label, selected = c == category) { category = c }
                }
            }
        }

        // Memory hits (P6 AnySearch-lite) when actively searching.
        if (query.length >= 3) {
            val memHits = vm.searchMemory(query)
            if (memHits.isNotEmpty()) {
                Text("FROM MEMORY", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanSecondary)
                memHits.forEach { t ->
                    Text(
                        "${if (t.role == "user") "you" else "vision"}: ${t.text}",
                        style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().glassPanel(radius = 12.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }

        // The 5-column rounded-icon grid (spec: "Grid: 5 Columns, Rounded Icons").
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (showExtras && frequent.isNotEmpty()) {
                header("Recent")
                items(frequent, key = { "freq_${it.packageName}" }) { app -> AppTile(app.label, app.icon, accent = true) { vm.launch(app.packageName) } }
                header("All apps")
            }
            if (showExtras) {
                item(key = "__vision_settings") { SpecialTile(VisionIcons.Settings, "Settings", onOpenSettings) }
                item(key = "__vision_hub") { SpecialTile(VisionIcons.Spark, "Vision Hub", onOpenHub) }
            }
            items(filtered, key = { it.packageName }) { app -> AppTile(app.label, app.icon) { vm.launch(app.packageName) } }
        }
    }
}

/** A full-width section header inside the grid. */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.header(title: String) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextSecondary, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A scrollable category filter chip — selected fills with the accent. */
@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (selected) JarvisColors.CyanPrimary else JarvisColors.CyanFaint)
            .border(1.dp, if (selected) JarvisColors.CyanPrimary else JarvisColors.CyanSecondary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label, style = MaterialTheme.typography.labelMedium,
            color = if (selected) VisionColors.Background else JarvisColors.TextSecondary,
        )
    }
}

/** One app in the grid — a rounded-icon surface with a label below. */
@Composable
private fun AppTile(label: String, icon: ImageBitmap, accent: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(16.dp))
                .background(if (accent) JarvisColors.CyanFaint else VisionColors.Surface.copy(alpha = 0.55f))
                .border(1.dp, JarvisColors.Border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { Image(icon, contentDescription = label, Modifier.size(38.dp).clip(RoundedCornerShape(11.dp))) }
        Text(
            label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

/** A distinct accent-tinted launcher tile for built-in Vision destinations. */
@Composable
private fun SpecialTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(VisionColors.PlasmaSweep)
                .border(1.dp, VisionColors.TextPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = VisionColors.Background, modifier = Modifier.size(28.dp)) }
        Text(
            label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}
