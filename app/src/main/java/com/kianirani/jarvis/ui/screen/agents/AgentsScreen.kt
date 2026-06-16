package com.kianirani.jarvis.ui.screen.agents

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.kianirani.jarvis.ui.theme.VisionIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.kianirani.jarvis.data.agent.AgentId
import com.kianirani.jarvis.data.agent.AgentRegistry
import com.kianirani.jarvis.data.agent.AgentState
import com.kianirani.jarvis.data.agent.TrustLevel
import com.kianirani.jarvis.ui.screen.home.statusColor
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AgentsViewModel @Inject constructor(val registry: AgentRegistry) : ViewModel() {
    val states = registry.states
    val history = registry.history
    fun refresh() = registry.refresh()
    fun setTrust(id: AgentId, level: TrustLevel) = registry.setTrust(id, level)
    fun setEnabled(id: AgentId, on: Boolean) = registry.setEnabled(id, on)
}

/**
 * AGENTS — Layer 2 management (v12). Per-agent: live status, enable toggle, and
 * a trust level (Read/Suggest/Auto/Critical) persisted via [AgentRegistry].
 * Shows recent (mock) activity at the bottom.
 */
@Composable
fun AgentsScreen(
    showBack: Boolean = true,
    onBack: () -> Unit = {},
    vm: AgentsViewModel = hiltViewModel(),
) {
    val agents by vm.states.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refresh() }
    val activeCount = agents.count { it.status.name == "ACTIVE" || it.status.name == "WORKING" }
    Column(
        Modifier.fillMaxSize().background(JarvisColors.ScreenBackdrop).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
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
                Text("Agents", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.TextPrimary)
                Text("$activeCount of ${agents.size} active", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            }
        }
        agents.forEachIndexed { i, a ->
            val action = vm.history.firstOrNull { it.agent == a.id }?.text
            AgentCard(a, i, action, vm::setEnabled, vm::setTrust)
        }

        Column(Modifier.fillMaxWidth().visionEnter(agents.size).glassPanel(radius = 14.dp).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RECENT ACTIVITY", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanSecondary)
            vm.history.forEach { h ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(h.agent.display, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.CyanPrimary, modifier = Modifier.width(96.dp))
                    Text(h.text, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary, modifier = Modifier.weight(1f))
                    Text(h.time, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
                }
            }
        }
    }
}

@Composable
private fun AgentCard(a: AgentState, index: Int, currentAction: String?, onEnabled: (AgentId, Boolean) -> Unit, onTrust: (AgentId, TrustLevel) -> Unit) {
    Column(Modifier.fillMaxWidth().visionEnter(index).glassPanel(radius = 16.dp).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(JarvisColors.CyanFaint, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                Icon(VisionIcons.forAgent(a.id), contentDescription = a.id.display, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.id.display, style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
                Text(a.status.name, style = MaterialTheme.typography.labelSmall, color = statusColor(a.status))
            }
            Box(Modifier.size(10.dp).background(statusColor(a.status), CircleShape))
            Spacer(Modifier.width(12.dp))
            MiniSwitch(a.enabled) { onEnabled(a.id, it) }
        }
        // Current action (spec: Status / Progress / Current Action) — latest from history.
        if (a.enabled && currentAction != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(6.dp).background(statusColor(a.status), CircleShape))
                Text(currentAction, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextSecondary,
                    maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        // Trust level selector
        Text("TRUST", style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TrustLevel.entries.forEach { lvl ->
                val sel = a.trust == lvl
                Text(
                    lvl.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sel) JarvisColors.CyanPrimary else JarvisColors.TextDim,
                    modifier = Modifier.weight(1f)
                        .border(1.dp, if (sel) JarvisColors.CyanPrimary else JarvisColors.Border, RoundedCornerShape(6.dp))
                        .background(if (sel) JarvisColors.CyanFaint else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable(enabled = a.enabled) { onTrust(a.id, lvl) }
                        .padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Text(a.trust.desc, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
    }
}

@Composable
private fun MiniSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val track = if (checked) JarvisColors.CyanPrimary.copy(alpha = 0.35f) else JarvisColors.GridLine
    val knob = if (checked) JarvisColors.CyanPrimary else JarvisColors.TextDim
    Box(
        Modifier.width(42.dp).height(22.dp).background(track, RoundedCornerShape(11.dp))
            .border(1.dp, knob.copy(alpha = 0.6f), RoundedCornerShape(11.dp))
            .clickable { onChange(!checked) }.padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) { Box(Modifier.size(16.dp).background(knob, CircleShape)) }
}
