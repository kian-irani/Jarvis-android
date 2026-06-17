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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlin.math.hypot

/**
 * HOME — Vision OS redesign (2026-06-16). An AI-operating-system home, not an
 * Android launcher: the [VisionOrb] is the visual hero (~45% of the screen),
 * orbited by **neural nodes** wired to it with glowing threads. Everything else
 * is deliberately quiet and spacious:
 *
 *   greeting + weather + settings → the AI-core orb (+ neural nodes) →
 *   "What would you like to do today?" command bar → premium Quick Actions →
 *   Continue Working · Recent Activity · Active Agents · Connected Devices.
 *
 * Reuses [HudViewModel] (clock/command/voice) and [HomeViewModel] (agents/stats).
 * Colours read state-backed [VisionColors]; entrances honour the animation toggle.
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
    // Greet the USER by their own name — not the assistant's name (bug fix).
    val name by home.userName.collectAsStateWithLifecycle()
    val assistantName by home.personaName.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { home.refresh() }

    val activeAgents = agents.count { it.status == AgentStatus.ACTIVE || it.status == AgentStatus.WORKING }

    Column(
        modifier.fillMaxWidth()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        GreetingRow(name = name, assistantName = assistantName, onOpenSettings = onOpenSettings)

        // ── The hero: the AI core, dominating the screen ──────────────────────
        OrbCluster(
            listening = s.isListening,
            // Tapping the core while Vision is speaking interrupts it (BUG-2), else talks.
            onTalk = { if (s.isSpeaking) hud.stopSpeaking() else hud.toggleListening() },
            onRecents = { openSystemRecents(ctx) },
            onMemory = onOpenMemory,
            onProjects = { onQuickAction(QuickAction.TASKS) },
            onAgents = onOpenAgents,
            onAutomation = { onQuickAction(QuickAction.AUTOMATION) },
            modifier = Modifier.fillMaxWidth().visionEnter(0),
        )
        Text(
            if (s.isListening) "LISTENING — speak now" else "tap the core to talk · swipe up for recents",
            style = MaterialTheme.typography.labelMedium,
            color = if (s.isListening) JarvisColors.NeonGreen else JarvisColors.TextDim,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp, bottom = 16.dp),
        )

        CommandBar(s.inputText, hud::onInputChange, hud::sendChat, s.isListening, s.isSpeaking, hud::toggleListening, hud::stopSpeaking, Modifier.fillMaxWidth().visionEnter(1))
        VisionOutput(
            output = s.jarvisOutput,
            isExpanded = s.isOutputExpanded,
            onToggle = hud::toggleOutputExpanded,
            onClear = hud::clearConversation,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(28.dp))

        QuickActionsGrid(order, home.quickActions::move, home.quickActions::reset, onQuickAction, Modifier.fillMaxWidth().visionEnter(2))
        Spacer(Modifier.height(28.dp))

        ContinueWorkingCard(stats, agents, onOpenMemory, Modifier.fillMaxWidth().visionEnter(3))
        Spacer(Modifier.height(16.dp))
        RecentActivityCard(stats, agents, Modifier.fillMaxWidth().visionEnter(4))
        Spacer(Modifier.height(16.dp))

        if (!showSidePanels) {
            Row(Modifier.fillMaxWidth().visionEnter(5), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ActiveAgentsCard(agents, activeAgents, onOpenAgents, Modifier.weight(1f))
                ConnectedDevicesCard(s.nodesOnline, Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Greeting on the left, a glass weather chip + a small settings button on the right. */
@Composable
private fun GreetingRow(name: String, assistantName: String, onOpenSettings: () -> Unit) {
    val ctx = LocalContext.current
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good Morning"; in 12..16 -> "Good Afternoon"; in 17..21 -> "Good Evening"; else -> "Good Night"
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            // Swipe down on the top area opens the system notification shade (RD10).
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragEnd = { if (dy > 60f) expandNotificationShade(ctx); dy = 0f },
                    onVerticalDrag = { _, amount -> dy += amount },
                )
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            // Greet the user by their own name; if unset, greet without a name
            // (never the assistant's name — that was the reported bug).
            Text(if (name.isBlank()) greeting else "$greeting,", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim)
            if (name.isNotBlank()) {
                Text(name, style = MaterialTheme.typography.displaySmall, color = JarvisColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${assistantName.ifBlank { "Vision" }} is ready to assist you", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.glassPanel(radius = 18.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(VisionIcons.Weather, "Weather", tint = JarvisColors.WarningAmber, modifier = Modifier.size(18.dp))
                Text("24°", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(JarvisColors.CyanFaint)
                    .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.5f), CircleShape)
                    .clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) { Icon(VisionIcons.Settings, "Settings", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(20.dp)) }
        }
    }
}

/** Anchor for a neural node: which icon/label, where (fractions of half-extent), what it opens. */
private data class NodeSpec(val icon: ImageVector, val label: String, val fx: Float, val fy: Float, val onClick: () -> Unit)

/**
 * The AI-core hero. The [VisionOrb] fills a near-full-width square (~45% of screen
 * height) and is orbited by four **neural nodes** — small circular satellites wired
 * to the core with glowing threads (drawn in a Canvas behind them), so they read as
 * extensions of Vision rather than buttons. Tap the core to talk; swipe up = recents.
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
    val nodes = listOf(
        NodeSpec(VisionIcons.Memory, "Memory", -0.58f, -0.46f, onMemory),
        NodeSpec(VisionIcons.Projects, "Projects", 0.58f, -0.46f, onProjects),
        NodeSpec(VisionIcons.Agents, "Agents", -0.58f, 0.46f, onAgents),
        NodeSpec(VisionIcons.Automation, "Automation", 0.58f, 0.46f, onAutomation),
    )
    val thread = VisionColors.CyanPrimary
    BoxWithConstraints(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        val half = maxWidth / 2

        VisionOrb(
            listening = listening,
            modifier = Modifier.fillMaxSize().padding(10.dp)
                .pointerInput(Unit) { detectTapGestures(onTap = { onTalk() }) }
                .pointerInput(Unit) {
                    var dy = 0f
                    detectVerticalDragGestures(
                        onDragEnd = { if (dy < -80f) onRecents(); dy = 0f },
                        onVerticalDrag = { _, amount -> dy += amount },
                    )
                },
        )

        // Glowing connection threads from the core out to each neural node.
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val hw = size.width / 2f
            val hh = size.height / 2f
            val ringR = minOf(hw, hh) * 0.60f
            nodes.forEach { n ->
                val node = Offset(c.x + n.fx * hw, c.y + n.fy * hh)
                val dx = node.x - c.x; val dy = node.y - c.y
                val len = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
                val start = Offset(c.x + dx / len * ringR, c.y + dy / len * ringR)
                drawLine(thread.copy(alpha = 0.10f), start, node, strokeWidth = minOf(hw, hh) * 0.022f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(thread.copy(alpha = 0.45f), start, node, strokeWidth = minOf(hw, hh) * 0.006f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawCircle(thread.copy(alpha = 0.6f), minOf(hw, hh) * 0.012f, Offset((start.x + node.x) / 2f, (start.y + node.y) / 2f))
            }
        }

        nodes.forEach { n ->
            NeuralNode(n.icon, n.label, n.onClick, Modifier.align(Alignment.Center).offset(x = half * n.fx, y = half * n.fy))
        }
    }
}

/** A small circular neural node — an icon orb with a thin glowing rim + tiny label. */
@Composable
private fun NeuralNode(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .background(VisionColors.SurfaceGlass)
                .border(1.dp, VisionColors.CyanPrimary.copy(alpha = 0.6f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = VisionColors.CyanPrimary, modifier = Modifier.size(20.dp)) }
        Text(label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextSecondary, maxLines = 1)
    }
}

/**
 * The premium command bar — 72dp, 36dp radius, glass + glow, sparkle left, a single
 * multi-state action button on the right. The button is Stop (red) while Vision is
 * speaking so the user can always interrupt (BUG-2), Send (cyan) when there is text,
 * else Mic — distinct icon AND colour per state, never colour alone.
 */
@Composable
private fun CommandBar(
    value: String,
    onChange: (String) -> Unit,
    onSend: () -> Unit,
    listening: Boolean,
    isSpeaking: Boolean,
    onVoice: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier.height(72.dp).glassPanel(radius = 36.dp).padding(start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(VisionColors.PlasmaSweep),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.Spark, "Vision", tint = VisionColors.Background, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = JarvisColors.TextPrimary),
            cursorBrush = SolidColor(JarvisColors.CyanPrimary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("What would you like to do today?", style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                inner()
            },
        )
        Spacer(Modifier.width(8.dp))
        val mode = commandBarMode(isSpeaking = isSpeaking, hasText = value.isNotBlank())
        val accent = when (mode) {
            CommandBarMode.STOP -> JarvisColors.DangerRed
            CommandBarMode.SEND -> JarvisColors.CyanPrimary
            CommandBarMode.MIC -> if (listening) JarvisColors.NeonGreen else JarvisColors.CyanPrimary
        }
        val icon = when (mode) {
            CommandBarMode.STOP -> VisionIcons.Stop
            CommandBarMode.SEND -> VisionIcons.Send
            CommandBarMode.MIC -> VisionIcons.Mic
        }
        val desc = when (mode) {
            CommandBarMode.STOP -> "Stop speaking"
            CommandBarMode.SEND -> "Send"
            CommandBarMode.MIC -> "Voice"
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .background(accent.copy(alpha = if (mode == CommandBarMode.MIC && !listening) 0.12f else 0.20f))
                .border(1.dp, accent.copy(alpha = 0.5f), CircleShape)
                .clickable {
                    when (mode) {
                        CommandBarMode.STOP -> onStop()
                        CommandBarMode.SEND -> onSend()
                        CommandBarMode.MIC -> onVoice()
                    }
                },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, desc, tint = accent, modifier = Modifier.size(20.dp)) }
    }
}

/**
 * BUG-1 — Vision's reply, never truncated. Collapsed it is capped at ~132dp and
 * scrolls; "Show more" expands it (capped taller, still scrolls) for long answers.
 * A discreet Clear control forgets the conversation (BUG-3). No state rides on
 * colour alone — each control pairs an icon with its label.
 */
@Composable
private fun VisionOutput(
    output: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (output.isBlank()) return
    val scroll = rememberScrollState()
    val long = output.length > 280
    Column(modifier.padding(top = 10.dp)) {
        Box(
            Modifier.fillMaxWidth()
                .heightIn(max = if (isExpanded) 320.dp else 132.dp)
                .verticalScroll(scroll),
        ) {
            Text(output, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextSecondary)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (long) {
                Row(
                    Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onToggle)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(if (isExpanded) "Show less" else "Show more", style = MaterialTheme.typography.labelMedium, color = JarvisColors.CyanPrimary)
                    Icon(if (isExpanded) VisionIcons.ExpandLess else VisionIcons.ExpandMore, null, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onClear)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(VisionIcons.Clear, "Clear conversation", tint = JarvisColors.TextDim, modifier = Modifier.size(18.dp))
                Text("Clear", style = MaterialTheme.typography.labelMedium, color = JarvisColors.TextDim)
            }
        }
    }
}

/** Premium Quick Actions — a 3×2 grid of glass cards (radius 24, 16dp gaps). */
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
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            if (edit) {
                Text("RESET", style = MaterialTheme.typography.labelSmall, color = JarvisColors.DangerRed,
                    modifier = Modifier.clickable { onReset() }.padding(horizontal = 8.dp))
            }
            Text(if (edit) "DONE" else "EDIT", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable { edit = !edit }.padding(start = 4.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            order.chunked(3).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    rowItems.forEach { qa ->
                        QuickCard(qa, edit, { onMove(qa, -1) }, { onMove(qa, 1) }, { onAction(qa) }, Modifier.weight(1f))
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun QuickCard(
    qa: QuickAction,
    edit: Boolean,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).clickable(enabled = !edit, onClick = onClick)
            .glassPanel(radius = 24.dp).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(VisionColors.PlasmaSweep),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.forAction(qa), qa.label, tint = VisionColors.Background, modifier = Modifier.size(24.dp)) }
        Text(qa.label, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (edit) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("‹", style = MaterialTheme.typography.titleLarge, color = JarvisColors.CyanPrimary, modifier = Modifier.clickable(onClick = onLeft))
                Text("›", style = MaterialTheme.typography.titleLarge, color = JarvisColors.CyanPrimary, modifier = Modifier.clickable(onClick = onRight))
            }
        }
    }
}

/** "Continue Working" — the most prominent content card: resume the last thread. */
@Composable
private fun ContinueWorkingCard(stats: HomeStats, agents: List<AgentState>, onResume: () -> Unit, modifier: Modifier) {
    val working = agents.firstOrNull { it.status == AgentStatus.WORKING || it.status == AgentStatus.ACTIVE }
    val subtitle = stats.recentTask ?: working?.let { "${it.id.display} agent active" } ?: "Nothing in progress — ask Vision to start"
    Row(
        modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onResume).glassPanel(radius = 24.dp).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(VisionColors.PlasmaSweep),
            contentAlignment = Alignment.Center,
        ) { Icon(VisionIcons.Projects, null, tint = VisionColors.Background, modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Continue Working", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(VisionIcons.Send, "Resume", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(22.dp))
    }
}

/** "Recent Activity" — a few live rows synthesised from tasks, agents and memory. */
@Composable
private fun RecentActivityCard(stats: HomeStats, agents: List<AgentState>, modifier: Modifier) {
    val working = agents.firstOrNull { it.status == AgentStatus.WORKING || it.status == AgentStatus.ACTIVE }
    Column(modifier.glassPanel(radius = 24.dp).padding(18.dp)) {
        Text("Recent Activity", style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
        Spacer(Modifier.height(12.dp))
        ActivityRow(VisionIcons.Tasks, "Tasks", if (stats.tasks > 0) "${stats.tasks} pending" else "all clear")
        ActivityRow(VisionIcons.Agents, "Agents", working?.id?.display?.let { "$it working" } ?: "idle")
        ActivityRow(VisionIcons.Memory, "Memory", "${stats.memories} stored")
    }
}

@Composable
private fun ActivityRow(icon: ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(JarvisColors.CyanFaint),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextSecondary, maxLines = 1)
    }
}

/** Compact "Active Agents" summary card (left of the two-up row). */
@Composable
private fun ActiveAgentsCard(agents: List<AgentState>, activeCount: Int, onOpenAgents: () -> Unit, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onOpenAgents).glassPanel(radius = 24.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Active Agents", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim)
        Text("$activeCount running", style = MaterialTheme.typography.titleLarge, color = JarvisColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            agents.take(4).forEach { a ->
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(JarvisColors.CyanFaint)
                        .border(1.dp, statusColor(a.status).copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(VisionIcons.forAgent(a.id), a.id.display, tint = JarvisColors.CyanPrimary, modifier = Modifier.size(15.dp)) }
            }
        }
    }
}

/** Compact "Connected Devices" summary card (right of the two-up row). */
@Composable
private fun ConnectedDevicesCard(nodesOnline: Int, modifier: Modifier) {
    Column(
        modifier.glassPanel(radius = 24.dp).padding(16.dp),
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
