package com.kianirani.jarvis.ui.screen.memory

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.core.memory.MemoryEngine
import com.kianirani.jarvis.core.memory.MemoryFilter
import com.kianirani.jarvis.core.memory.MemoryType
import com.kianirani.jarvis.core.util.RelativeTime
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.VisionIcons
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryUi(
    val count: Int = 0,
    val all: List<MemoryEngine.Stored> = emptyList(),
    val rows: List<MemoryEngine.Stored> = emptyList(),
    val types: List<MemoryType> = emptyList(),
    val activeType: MemoryType? = null,
    val query: String = "",
    val loading: Boolean = false,
)

/**
 * CF4.3 — Memory management. Browses on-device memories via [MemoryEngine], filters
 * by type + a model-independent text query ([MemoryFilter]), and deletes single
 * memories or wipes all. Deletes and browse work even before the embedding model is
 * downloaded (plain Room reads/writes); semantic recall is used elsewhere (chat).
 */
@HiltViewModel
class MemoryViewModel @Inject constructor(private val memory: MemoryEngine) : ViewModel() {
    private val _ui = MutableStateFlow(MemoryUi())
    val ui: StateFlow<MemoryUi> = _ui.asStateFlow()

    fun refresh() = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true)
        val all = memory.browse()
        val count = runCatching { memory.count() }.getOrDefault(all.size)
        recompute(_ui.value.copy(count = count, all = all, types = MemoryFilter.typesPresent(all), loading = false))
    }

    fun setQuery(q: String) = recompute(_ui.value.copy(query = q))

    fun setType(type: MemoryType?) = recompute(_ui.value.copy(activeType = type))

    fun delete(id: String) = viewModelScope.launch {
        memory.forget(id)
        refresh()
    }

    fun clearAll() = viewModelScope.launch {
        memory.clearAll()
        refresh()
    }

    private fun recompute(base: MemoryUi) {
        _ui.value = base.copy(rows = MemoryFilter.filter(base.all, base.query, base.activeType))
    }
}

@Composable
fun MemoryScreen(
    showBack: Boolean = true,
    onBack: () -> Unit = {},
    vm: MemoryViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var confirmingClear by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refresh() }

    Column(
        Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Header: title + (separated) destructive Clear-all entry point.
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (showBack) {
                Box(
                    Modifier.size(40.dp).background(VisionColors.CyanFaint, CircleShape)
                        .border(1.dp, VisionColors.CyanSecondary.copy(alpha = 0.5f), CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) { Icon(VisionIcons.Back, "Back", tint = VisionColors.CyanPrimary, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("Memory", style = MaterialTheme.typography.headlineLarge, color = VisionColors.TextPrimary)
                Text("On-device knowledge graph", style = MaterialTheme.typography.bodySmall, color = VisionColors.TextDim)
            }
            if (ui.count > 0 && !confirmingClear) {
                Box(
                    Modifier.size(40.dp).background(VisionColors.DangerRed.copy(alpha = 0.12f), CircleShape)
                        .clickable { confirmingClear = true; pendingDeleteId = null },
                    contentAlignment = Alignment.Center,
                ) { Icon(VisionIcons.Clear, "Clear all memories", tint = VisionColors.DangerRed, modifier = Modifier.size(20.dp)) }
            }
        }

        if (confirmingClear) {
            ConfirmStrip(
                label = "Clear all ${ui.count} memories?",
                confirmText = "Clear all",
                onConfirm = { confirmingClear = false; vm.clearAll() },
                onCancel = { confirmingClear = false },
            )
        }

        // Stat card.
        Row(
            Modifier.fillMaxWidth().visionEnter(0).glassPanel(radius = 18.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).background(VisionColors.CyanFaint, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(VisionIcons.Memory, null, tint = VisionColors.CyanPrimary, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${ui.count}", style = MaterialTheme.typography.headlineLarge, color = VisionColors.TextPrimary)
                Text("memories stored on device", style = MaterialTheme.typography.bodySmall, color = VisionColors.TextDim)
            }
        }

        // Live local search pill (model-independent).
        Row(
            Modifier.fillMaxWidth().visionEnter(1).glassPanel(radius = 26.dp)
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(VisionIcons.Search, null, tint = VisionColors.TextDim, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it; vm.setQuery(it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = VisionColors.TextPrimary),
                cursorBrush = SolidColor(VisionColors.CyanPrimary),
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search memory…", style = MaterialTheme.typography.bodyLarge, color = VisionColors.TextDim)
                    }
                    inner()
                },
            )
            if (query.isNotEmpty()) {
                Box(
                    Modifier.size(40.dp).clickable { query = ""; vm.setQuery("") },
                    contentAlignment = Alignment.Center,
                ) { Icon(VisionIcons.Close, "Clear search", tint = VisionColors.TextDim, modifier = Modifier.size(18.dp)) }
            }
        }

        // Type filter chips (All + present types).
        if (ui.types.size > 1) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip("All", ui.activeType == null) { vm.setType(null) }
                ui.types.forEach { t ->
                    FilterChip(t.name.lowercase().replaceFirstChar { it.uppercase() }, ui.activeType == t) { vm.setType(t) }
                }
            }
        }

        Text(
            if (ui.query.isBlank() && ui.activeType == null) "RECENT" else "RESULTS",
            style = MaterialTheme.typography.labelSmall, color = VisionColors.CyanSecondary,
        )

        if (ui.rows.isEmpty()) {
            Text(
                if (ui.loading) {
                    "Loading…"
                } else if (ui.count == 0) {
                    "No memories yet — chat with Vision to build context."
                } else {
                    "No memories match this filter."
                },
                style = MaterialTheme.typography.bodyMedium, color = VisionColors.TextDim,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            ui.rows.forEachIndexed { i, m ->
                MemoryRowItem(
                    memory = m,
                    index = i,
                    confirming = pendingDeleteId == m.id,
                    onAskDelete = { pendingDeleteId = m.id; confirmingClear = false },
                    onConfirmDelete = { pendingDeleteId = null; vm.delete(m.id) },
                    onCancelDelete = { pendingDeleteId = null },
                )
            }
        }
    }
}

@Composable
private fun MemoryRowItem(
    memory: MemoryEngine.Stored,
    index: Int,
    confirming: Boolean,
    onAskDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().visionEnter(index + 2).glassPanel(radius = 14.dp).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).background(VisionColors.CyanFaint, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.Memory, null, tint = VisionColors.CyanPrimary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                memory.content, style = MaterialTheme.typography.bodyLarge, color = VisionColors.TextPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${memory.type.name} · ${RelativeTime.ago(memory.createdAt)}",
                style = MaterialTheme.typography.labelSmall, color = VisionColors.CyanSecondary,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (confirming) {
            Text(
                "Delete",
                style = MaterialTheme.typography.labelLarge, color = VisionColors.DangerRed,
                modifier = Modifier.clickable(onClick = onConfirmDelete).padding(horizontal = 10.dp, vertical = 12.dp),
            )
            Text(
                "Cancel",
                style = MaterialTheme.typography.labelLarge, color = VisionColors.TextDim,
                modifier = Modifier.clickable(onClick = onCancelDelete).padding(horizontal = 8.dp, vertical = 12.dp),
            )
        } else {
            Box(
                Modifier.size(40.dp).clickable(onClick = onAskDelete),
                contentAlignment = Alignment.Center,
            ) { Icon(VisionIcons.Delete, "Delete memory", tint = VisionColors.TextDim, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun FilterChip(label: String, active: Boolean, onClick: () -> Unit) {
    val accent = VisionColors.CyanPrimary
    Box(
        Modifier
            .background(
                if (active) VisionColors.CyanFaint else VisionColors.TextDim.copy(alpha = 0.06f),
                CircleShape,
            )
            .border(1.dp, if (active) accent else VisionColors.TextDim.copy(alpha = 0.3f), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) accent else VisionColors.TextDim,
        )
    }
}

@Composable
private fun ConfirmStrip(label: String, confirmText: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().glassPanel(radius = 14.dp)
            .border(1.dp, VisionColors.DangerRed.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = VisionColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(
            confirmText,
            style = MaterialTheme.typography.labelLarge, color = VisionColors.DangerRed,
            modifier = Modifier.clickable(onClick = onConfirm).padding(horizontal = 10.dp, vertical = 12.dp),
        )
        Text(
            "Cancel",
            style = MaterialTheme.typography.labelLarge, color = VisionColors.TextDim,
            modifier = Modifier.clickable(onClick = onCancel).padding(horizontal = 8.dp, vertical = 12.dp),
        )
    }
}
