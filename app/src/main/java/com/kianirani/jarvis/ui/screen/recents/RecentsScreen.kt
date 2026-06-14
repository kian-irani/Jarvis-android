package com.kianirani.jarvis.ui.screen.recents

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.glassPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RecentApp(val label: String, val packageName: String, val icon: ImageBitmap, val lastUsed: Long)

/**
 * In-app Recents (open/recently-used apps). As a third-party HOME launcher the
 * system Overview is often unreachable (user bug 2026-06-14: "recents menu does
 * not appear inside Vision"), so we build our own list from UsageStatsManager.
 * Needs the special PACKAGE_USAGE_STATS access, which the user grants once.
 */
@HiltViewModel
class RecentsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _apps = MutableStateFlow<List<RecentApp>>(emptyList())
    val apps = _apps.asStateFlow()
    private val _hasAccess = MutableStateFlow(hasUsageAccess())
    val hasAccess = _hasAccess.asStateFlow()

    fun refresh() {
        _hasAccess.value = hasUsageAccess()
        if (!_hasAccess.value) { _apps.value = emptyList(); return }
        viewModelScope.launch { _apps.value = withContext(Dispatchers.IO) { loadRecents() } }
    }

    fun launch(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(it)
        }
    }

    fun openUsageAccessSettings() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun loadRecents(): List<RecentApp> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val begin = end - 24L * 60 * 60 * 1000 // last 24h
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begin, end) ?: return emptyList()
        val pm = context.packageManager
        return stats
            .filter { it.lastTimeUsed > 0 && it.packageName != context.packageName }
            .groupBy { it.packageName }
            .mapNotNull { (pkg, list) ->
                val last = list.maxOf { it.lastTimeUsed }
                pm.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null // skip non-launchable
                val ai = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull() ?: return@mapNotNull null
                RecentApp(
                    label = pm.getApplicationLabel(ai).toString(),
                    packageName = pkg,
                    icon = pm.getApplicationIcon(ai).toBitmap(96, 96).asImageBitmap(),
                    lastUsed = last,
                )
            }
            .sortedByDescending { it.lastUsed }
            .take(24)
    }
}

/** RECENT APPS screen — open-apps list for launcher mode. */
@Composable
fun RecentsScreen(vm: RecentsViewModel = hiltViewModel(), onBack: () -> Unit = {}) {
    val apps by vm.apps.collectAsState()
    val hasAccess by vm.hasAccess.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }
    Column(
        Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop).systemBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ BACK", style = MaterialTheme.typography.labelLarge, color = JarvisColors.TextDim,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
            Text("RECENT APPS", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.CyanPrimary)
        }
        when {
            !hasAccess -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Vision needs Usage Access to show your open/recent apps as a launcher. " +
                        "برای نمایش برنامه‌های باز، دسترسی Usage Access لازم است.",
                    style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim,
                )
                Text("GRANT USAGE ACCESS", style = MaterialTheme.typography.labelLarge, color = JarvisColors.CyanPrimary,
                    modifier = Modifier.glassPanel(radius = 8.dp).clickable { vm.openUsageAccessSettings() }
                        .padding(horizontal = 14.dp, vertical = 10.dp))
            }
            apps.isEmpty() -> Text("No recent apps yet.", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim)
            else -> LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    Column(
                        Modifier.clickable { vm.launch(app.packageName) }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(app.icon, app.label, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.height(4.dp))
                        Text(app.label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
