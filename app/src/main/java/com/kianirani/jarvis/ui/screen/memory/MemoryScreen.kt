package com.kianirani.jarvis.ui.screen.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionIcons
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryRow(val title: String, val subtitle: String)
data class MemoryUi(val count: Int = 0, val rows: List<MemoryRow> = emptyList(), val searching: Boolean = false)

@HiltViewModel
class MemoryViewModel @Inject constructor(private val memory: MemoryRepository) : ViewModel() {
    private val _ui = MutableStateFlow(MemoryUi())
    val ui: StateFlow<MemoryUi> = _ui.asStateFlow()

    fun refresh() = viewModelScope.launch {
        val count = runCatching { memory.count() }.getOrDefault(0)
        val recent = runCatching { memory.list(null, 30, 0) }.getOrDefault(emptyList())
            .map { MemoryRow(it.content.take(80), it.type) }
        _ui.value = MemoryUi(count, recent, false)
    }

    fun search(q: String) = viewModelScope.launch {
        if (q.isBlank()) { refresh(); return@launch }
        _ui.value = _ui.value.copy(searching = true)
        val hits = runCatching { memory.search(q, 20) }.getOrDefault(emptyList())
            .map { MemoryRow(it.content.take(80), "match ${(it.score * 100).toInt()}%") }
        _ui.value = _ui.value.copy(rows = hits, searching = false)
    }
}

/**
 * MEMORY — Layer 3 workspace (v12). Shows on-device memory count, recent
 * episodic memories, and semantic search over [MemoryRepository].
 */
@Composable
fun MemoryScreen(
    showBack: Boolean = true,
    onBack: () -> Unit = {},
    vm: MemoryViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refresh() }
    Column(
        Modifier.fillMaxSize().background(JarvisColors.ScreenBackdrop).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showBack) {
                Box(
                    Modifier.size(40.dp).background(JarvisColors.CyanFaint, CircleShape)
                        .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.5f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(VisionIcons.Back, "Back", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Memory", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.TextPrimary)
                Text("On-device knowledge graph", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            }
        }

        Row(
            Modifier.fillMaxWidth().visionEnter(0).glassPanel(radius = 18.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).background(JarvisColors.CyanFaint, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(VisionIcons.Memory, null, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${ui.count}", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.TextPrimary)
                Text("memories stored on device", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            }
        }

        // Glass search pill — leading search icon + send button.
        Row(Modifier.fillMaxWidth().visionEnter(1).glassPanel(radius = 26.dp).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(VisionIcons.Search, null, tint = JarvisColors.TextDim, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = JarvisColors.TextPrimary),
                cursorBrush = SolidColor(JarvisColors.CyanPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search(query) }),
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                decorationBox = { inner -> if (query.isEmpty()) Text("Search memory…", style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextDim); inner() },
            )
            Box(
                Modifier.size(40.dp).background(JarvisColors.CyanFaint, CircleShape)
                    .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.5f), CircleShape)
                    .clickable { vm.search(query) },
                contentAlignment = Alignment.Center,
            ) { Icon(VisionIcons.Send, "Search", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(18.dp)) }
        }

        Text(
            if (query.isBlank()) "RECENT" else "RESULTS",
            style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanSecondary,
        )

        if (ui.rows.isEmpty()) {
            Text(if (ui.searching) "Searching…" else "No memories yet — chat with Vision to build context.",
                style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim,
                modifier = Modifier.padding(top = 4.dp))
        } else {
            ui.rows.forEachIndexed { i, r ->
                Row(Modifier.fillMaxWidth().visionEnter(i + 2).glassPanel(radius = 14.dp).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).background(JarvisColors.CyanFaint, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(VisionIcons.Memory, null, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextPrimary,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(r.subtitle, style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanSecondary)
                    }
                }
            }
        }
    }
}
