package com.kianirani.jarvis.ui.screen.hud

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.kianirani.jarvis.ui.theme.*
import kotlin.math.*

data class NodeInfo(val id: String, val name: String, val ip: String, val role: String, val online: Boolean, val cpu: Int, val ram: Int, val ping: Int)
enum class EventLevel { OK, INFO, WARN, ERR }
data class LogEvent(val time: String, val message: String, val level: EventLevel)
data class HudUiState(
    val nodes: List<NodeInfo> = emptyList(),
    val brainOnline: Boolean = false, val nodesOnline: Int = 0, val groqOnline: Boolean = false,
    val brainCpu: Float = 0f, val brainRam: Float = 0f, val brainNet: Float = 0f, val brainDiskIo: Float = 0f,
    val jarvisOutput: String = "", val inputText: String = "", val isListening: Boolean = false,
    val waveformAmplitudes: List<Float> = List(40) { 0.05f },
    val currentTime: String = "", val eventLog: List<LogEvent> = emptyList()
)

@Composable fun HudScreen(
    viewModel: HudViewModel,
    onOpenElection: () -> Unit = {},
    onOpenAiSettings: () -> Unit = {},
    onOpenApps: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val settingsVm: com.kianirani.jarvis.ui.screen.settings.SettingsHubViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val auroraOn by settingsVm.settings.aurora.collectAsState()
    val scanOn by settingsVm.settings.scanLine.collectAsState()
    var quickPanel by remember { mutableStateOf(false) }
    val wide = LocalConfiguration.current.screenWidthDp > 600
    // Background art runs edge-to-edge under the system bars; interactive
    // content is inset so it never collides with the status bar clock/icons.
    Box(Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop)) {
        if (auroraOn) PlasmaAurora(Modifier.fillMaxSize())
        HexGrid(Modifier.fillMaxSize())
        if (scanOn) ScanLine(Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            CompositionLocalProvider(
                LocalOpenElection provides onOpenElection,
                LocalOpenAiSettings provides onOpenAiSettings,
                LocalOpenApps provides onOpenApps,
                LocalOpenSettings provides onOpenSettings,
                LocalOpenQuickPanel provides { quickPanel = true },
            ) {
                if (wide) LandscapeLayout(state, viewModel) else PortraitLayout(state, viewModel)
            }
            CornerBrackets(Modifier.fillMaxSize())
            if (quickPanel) QuickPanel(settingsVm) { quickPanel = false }
        }
    }
}

/**
 * VISION QUICKPANEL (P11.5 + user directive 2026-06-12) — swipe-free quick
 * settings: FX, voice and trust level toggles without leaving the HUD.
 */
@Composable fun QuickPanel(vm: com.kianirani.jarvis.ui.screen.settings.SettingsHubViewModel, onDismiss: () -> Unit) {
    val s = vm.settings
    val aurora by s.aurora.collectAsState()
    val scan by s.scanLine.collectAsState()
    val voice by s.voiceEnabled.collectAsState()
    val tts by s.ttsEnabled.collectAsState()
    val trust by s.trustLevel.collectAsState()
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 56.dp)
                .glassPanel(radius = 16.dp)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("VISION QUICKPANEL", style = MaterialTheme.typography.labelLarge, color = JarvisColors.CyanPrimary)
                Spacer(Modifier.weight(1f))
                Text("✕", color = JarvisColors.TextDim, modifier = Modifier.clickable(onClick = onDismiss).padding(4.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickToggle("AURORA", aurora, Modifier.weight(1f)) { s.set(com.kianirani.jarvis.data.settings.VisionSettings.KEY_AURORA, it) }
                QuickToggle("SCAN", scan, Modifier.weight(1f)) { s.set(com.kianirani.jarvis.data.settings.VisionSettings.KEY_SCANLINE, it) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickToggle("VOICE", voice, Modifier.weight(1f)) { s.set(com.kianirani.jarvis.data.settings.VisionSettings.KEY_VOICE, it) }
                QuickToggle("TTS", tts, Modifier.weight(1f)) { s.set(com.kianirani.jarvis.data.settings.VisionSettings.KEY_TTS, it) }
            }
            val trustName = listOf("SOVEREIGN", "BALANCED", "OPEN")[trust]
            Row(
                Modifier.fillMaxWidth()
                    .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { s.setTrustLevel((trust + 1) % 3) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("TRUST", style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
                Spacer(Modifier.weight(1f))
                Text(trustName, style = MaterialTheme.typography.labelLarge, color = JarvisColors.CyanPrimary)
            }
        }
    }
}

@Composable private fun QuickToggle(label: String, on: Boolean, modifier: Modifier, onChange: (Boolean) -> Unit) {
    val c = if (on) JarvisColors.CyanPrimary else JarvisColors.TextDim
    Column(
        modifier
            .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .background(if (on) JarvisColors.CyanFaint else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onChange(!on) }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c)
        Text(if (on) "ON" else "OFF", style = MaterialTheme.typography.labelSmall, color = c)
    }
}

/** Tap the VISION logo in the TopBar to open the Brain Election screen. */
val LocalOpenElection = staticCompositionLocalOf<() -> Unit> { {} }
val LocalOpenAiSettings = staticCompositionLocalOf<() -> Unit> { {} }
val LocalOpenApps = staticCompositionLocalOf<() -> Unit> { {} }
val LocalOpenSettings = staticCompositionLocalOf<() -> Unit> { {} }
val LocalOpenQuickPanel = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * Portrait home (REDESIGN 2026-06-12, user directive): the Eye of Vision is
 * the single focal point — reactor-iris center stage, status constellation
 * under it, conversation panel, command bar, then a launcher dock.
 */
@Composable fun PortraitLayout(s: HudUiState, vm: HudViewModel) {
    val appsVm: com.kianirani.jarvis.ui.screen.drawer.AppDrawerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val frequent by appsVm.frequent.collectAsState()
    LaunchedEffect(Unit) { appsVm.refresh() }
    Column(Modifier.fillMaxSize()) {
        PortraitTopBar(s, Modifier.fillMaxWidth())
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReactorEye(
                listening = s.isListening,
                modifier = Modifier.fillMaxWidth().weight(1f).visionEnter(0)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = vm::toggleListening),
            )
            Text(
                if (s.isListening) "● LISTENING — speak now" else "tap the eye to talk",
                style = MaterialTheme.typography.labelMedium,
                color = if (s.isListening) JarvisColors.NeonGreen else JarvisColors.TextDim,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            StatusConstellation(s, Modifier.fillMaxWidth().visionEnter(1))
            Spacer(Modifier.height(8.dp))
            FrequentAppsRow(frequent, appsVm::launch, Modifier.fillMaxWidth().visionEnter(2))
            Spacer(Modifier.height(8.dp))
            TypewriterPanel(s.jarvisOutput, Modifier.fillMaxWidth().visionEnter(3))
            Spacer(Modifier.height(8.dp))
            InputBar(s.inputText, vm::onInputChange, vm::sendChat, Modifier.fillMaxWidth().visionEnter(4))
            Spacer(Modifier.height(10.dp))
            LauncherDock(s.isListening, vm::toggleListening, Modifier.fillMaxWidth().visionEnter(5))
            Spacer(Modifier.height(8.dp))
        }
        WaveformBar(s.waveformAmplitudes, s.isListening, Modifier.fillMaxWidth().height(40.dp))
    }
}

/**
 * Slim portrait top bar (REDESIGN 2026-06-12, user feedback: the old bar crammed
 * logo + 3 chips + buttons + clock into one row and the clock looked terrible).
 * Now: VISION mark on the left, a large breathing clock on the right. Status
 * lives in the constellation under the eye, so it isn't duplicated here.
 */
@Composable fun PortraitTopBar(s: HudUiState, modifier: Modifier) {
    val openElection = LocalOpenElection.current
    Row(
        modifier.height(56.dp).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(VisionColors.PlasmaSweep)
                    .clickable(onClick = openElection),
                Alignment.Center,
            ) { Text("V", color = VisionColors.Background, style = MaterialTheme.typography.titleMedium) }
            Text("VISION", style = MaterialTheme.typography.titleLarge, color = VisionColors.CyanPrimary)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(s.currentTime, style = MaterialTheme.typography.displaySmall, color = JarvisColors.CyanPrimary)
            Text("UTC+3:30", style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim, modifier = Modifier.padding(bottom = 6.dp))
        }
    }
}

/**
 * Frequently-used apps, right on the home screen (user feedback: needs real
 * launcher features). A horizontal strip of live app icons + an ALL tile to the
 * full drawer — so the most-used apps sit one tap from the eye.
 */
@Composable fun FrequentAppsRow(
    apps: List<com.kianirani.jarvis.ui.screen.drawer.AppEntry>,
    onLaunch: (String) -> Unit,
    modifier: Modifier,
) {
    val openApps = LocalOpenApps.current
    Row(
        modifier.glassPanel(radius = 16.dp).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        apps.take(4).forEach { app ->
            Column(
                Modifier.clip(RoundedCornerShape(12.dp)).clickable { onLaunch(app.packageName) }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(app.icon, contentDescription = app.label, Modifier.size(40.dp))
                Text(app.label.take(8), style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim, maxLines = 1)
            }
        }
        Column(
            Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = openApps).padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                    .background(JarvisColors.CyanFaint).border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                Alignment.Center,
            ) { Text("▦", style = MaterialTheme.typography.titleMedium, color = JarvisColors.CyanPrimary) }
            Text("ALL", style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
        }
    }
}

/** Compact status chips: brain, mesh nodes, cloud AI. */
@Composable fun StatusConstellation(s: HudUiState, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("BRAIN", s.brainOnline, if (s.brainOnline) "ONLINE" else "OFFLINE"),
            Triple("MESH", s.nodesOnline > 0, "${s.nodesOnline} NODE${if (s.nodesOnline == 1) "" else "S"}"),
            Triple("CLOUD", s.groqOnline, if (s.groqOnline) "READY" else "NO KEY"),
        ).forEach { (lbl, ok, detail) ->
            val c = if (ok) JarvisColors.NeonGreen else JarvisColors.DangerRed
            Column(
                Modifier.weight(1f)
                    .border(1.dp, c.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                    .background(c.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(lbl, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
                Text(detail, style = MaterialTheme.typography.labelSmall, color = c)
            }
        }
    }
}

/** Bottom dock — launcher-grade shortcuts with big touch targets. */
@Composable fun LauncherDock(listening: Boolean, onMic: () -> Unit, modifier: Modifier) {
    val apps = LocalOpenApps.current
    val settings = LocalOpenSettings.current
    val election = LocalOpenElection.current
    Row(
        modifier.glassPanel(radius = 16.dp).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockButton("▦", "APPS", JarvisColors.CyanPrimary, apps)
        DockButton("◆", "BRAIN", JarvisColors.CyanSecondary, election)
        DockButton(if (listening) "●" else "○", "MIC", if (listening) JarvisColors.NeonGreen else JarvisColors.CyanSecondary, onMic)
        DockButton("☰", "PANEL", JarvisColors.CyanSecondary, LocalOpenQuickPanel.current)
        DockButton("⚙", "CONFIG", JarvisColors.CyanSecondary, settings)
    }
}

@Composable private fun DockButton(glyph: String, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(glyph, style = MaterialTheme.typography.headlineMedium, color = tint)
        Text(label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
    }
}

@Composable fun LandscapeLayout(s: HudUiState, vm: HudViewModel) {
    Column(Modifier.fillMaxSize()) {
        TopBar(s.brainOnline, s.nodesOnline, s.groqOnline, s.isListening, s.currentTime, vm::toggleListening, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().weight(1f).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.width(200.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) { s.nodes.forEach { NodeCard(it, Modifier.fillMaxWidth()) }; MetricPanel(s, Modifier.fillMaxWidth().weight(1f)) }
            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { OrbPanel(s, Modifier.fillMaxWidth().weight(1f)); TypewriterPanel(s.jarvisOutput, Modifier.fillMaxWidth()); InputBar(s.inputText, vm::onInputChange, vm::sendChat, Modifier.fillMaxWidth()) }
            Column(Modifier.width(200.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) { RadarPanel(s, Modifier.fillMaxWidth().height(160.dp)); LogPanel(s, Modifier.fillMaxWidth().weight(1f)) }
        }
        WaveformBar(s.waveformAmplitudes, s.isListening, Modifier.fillMaxWidth().height(56.dp))
    }
}

@Composable fun TopBar(
    brainOnline: Boolean,
    nodesOnline: Int,
    groqOnline: Boolean,
    isListening: Boolean,
    currentTime: String,
    onToggleListening: () -> Unit,
    modifier: Modifier,
) {
    Row(modifier.height(52.dp).background(Brush.horizontalGradient(listOf(JarvisColors.CyanFaint, Color.Transparent, JarvisColors.CyanFaint))).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val openElection = LocalOpenElection.current
            val logoInf = rememberInfiniteTransition(label = "logo")
            val logoGlow by logoInf.animateFloat(0.4f, 1f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "lg")
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)).background(VisionColors.PlasmaSweep).border(1.dp, VisionColors.CyanPrimary.copy(alpha = logoGlow), RoundedCornerShape(6.dp)).clickable(onClick = openElection), Alignment.Center) { Text("V", color = VisionColors.Background, style = MaterialTheme.typography.headlineLarge) }
            Column { Text("VISION", style = MaterialTheme.typography.headlineLarge, color = VisionColors.CyanPrimary); Text("v${com.kianirani.jarvis.BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = VisionColors.TextDim) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("BRAIN" to brainOnline, "NODES" to (nodesOnline > 0), "GROQ" to groqOnline).forEach { (lbl, ok) ->
                val c = if (ok) JarvisColors.NeonGreen else JarvisColors.DangerRed
                Column(Modifier.border(1.dp, c.copy(alpha = 0.3f), RoundedCornerShape(3.dp)).background(c.copy(alpha = 0.05f), RoundedCornerShape(3.dp)).padding(horizontal = 8.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(lbl, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
                    Text(if (ok) "OK" else "ERR", style = MaterialTheme.typography.labelSmall, color = c)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val openAi = LocalOpenAiSettings.current
            Box(Modifier.clip(RoundedCornerShape(3.dp)).border(1.dp, JarvisColors.Border, RoundedCornerShape(3.dp)).clickable(onClick = openAi).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("AI", style = MaterialTheme.typography.labelMedium, color = JarvisColors.TextDim)
            }
            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(if (isListening) JarvisColors.CyanFaint else Color.Transparent).border(1.dp, if (isListening) JarvisColors.CyanPrimary else JarvisColors.Border, RoundedCornerShape(3.dp)).clickable(onClick = onToggleListening).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text(if (isListening) "MIC ON" else "MIC", style = MaterialTheme.typography.labelMedium, color = if (isListening) JarvisColors.CyanPrimary else JarvisColors.TextDim)
            }
            Column(horizontalAlignment = Alignment.End) { Text(currentTime, style = MaterialTheme.typography.headlineMedium, color = JarvisColors.CyanPrimary); Text("UTC+3:30", style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim) }
        }
    }
}

@Composable fun OrbPanel(s: HudUiState, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "orb")
    val ring  by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "r1")
    val ring2 by inf.animateFloat(360f, 0f, infiniteRepeatable(tween(13000, easing = LinearEasing)), label = "r2")
    val pulse by inf.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p")
    val glow  by inf.animateFloat(0.3f, 0.65f, infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    val blink by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "b")
    HudCard(modifier) {
        Canvas(Modifier.fillMaxSize().padding(28.dp)) {
            val cx = size.width / 2; val cy = size.height / 2; val maxR = minOf(cx, cy)
            // ambient plasma halo behind the orb
            drawCircle(Brush.radialGradient(listOf(VisionColors.Violet.copy(alpha = 0.18f * glow * 2f), Color.Transparent), Offset(cx, cy), maxR), maxR, Offset(cx, cy))
            repeat(4) { i -> drawCircle(VisionColors.GridLine, maxR * (0.28f + i * 0.18f), Offset(cx, cy), style = Stroke(0.8.dp.toPx())) }
            // outer cyan ring with orbiting node
            rotate(ring, Offset(cx, cy)) { drawCircle(VisionColors.CyanSecondary.copy(alpha = 0.4f), maxR * 0.88f, Offset(cx, cy), style = Stroke(1.dp.toPx())); drawCircle(VisionColors.CyanPrimary, 4.dp.toPx(), Offset(cx + maxR * 0.88f, cy)) }
            // violet counter-rotating dashed ring
            rotate(ring2, Offset(cx, cy)) {
                drawCircle(VisionColors.BorderViolet.copy(alpha = 0.7f), maxR * 0.65f, Offset(cx, cy), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)))
                repeat(3) { i -> val a = (i * 120f) * (PI / 180f).toFloat(); drawCircle(VisionColors.Magenta, 3.dp.toPx(), Offset(cx + maxR * 0.65f * cos(a), cy + maxR * 0.65f * sin(a))) }
            }
            // breathing plasma core: cyan rim → violet → deep void
            val r = maxR * 0.27f * pulse
            drawCircle(Brush.radialGradient(listOf(VisionColors.CyanPrimary, VisionColors.Violet, VisionColors.VioletDeep, VisionColors.BlueMid), Offset(cx - r * 0.25f, cy - r * 0.25f), r * 1.15f), r, Offset(cx, cy))
            // twin glow rings
            drawCircle(VisionColors.CyanPrimary.copy(alpha = glow), r + 12.dp.toPx(), Offset(cx, cy), style = Stroke(2.dp.toPx()))
            drawCircle(VisionColors.Magenta.copy(alpha = glow * 0.6f), r + 22.dp.toPx(), Offset(cx, cy), style = Stroke(1.dp.toPx()))
        }
        Column(Modifier.align(Alignment.TopCenter).padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("VISION", style = MaterialTheme.typography.displayLarge, color = JarvisColors.CyanPrimary) }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(if (s.isListening) "LISTENING" else "STANDBY", style = MaterialTheme.typography.labelMedium, color = if (s.isListening) JarvisColors.NeonGreen.copy(alpha = 0.5f + blink * 0.5f) else JarvisColors.CyanSecondary.copy(alpha = 0.7f)) }
    }
}

@Composable fun HudCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.glassPanel(radius = 8.dp)) { content() }
}

private val AuroraVioletColors = listOf(VisionColors.Violet.copy(alpha = 0.30f), Color.Transparent)
private val AuroraCyanColors = listOf(VisionColors.CyanSecondary.copy(alpha = 0.22f), Color.Transparent)
private val AuroraMagentaColors = listOf(VisionColors.Magenta.copy(alpha = 0.16f), Color.Transparent)

/** Slow drifting plasma aurora behind the whole HUD — pure graphicsLayer, no recomposition. */
@Composable fun PlasmaAurora(modifier: Modifier) {
    val inf = rememberInfiniteTransition(label = "aurora")
    val t by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), label = "t")
    Canvas(modifier.alpha(0.5f)) {
        val w = size.width; val h = size.height
        drawCircle(
            Brush.radialGradient(AuroraVioletColors, Offset(w * (0.2f + 0.2f * t), h * 0.25f), w * 0.7f),
            radius = w * 0.7f, center = Offset(w * (0.2f + 0.2f * t), h * 0.25f),
        )
        drawCircle(
            Brush.radialGradient(AuroraCyanColors, Offset(w * (0.85f - 0.2f * t), h * 0.8f), w * 0.6f),
            radius = w * 0.6f, center = Offset(w * (0.85f - 0.2f * t), h * 0.8f),
        )
        drawCircle(
            Brush.radialGradient(AuroraMagentaColors, Offset(w * (0.6f + 0.15f * t), h * (0.55f - 0.1f * t)), w * 0.5f),
            radius = w * 0.5f, center = Offset(w * (0.6f + 0.15f * t), h * (0.55f - 0.1f * t)),
        )
    }
}

@Composable fun BracketDeco() {
    Canvas(Modifier.fillMaxSize()) {
        val s = 9.dp.toPx(); val p = 4.dp.toPx(); val w = 1.5.dp.toPx(); val c = JarvisColors.CyanSecondary; val W = this.size.width; val H = this.size.height
        listOf(Triple(Offset(p,p),Offset(p+s,p),Offset(p,p+s)), Triple(Offset(W-p,p),Offset(W-p-s,p),Offset(W-p,p+s)), Triple(Offset(p,H-p),Offset(p+s,H-p),Offset(p,H-p-s)), Triple(Offset(W-p,H-p),Offset(W-p-s,H-p),Offset(W-p,H-p-s))).forEach { (o,h,v) -> drawLine(c,o,h,w); drawLine(c,o,v,w) }
    }
}

@Composable fun TypewriterPanel(text: String, modifier: Modifier) {
    val cur by rememberInfiniteTransition(label="cur").animateFloat(0f,1f,infiniteRepeatable(tween(800),RepeatMode.Reverse),label="c")
    HudCard(modifier) { Column(Modifier.padding(12.dp)) { Text("VISION OUTPUT", style=MaterialTheme.typography.labelSmall, color=JarvisColors.TextDim, modifier=Modifier.padding(bottom=6.dp)); Row { Text(">> ", style=MaterialTheme.typography.bodyMedium, color=JarvisColors.CyanPrimary); Text(text, style=MaterialTheme.typography.bodyMedium, color=JarvisColors.TextPrimary, maxLines=3, overflow=TextOverflow.Ellipsis); Text("|", style=MaterialTheme.typography.bodyMedium, color=JarvisColors.CyanPrimary.copy(alpha=cur)) } } }
}

@Composable fun NodeCard(n: NodeInfo, modifier: Modifier) {
    val inf = rememberInfiniteTransition(label="dot")
    val sc by inf.animateFloat(1f,2.5f,infiniteRepeatable(tween(1500,easing=FastOutSlowInEasing),RepeatMode.Restart),label="s")
    val col = if(n.online) JarvisColors.NeonGreen else JarvisColors.DangerRed
    HudCard(modifier) { Column(Modifier.padding(10.dp)) {
        Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically) {
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) { Box(Modifier.size(12.dp),Alignment.Center){Box(Modifier.size(10.dp).scale(sc).clip(CircleShape).background(col.copy(alpha=0.25f)));Box(Modifier.size(6.dp).clip(CircleShape).background(col))}; Text(n.name,style=MaterialTheme.typography.labelLarge,color=JarvisColors.TextPrimary) }
            Text(if(n.online)"ON" else "OFF",style=MaterialTheme.typography.labelSmall,color=col)
        }
        Spacer(Modifier.height(3.dp)); Text(n.role,style=MaterialTheme.typography.bodySmall,color=JarvisColors.TextDim); Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){listOf("CPU" to "${n.cpu}%","RAM" to "${n.ram}%").forEach{(l,v)->Column{Text(l,style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim);Text(v,style=MaterialTheme.typography.titleLarge,color=JarvisColors.CyanPrimary)}}}
    } }
}

@Composable fun MetricPanel(s: HudUiState, modifier: Modifier) {
    HudCard(modifier){Column(Modifier.padding(10.dp)){Text("BRAIN METRICS",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim,modifier=Modifier.padding(bottom=8.dp));MetricBar("CPU",s.brainCpu,JarvisColors.CyanPrimary);MetricBar("RAM",s.brainRam,JarvisColors.BlueDeep);MetricBar("NET",s.brainNet,JarvisColors.NeonGreen)}}
}

@Composable fun MetricBar(label: String, value: Float, color: Color) {
    val v by animateFloatAsState(value,tween(400),label="bar")
    Column(Modifier.padding(bottom=7.dp)){Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween){Text(label,style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim);Text("${v.toInt()}%",style=MaterialTheme.typography.labelSmall,color=color)};Spacer(Modifier.height(3.dp));Box(Modifier.fillMaxWidth().height(2.dp).background(JarvisColors.GridLine,RoundedCornerShape(1.dp))){Box(Modifier.fillMaxWidth((v/100f).coerceIn(0f,1f)).fillMaxHeight().background(Brush.horizontalGradient(listOf(color.copy(alpha=0.5f),color)),RoundedCornerShape(1.dp)))}}
}

@Composable fun RadarPanel(s: HudUiState, modifier: Modifier) {
    HudCard(modifier){Column(Modifier.padding(10.dp)){Text("RADAR",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim,modifier=Modifier.padding(bottom=4.dp))
        Canvas(Modifier.fillMaxSize()){val cx=size.width/2;val cy=size.height/2;val r=minOf(cx,cy)*0.72f;val vals=listOf(s.brainCpu/100f,s.brainRam/100f,s.brainNet/100f,0.45f,0.62f);val n=vals.size;val step=(2*PI/n).toFloat()
            repeat(4){i->val gr=r*((i+1)/4f);val path=Path();repeat(n){j->val a=step*j-(PI/2).toFloat();if(j==0)path.moveTo(cx+gr*cos(a),cy+gr*sin(a)) else path.lineTo(cx+gr*cos(a),cy+gr*sin(a))};path.close();drawPath(path,JarvisColors.GridLine,style=Stroke(0.8.dp.toPx()))}
            val dp=Path();vals.forEachIndexed{j,v->val a=step*j-(PI/2).toFloat();if(j==0)dp.moveTo(cx+r*v*cos(a),cy+r*v*sin(a)) else dp.lineTo(cx+r*v*cos(a),cy+r*v*sin(a))};dp.close();drawPath(dp,JarvisColors.CyanFaint);drawPath(dp,JarvisColors.CyanPrimary,style=Stroke(1.5.dp.toPx()))
            vals.forEachIndexed{j,v->val a=step*j-(PI/2).toFloat();drawCircle(JarvisColors.CyanPrimary,3.dp.toPx(),Offset(cx+r*v*cos(a),cy+r*v*sin(a)))}}}}
}

@Composable fun LogPanel(s: HudUiState, modifier: Modifier) {
    HudCard(modifier){Column(Modifier.padding(10.dp)){Text("EVENT LOG",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim,modifier=Modifier.padding(bottom=6.dp))
        s.eventLog.takeLast(8).forEach{e->Row(Modifier.padding(bottom=3.dp),horizontalArrangement=Arrangement.spacedBy(5.dp)){Text(e.time,style=MaterialTheme.typography.bodySmall,color=JarvisColors.TextDim);Text(e.message,style=MaterialTheme.typography.bodySmall,maxLines=1,overflow=TextOverflow.Ellipsis,color=when(e.level){EventLevel.OK->JarvisColors.NeonGreen;EventLevel.WARN->JarvisColors.WarningAmber;EventLevel.ERR->JarvisColors.DangerRed;else->JarvisColors.CyanSecondary})}}}}
}

@Composable fun InputBar(value: String, onChange: (String)->Unit, onSend: ()->Unit, modifier: Modifier) {
    val active = value.isNotBlank()
    Row(modifier.border(1.dp,JarvisColors.Border,RoundedCornerShape(4.dp)).background(JarvisColors.CyanFaint,RoundedCornerShape(4.dp)).padding(horizontal=12.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        Text(">>",color=JarvisColors.CyanPrimary,style=MaterialTheme.typography.bodyLarge)
        BasicTextField(value,onChange,Modifier.weight(1f),textStyle=MaterialTheme.typography.bodyMedium.copy(color=JarvisColors.TextPrimary),cursorBrush=SolidColor(JarvisColors.CyanPrimary),singleLine=true,keyboardOptions=KeyboardOptions(imeAction=ImeAction.Send),keyboardActions=KeyboardActions(onSend={if(active)onSend()}),decorationBox={inner->Box{if(value.isEmpty())Text("Send command...",style=MaterialTheme.typography.bodyMedium,color=JarvisColors.TextDim);inner()}})
        Box(Modifier.clip(RoundedCornerShape(4.dp)).background(if(active)JarvisColors.CyanPrimary.copy(alpha=0.18f) else Color.Transparent).border(1.dp,if(active)JarvisColors.CyanPrimary else JarvisColors.Border,RoundedCornerShape(4.dp)).clickable(enabled=active,onClick=onSend).padding(horizontal=12.dp,vertical=6.dp)){
            Text("SEND",style=MaterialTheme.typography.labelMedium,color=if(active)JarvisColors.CyanPrimary else JarvisColors.TextDim)
        }
    }
}

@Composable fun WaveformBar(amps: List<Float>, active: Boolean, modifier: Modifier) {
    Row(modifier.background(Brush.horizontalGradient(listOf(JarvisColors.CyanFaint,Color.Transparent,JarvisColors.CyanFaint))).padding(horizontal=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
        Text("VOICE",style=MaterialTheme.typography.labelSmall,color=JarvisColors.TextDim)
        Canvas(Modifier.weight(1f).fillMaxHeight().padding(vertical=6.dp)){val bw=(size.width-(amps.size-1)*3.dp.toPx())/amps.size;val mh=size.height;amps.forEachIndexed{i,a->val bh=mh*a.coerceIn(0.02f,1f);val x=i*(bw+3.dp.toPx());val y=(mh-bh)/2f;drawRect(Brush.verticalGradient(if(active)listOf(JarvisColors.CyanPrimary,JarvisColors.BlueDeep)else listOf(JarvisColors.CyanSecondary.copy(alpha=0.3f),JarvisColors.BlueDeep.copy(alpha=0.2f)),startY=y,endY=y+bh),Offset(x,y),Size(bw,bh))}}
        Text(if(active)"ON" else "48k",style=MaterialTheme.typography.labelSmall,color=if(active)JarvisColors.NeonGreen else JarvisColors.TextDim)
    }
}

@Composable fun HexGrid(modifier: Modifier) {
    Canvas(modifier){val hw=52.dp.toPx();val hh=60.dp.toPx();val paint=Paint().apply{color=JarvisColors.GridLine;style=PaintingStyle.Stroke;strokeWidth=0.8.dp.toPx()}
        repeat((size.height/hh).toInt()+2){row->repeat((size.width/hw).toInt()+2){col->val cx=col*hw+(if(row%2==1)hw/2 else 0f);val cy=row*hh*0.75f;val path=Path();repeat(6){i->val a=(60*i-30)*(PI/180f).toFloat();if(i==0)path.moveTo(cx+hw/2*cos(a),cy+hw/2*sin(a))else path.lineTo(cx+hw/2*cos(a),cy+hw/2*sin(a))};path.close();drawContext.canvas.drawPath(path,paint)}}}
}

@Composable fun ScanLine(modifier: Modifier) {
    val inf=rememberInfiniteTransition(label="scan");val y by inf.animateFloat(-5f,105f,infiniteRepeatable(tween(4000,easing=LinearEasing)),label="sy")
    Canvas(modifier.alpha(0.3f)){drawRect(Brush.horizontalGradient(listOf(Color.Transparent,JarvisColors.CyanPrimary,Color.Transparent)),Offset(0f,size.height*y/100f),Size(size.width,3.dp.toPx()))}
}

@Composable fun CornerBrackets(modifier: Modifier) {
    Canvas(modifier){val s=44.dp.toPx();val w=2.dp.toPx();val c=JarvisColors.CyanSecondary.copy(alpha=0.45f);val W=size.width;val H=size.height
        listOf(Triple(Offset(0f,0f),Offset(s,0f),Offset(0f,s)),Triple(Offset(W,0f),Offset(W-s,0f),Offset(W,s)),Triple(Offset(0f,H),Offset(s,H),Offset(0f,H-s)),Triple(Offset(W,H),Offset(W-s,H),Offset(W,H-s))).forEach{(o,h,v)->drawLine(c,o,h,w);drawLine(c,o,v,w)}}
}
