package com.kianirani.jarvis.ui.screen.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kianirani.jarvis.data.agent.AgentState
import com.kianirani.jarvis.data.agent.AgentStatus
import com.kianirani.jarvis.data.settings.QuickAction
import com.kianirani.jarvis.service.VisionAccessibilityService
import com.kianirani.jarvis.ui.screen.hud.HudViewModel
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.VisionIcons
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter
import java.util.Calendar

/**
 * HOME (RD2, 2026-06-16) — rebuilt screen-for-screen against the orb-launcher
 * reference in `Example/`:
 *
 *   VISION OS header → greeting + weather → AI-core [VisionOrb] ringed by four
 *   floating satellite chips (Memory · Projects · Agents · Automation) →
 *   "What would you like to do today?" command bar → single-row Quick Access →
 *   Today's Overview card → AI Status card (donut + CPU/RAM/Battery) →
 *   Active Agents + Connected Devices two-up.
 *
 * Reuses [HudViewModel] for clock/command/voice and [HomeViewModel] for agents +
 * device stats. Bottom navigation lives in MainActivity. Everything reads the
 * state-backed [VisionColors] so theme/accent recolour live; entrances honour the
 * global animation toggle via [visionEnter] (reduced-motion safe).
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
    val s by hud.uiState.collectAsStateWithLifecycle()
    val stats by home.stats.collectAsStateWithLifecycle()
    val agents by home.agentStates.collectAsStateWithLifecycle()
    val order by home.quickActions.order.collectAsStateWithLifecycle()
    val name by home.personaName.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { home.refresh() }

    val activeAgents = agents.count { it.status == AgentStatus.ACTIVE || it.status == AgentStatus.WORKING }

    Column(
        modifier.fillMaxWidth()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        VisionOsHeader(brainOnline = s.brainOnline, onOpenSettings = onOpenSettings)
        Spacer(Modifier.height(10.dp))
        GreetingRow(name = name)
        Spacer(Modifier.height(4.dp))

        OrbCluster(
            listening = s.isListening,
            onTalk = hud::toggleListening,
            onRecents = { openSystemRecents(ctx) },
            onMemory = onOpenMemory,
            onProjects = { onQuickAction(QuickAction.TASKS) },
            onAgents = onOpenAgents,
            onAutomation = { onQuickAction(QuickAction.AUTOMATION) },
            modifier = Modifier.fillMaxWidth().visionEnter(0),
        )
        Text(
            if (s.isListening) "LISTENING — speak now" else "tap the orb to talk · swipe up for recent apps",
            style = MaterialTheme.typography.labelMedium,
            color = if (s.isListening) JarvisColors.NeonGreen else JarvisColors.TextDim,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp),
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
        Spacer(Modifier.height(16.dp))

        QuickAccessRow(order, home.quickActions::move, home.quickActions::reset, onQuickAction, Modifier.fillMaxWidth().visionEnter(2))
        Spacer(Modifier.height(14.dp))

        OverviewCard(stats, agents, onOpenMemory, Modifier.fillMaxWidth().visionEnter(3))
        Spacer(Modifier.height(14.dp))

        AiStatusCard(stats, s.nodesOnline, s.brainOnline, Modifier.fillMaxWidth().visionEnter(4))
        Spacer(Modifier.height(14.dp))

        if (!showSidePanels) {
            Row(Modifier.fillMaxWidth().visionEnter(5), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ActiveAgentsCard(agents, activeAgents, onOpenAgents, Modifier.weight(1f))
                ConnectedDevicesCard(s.nodesOnline, Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))
        }
    }
}

/** Centered "VISION OS / AI-NATIVE LAUNCHER" wordmark with a ghost settings gear. */
@Composable
private fun VisionOsHeader(brainOnline: Boolean, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    // Launcher gesture (RD10): a downward pull on the header opens the system
    // notification shade — the classic home-screen swipe-down.
    val headerGesture = Modifier.pointerInput(Unit) {
        var dy = 0f
        detectVerticalDragGestures(
            onDragEnd = { if (dy > 60f) expandNotificationShade(ctx); dy = 0f },
            onVerticalDrag = { _, amount -> dy += amount },
        )
    }
    Box(Modifier.fillMaxWidth().height(52.dp).then(headerGesture), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "VISION OS",
                style = MaterialTheme.typography.titleLarge,
                color = JarvisColors.TextPrimary,
            )
            Text(
                "AI-NATIVE LAUNCHER",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisColors.TextDim,
            )
        }
        // Brain heartbeat dot (left) + settings (right) tucked into the header rail.
        Box(
            Modifier.align(Alignment.CenterStart).size(8.dp)
                .background(if (brainOnline) JarvisColors.NeonGreen else JarvisColors.DangerRed, CircleShape),
        )
        Box(
            Modifier.align(Alignment.CenterEnd).size(40.dp).clip(CircleShape)
                .background(JarvisColors.CyanFaint)
                .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.Settings, "Settings", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(20.dp)) }
    }
}

/** Greeting on the left, a glass weather chip on the right (static until weather phase). */
@Composable
private fun GreetingRow(name: String) {
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning"; in 12..16 -> "Good Afternoon"; in 17..21 -> "Good Evening"; else -> "Good Night"
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text("$greeting,", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim)
            Text(name.ifBlank { "Vision" }, style = MaterialTheme.typography.headlineLarge, color = JarvisColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Vision is ready to assist you", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Row(
            Modifier.glassPanel(radius = 18.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(VisionIcons.Weather, "Weather", tint = JarvisColors.WarningAmber, modifier = Modifier.size(22.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("24°", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
                Text("Mostly Cloudy", style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
            }
        }
    }
}

/**
 * The AI-core hero: the [VisionOrb] centred in a square box, ringed by four glass
 * satellite chips at the corners (Memory · Projects · Agents · Automation), each a
 * tappable shortcut. Tap the orb to talk; swipe up opens system recents.
 */
@Composable
private fun OrbCluster(
    listening: Boolean,
    onTalk: () -> Unit,
    onRecents: () -> Unit,
    onMemory: () -> Unit,
    onProjects: () -> Unit,
    onAgents: () -> Unit,
    onAutomation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        VisionOrb(
            listening = listening,
            modifier = Modifier.fillMaxSize().padding(34.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = { onTalk() }) }
                .pointerInput(Unit) {
                    var dy = 0f
                    detectVerticalDragGestures(
                        onDragEnd = { if (dy < -80f) onRecents(); dy = 0f },
                        onVerticalDrag = { _, amount -> dy += amount },
                    )
                },
        )
        SatelliteNode(VisionIcons.Memory, "Memory", onMemory, Modifier.align(Alignment.TopStart).offset(x = 4.dp, y = 12.dp))
        SatelliteNode(VisionIcons.Projects, "Projects", onProjects, Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 12.dp))
        SatelliteNode(VisionIcons.Agents, "Agents", onAgents, Modifier.align(Alignment.BottomStart).offset(x = 4.dp, y = (-12).dp))
        SatelliteNode(VisionIcons.Automation, "Automation", onAutomation, Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-12).dp))
    }
}

/** A small glass chip orbiting the orb — circular icon badge + label. */
@Composable
private fun SatelliteNode(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick)
            .glassPanel(radius = 20.dp).padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            Modifier.size(28.dp).background(JarvisColors.CyanFaint, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(16.dp)) }
        Text(label, style = MaterialTheme.typography.labelMedium, color = JarvisColors.TextPrimary, maxLines = 1)
    }
}

@Composable
private fun CommandBar(value: String, onChange: (String) -> Unit, onSend: () -> Unit, modifier: Modifier) {
    Row(
        modifier.glassPanel(radius = 28.dp).padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).background(VisionColors.PlasmaSweep, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.Spark, "Vision", tint = VisionColors.Background, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = JarvisColors.TextPrimary),
            cursorBrush = SolidColor(JarvisColors.CyanPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("What would you like to do today?", style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                inner()
            },
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(40.dp).background(JarvisColors.CyanFaint, CircleShape)
                .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.5f), CircleShape)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.Send, "Send", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(18.dp)) }
    }
}

/** Single-row Quick Access (reference): six compact glass tiles + edit/reorder. */
@Composable
private fun QuickAccessRow(
    order: List<QuickAction>,
    onMove: (QuickAction, Int) -> Unit,
    onReset: () -> Unit,
    onAction: (QuickAction) -> Unit,
    modifier: Modifier,
) {
    var edit by remember { mutableStateOf(false) }
    Column(modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Quick Access", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            if (edit) {
                Text("RESET", style = MaterialTheme.typography.labelSmall, color = JarvisColors.DangerRed,
                    modifier = Modifier.clickable { onReset() }.padding(horizontal = 8.dp))
            }
            Text(if (edit) "DONE" else "EDIT", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable { edit = !edit }.padding(start = 4.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            order.forEach { qa ->
                QuickTile(qa, edit, { onMove(qa, -1) }, { onMove(qa, 1) }, { onAction(qa) }, Modifier.weight(1f))
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
        modifier.clip(RoundedCornerShape(16.dp)).clickable(enabled = !edit, onClick = onClick)
            .glassPanel(radius = 16.dp).padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(40.dp).background(JarvisColors.CyanFaint, CircleShape)
                .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.forAction(qa), qa.label, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(20.dp)) }
        Text(qa.label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (edit) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("‹", style = MaterialTheme.typography.titleMedium, color = JarvisColors.CyanPrimary, modifier = Modifier.clickable(onClick = onLeft))
                Text("›", style = MaterialTheme.typography.titleMedium, color = JarvisColors.CyanPrimary, modifier = Modifier.clickable(onClick = onRight))
            }
        }
    }
}

/** "Today's Overview" — a few live activity rows synthesised from stats + agents. */
@Composable
private fun OverviewCard(stats: HomeStats, agents: List<AgentState>, onOpenMemory: () -> Unit, modifier: Modifier) {
    val working = agents.firstOrNull { it.status == AgentStatus.WORKING || it.status == AgentStatus.ACTIVE }
    Column(modifier.glassPanel(radius = 20.dp).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Today's Overview", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Text("View all ›", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable(onClick = onOpenMemory))
        }
        Spacer(Modifier.height(12.dp))
        OverviewItem(VisionIcons.Tasks, "Pending tasks", if (stats.tasks > 0) "${stats.tasks} to review" else "all clear", stats.recentTask?.takeIf { stats.tasks > 0 } ?: "now")
        OverviewItem(VisionIcons.Agents, "Agent activity", working?.let { it.id.display } ?: "idle", working?.status?.name ?: "—")
        OverviewItem(VisionIcons.Memory, "Memory", "${stats.memories} stored", "synced")
    }
}

@Composable
private fun OverviewItem(icon: ImageVector, title: String, subtitle: String, trailing: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(34.dp).background(JarvisColors.CyanFaint, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextPrimary, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(trailing, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextSecondary, maxLines = 1)
    }
}

/** "AI Status" — operational banner + CPU/RAM/Battery metrics with a donut gauge. */
@Composable
private fun AiStatusCard(stats: HomeStats, nodesOnline: Int, brainOnline: Boolean, modifier: Modifier) {
    // No real CPU read; derive a calm "load" from agents-implied work is out of scope,
    // so show RAM (live), Battery (live), and Nodes online. The donut tracks battery.
    val battery = stats.battery.coerceIn(0, 100)
    Column(modifier.glassPanel(radius = 20.dp).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("AI Status", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.size(8.dp).background(if (brainOnline) JarvisColors.NeonGreen else JarvisColors.WarningAmber, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(if (brainOnline) "All systems operational" else "Local-only mode",
                style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric(VisionIcons.Ram, "RAM", if (stats.freeRamMb >= 0) "${stats.freeRamMb} MB free" else "n/a")
                Metric(VisionIcons.Cpu, "Nodes", if (nodesOnline > 0) "$nodesOnline online" else "device only")
                Metric(VisionIcons.Battery, "Battery", if (stats.battery in 0..100) "${stats.battery}%" else "n/a")
            }
            Spacer(Modifier.width(12.dp))
            DonutGauge(percent = if (stats.battery in 0..100) battery / 100f else 0f, label = if (stats.battery in 0..100) "$battery%" else "—")
        }
    }
}

@Composable
private fun Metric(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary)
    }
}

/** Small plasma-stroked progress ring with a centred value label. */
@Composable
private fun DonutGauge(percent: Float, label: String) {
    Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
        val track = JarvisColors.GridLine
        val sweep = VisionColors.PlasmaSweep
        Canvas(Modifier.fillMaxSize()) {
            val w = size.minDimension * 0.12f
            val inset = w / 2f
            val arcSize = Size(size.width - w, size.height - w)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
            drawArc(sweep, -90f, 360f * percent.coerceIn(0f, 1f), false, topLeft, arcSize, style = Stroke(w, cap = StrokeCap.Round))
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
    }
}

/** Compact "Active Agents" summary card (left of the two-up row). */
@Composable
private fun ActiveAgentsCard(agents: List<AgentState>, activeCount: Int, onOpenAgents: () -> Unit, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).clickable(onClick = onOpenAgents).glassPanel(radius = 18.dp).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Active Agents", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim)
        Text("$activeCount running", style = MaterialTheme.typography.titleLarge, color = JarvisColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            agents.take(4).forEach { a ->
                Box(
                    Modifier.size(26.dp).background(JarvisColors.CyanFaint, CircleShape)
                        .border(1.dp, statusColor(a.status).copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(VisionIcons.forAgent(a.id), a.id.display, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(14.dp)) }
            }
        }
    }
}

/** Compact "Connected Devices" summary card (right of the two-up row). */
@Composable
private fun ConnectedDevicesCard(nodesOnline: Int, modifier: Modifier) {
    Column(
        modifier.glassPanel(radius = 18.dp).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Connected Devices", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim)
        Text("${nodesOnline + 1} connected", style = MaterialTheme.typography.titleLarge, color = JarvisColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(VisionIcons.Devices, "This device", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(20.dp))
            repeat(nodesOnline.coerceAtMost(3)) {
                Icon(VisionIcons.Cpu, "Node", tint = JarvisColors.CyanSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * Open the genuine system recent-apps switcher via the accessibility service.
 * If Device Control isn't enabled yet, guide the user to turn it on once — there
 * is no app-level way to drive Overview without it.
 */
/**
 * Open the system notification shade (RD10 launcher gesture). Uses the hidden
 * StatusBarManager#expandNotificationsPanel via reflection — best-effort, since
 * some OEM builds restrict it; failure is silent (the gesture just no-ops).
 */
private fun expandNotificationShade(ctx: Context) {
    runCatching {
        val sb = ctx.getSystemService("statusbar")
        Class.forName("android.app.StatusBarManager")
            .getMethod("expandNotificationsPanel")
            .invoke(sb)
    }
}

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
