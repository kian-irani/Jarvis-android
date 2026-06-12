package com.kianirani.jarvis.ui.screen.drawer

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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

data class AppEntry(val label: String, val packageName: String, val icon: ImageBitmap)

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

    // P10 digital-twin-lite: launch counters drive the FREQUENT row.
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
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                AppEntry(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    icon = it.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}

@Composable
fun AppDrawerScreen(vm: AppDrawerViewModel = hiltViewModel(), onBack: () -> Unit = {}) {
    val apps by vm.apps.collectAsState()
    val frequent by vm.frequent.collectAsState()
    val query by vm.query.collectAsState()
    val filtered = if (query.isBlank()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    LocalContext.current
    Column(
        Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop).systemBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ BACK", style = MaterialTheme.typography.labelLarge, color = JarvisColors.TextDim,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
            Text("APPLICATIONS", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.CyanPrimary)
            Spacer(Modifier.weight(1f))
            Text("${filtered.size}", style = MaterialTheme.typography.labelMedium, color = JarvisColors.TextDim)
        }
        BasicTextField(
            value = query, onValueChange = vm::onQuery, singleLine = true,
            textStyle = TextStyle(color = JarvisColors.TextPrimary),
            cursorBrush = SolidColor(JarvisColors.CyanPrimary),
            decorationBox = { inner ->
                Row(
                    Modifier.fillMaxWidth()
                        .border(1.dp, JarvisColors.Border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (query.isEmpty()) {
                        Text("search apps…", color = JarvisColors.TextDim, style = MaterialTheme.typography.bodyMedium)
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (query.length >= 3) {
            val memHits = vm.searchMemory(query)
            if (memHits.isNotEmpty()) {
                Text("MEMORY", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanSecondary)
                memHits.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth()
                            .border(1.dp, JarvisColors.Border, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "${if (t.role == "user") "you" else "vision"}: ${t.text}",
                            style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        if (frequent.isNotEmpty() && query.isBlank()) {
            Text("FREQUENT", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanSecondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                frequent.forEach { app ->
                    Column(
                        Modifier.clickable { vm.launch(app.packageName) }.padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Column(Modifier.glassPanel(radius = 14.dp).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(app.icon, contentDescription = app.label, Modifier.size(40.dp))
                        }
                        Text(app.label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 76.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filtered, key = { it.packageName }) { app ->
                Column(
                    Modifier
                        .clickable { vm.launch(app.packageName) }
                        .padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Column(
                        Modifier.glassPanel(radius = 14.dp).padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(app.icon, contentDescription = app.label, Modifier.size(44.dp))
                    }
                    Text(
                        app.label, style = MaterialTheme.typography.labelSmall,
                        color = JarvisColors.TextPrimary, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
