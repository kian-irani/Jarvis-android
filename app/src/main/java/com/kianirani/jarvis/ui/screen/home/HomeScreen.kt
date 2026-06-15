package com.kianirani.jarvis.ui.screen.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.kianirani.jarvis.ui.theme.VisionIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kianirani.jarvis.data.agent.AgentState
import com.kianirani.jarvis.data.agent.AgentStatus
import com.kianirani.jarvis.data.settings.QuickAction
import com.kianirani.jarvis.service.VisionAccessibilityService
import com.kianirani.jarvis.ui.screen.hud.HudViewModel
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter
import java.util.Calendar

/**
 * HOME (v12 reskin, 2026-06-14) — the orb-launcher home from the design
 * reference: greeting bar → glowing [VisionOrb] → "Ask Vision…" command bar →
 * stats → quick-actions grid → Active Agents card → device/widgets card. Reuses
 * [HudViewModel] for clock/command/voice and [HomeViewModel] for agents + stats.
 * Bottom navigation lives in MainActivity.
 */
@Composable
fun HomeScreen(
    hud: HudViewModel,
    home: HomeViewModel,
    onOpenSettings: () -> Unit,
    onOpenAgents: () -> Unit,
    onOpenMemory: () -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    showSidePanels: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val s by hud.uiState.collectAsState()
    val stats by home.stats.collectAsState()
    val agents by home.agentStates.collectAsState()
    val order by home.quickActions.order.collectAsState()
    val name by home.personaName.collectAsState()
    val ctx = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { home.refresh() }

    val activeAgents = agents.count { it.status == AgentStatus.ACTIVE || it.status == AgentStatus.WORKING }

    Column(
        modifier.fillMaxWidth()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        GreetingTopBar(name = name, time = s.currentTime, brainOnline = s.brainOnline, onOpenSettings = onOpenSettings)
        Spacer(Modifier.height(8.dp))
        VisionOrb(
            listening = s.isListening,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.15f).visionEnter(0)
                // Tap → talk. Swipe up → open the REAL system recent-apps switcher
                // (GLOBAL_ACTION_RECENTS via accessibility) — the only way a 3rd-party
                // launcher can reach Overview, since the hardware button can't be rebound.
                .pointerInput(Unit) { detectTapGestures(onTap = { hud.toggleListening() }) }
                .pointerInput(Unit) {
                    var dy = 0f
                    detectVerticalDragGestures(
                        onDragEnd = { if (dy < -80f) openSystemRecents(ctx); dy = 0f },
                        onVerticalDrag = { _, amount -> dy += amount },
                    )
                },
        )
        Text(
            if (s.isListening) "● LISTENING — speak now" else "tap the orb to talk · swipe up for recent apps",
            style = MaterialTheme.typography.labelMedium,
            color = if (s.isListening) JarvisColors.NeonGreen else JarvisColors.TextDim,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp),
        )
        CommandBar(s.inputText, hud::onInputChange, hud::sendChat, Modifier.fillMaxWidth().visionEnter(1))
        if (s.jarvisOutput.isNotBlank()) {
            Text(
                s.jarvisOutput,
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisColors.TextSecondary,
                maxLines = 4, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        StatsRow(activeAgents, stats.tasks, s.nodesOnline, Modifier.fillMaxWidth().visionEnter(2))
        Spacer(Modifier.height(14.dp))
        QuickActionsGrid(order, home.quickActions::move, home.quickActions::reset, onQuickAction, Modifier.fillMaxWidth().visionEnter(3))
        Spacer(Modifier.height(14.dp))
        if (!showSidePanels) {
            AgentsPanel(agents, onOpenAgents, Modifier.fillMaxWidth().visionEnter(4))
            Spacer(Modifier.height(14.dp))
            WidgetsCard(stats, onOpenMemory, Modifier.fillMaxWidth().visionEnter(5))
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
fun GreetingTopBar(name: String, time: String, brainOnline: Boolean, onOpenSettings: () -> Unit) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Morning"; in 12..16 -> "Afternoon"; in 17..21 -> "Evening"; else -> "Night"
    }
    Row(
        Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("$greeting,", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            Text(name.ifBlank { "VISION" }, style = MaterialTheme.typography.titleLarge, color = JarvisColors.TextPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Static weather chip (real weather lands in a later phase).
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("22°", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
                Icon(VisionIcons.Weather, contentDescription = "Weather", tint = JarvisColors.WarningAmber, modifier = Modifier.size(18.dp))
            }
            Text(time, style = MaterialTheme.typography.labelLarge, color = JarvisColors.CyanPrimary)
            Box(Modifier.size(7.dp).background(if (brainOnline) JarvisColors.NeonGreen else JarvisColors.DangerRed, CircleShape))
            Box(
                Modifier.size(40.dp).background(JarvisColors.CyanFaint, CircleShape)
                    .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) { Icon(VisionIcons.Settings, contentDescription = "Settings", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun CommandBar(value: String, onChange: (String) -> Unit, onSend: () -> Unit, modifier: Modifier) {
    Row(
        modifier.glassPanel(radius = 26.dp).padding(start = 18.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = JarvisColors.TextPrimary),
            cursorBrush = SolidColor(JarvisColors.CyanPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("Ask Vision…", style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextDim)
                inner()
            },
        )
        Box(
            Modifier.size(40.dp).background(VisionColors.PlasmaSweep, CircleShape).clickable(onClick = onSend),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.Send, contentDescription = "Send", tint = VisionColors.Background, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun StatsRow(agents: Int, tasks: Int, devices: Int, modifier: Modifier) {
    Row(
        modifier.glassPanel(radius = 18.dp).padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Stat(VisionIcons.Agents, agents, "Agents")
        StatDivider()
        Stat(VisionIcons.Tasks, tasks, "Tasks")
        StatDivider()
        Stat(VisionIcons.Devices, devices, "Devices")
    }
}

@Composable
private fun Stat(icon: ImageVector, value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(18.dp))
            Text("$value", style = MaterialTheme.typography.titleLarge, color = JarvisColors.TextPrimary)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(JarvisColors.GridLine))
}

@Composable
private fun QuickActionsGrid(
    order: List<QuickAction>,
    onMove: (QuickAction, Int) -> Unit,
    onReset: () -> Unit,
    onAction: (QuickAction) -> Unit,
    modifier: Modifier,
) {
    var edit by remember { mutableStateOf(false) }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("QUICK ACTIONS", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanSecondary)
            Spacer(Modifier.weight(1f))
            if (edit) {
                Text("RESET", style = MaterialTheme.typography.labelSmall, color = JarvisColors.DangerRed,
                    modifier = Modifier.clickable { onReset() }.padding(horizontal = 8.dp))
            }
            Text(if (edit) "DONE" else "EDIT", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable { edit = !edit }.padding(start = 4.dp))
        }
        // Two rows of three for a clean phone grid.
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            order.chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { qa ->
                        QuickTile(qa, edit, { onMove(qa, -1) }, { onMove(qa, 1) }, { onAction(qa) }, Modifier.weight(1f))
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(
    qa: QuickAction,
    edit: Boolean,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .glassPanel(radius = 16.dp)
            .clickable(enabled = !edit, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(46.dp).background(JarvisColors.CyanFaint, CircleShape)
                .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.forAction(qa), contentDescription = qa.label, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(22.dp)) }
        Text(qa.label, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextPrimary, maxLines = 1)
        if (edit) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("‹", style = MaterialTheme.typography.titleLarge, color = JarvisColors.CyanPrimary, modifier = Modifier.clickable(onClick = onLeft))
                Text("›", style = MaterialTheme.typography.titleLarge, color = JarvisColors.CyanPrimary, modifier = Modifier.clickable(onClick = onRight))
            }
        }
    }
}

@Composable
fun AgentsPanel(agents: List<AgentState>, onOpenAgents: () -> Unit, modifier: Modifier) {
    Column(modifier.glassPanel(radius = 18.dp).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Active Agents", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Text("MANAGE ›", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable(onClick = onOpenAgents))
        }
        Spacer(Modifier.height(10.dp))
        agents.forEach { a ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(30.dp).background(JarvisColors.CyanFaint, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) { Icon(VisionIcons.forAgent(a.id), contentDescription = a.id.display, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Text(a.id.display, style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                Text(a.status.name, style = MaterialTheme.typography.labelSmall, color = statusColor(a.status))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(9.dp).background(statusColor(a.status), CircleShape))
            }
        }
    }
}

@Composable
private fun WidgetsCard(stats: HomeStats, onOpenMemory: () -> Unit, modifier: Modifier) {
    Column(modifier.glassPanel(radius = 18.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WidgetRow("Recent task", stats.recentTask ?: "no pending tasks")
        WidgetRow(
            "Device",
            buildString {
                append(if (stats.battery in 0..100) "${stats.battery}% battery" else "battery n/a")
                if (stats.freeRamMb >= 0) append(" · ${stats.freeRamMb}MB free")
            },
        )
        WidgetRow("Memory", "${stats.memories} stored")
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpenMemory).padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Smart suggestion", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            Spacer(Modifier.weight(1f))
            Text("Review your memory workspace ›", style = MaterialTheme.typography.bodySmall, color = JarvisColors.CyanPrimary)
        }
    }
}

@Composable
private fun WidgetRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Open the genuine system recent-apps switcher via the accessibility service.
 * If Device Control isn't enabled yet, guide the user to turn it on once — there
 * is no app-level way to drive Overview without it.
 */
private fun openSystemRecents(ctx: Context) {
    val svc = VisionAccessibilityService.instance
    if (svc != null) {
        svc.openRecents()
    } else {
        Toast.makeText(ctx, "Enable Vision Device Control (Accessibility) to open recent apps", Toast.LENGTH_LONG).show()
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

internal fun statusColor(s: AgentStatus): Color = when (s) {
    AgentStatus.ACTIVE -> VisionColors.NeonGreen
    AgentStatus.WORKING -> VisionColors.CyanPrimary
    AgentStatus.IDLE -> VisionColors.WarningAmber
    AgentStatus.OFF -> VisionColors.TextDim
}
