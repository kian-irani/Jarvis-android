package com.kianirani.jarvis.ui.screen.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.kianirani.jarvis.data.ai.AiProvider
import com.kianirani.jarvis.data.ai.AiUsageStore
import com.kianirani.jarvis.data.ai.ChatHistoryStore
import com.kianirani.jarvis.data.settings.ActivationStore
import com.kianirani.jarvis.data.settings.VisionSettings
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsHubViewModel @Inject constructor(
    val settings: VisionSettings,
    val activation: ActivationStore,
    val usage: AiUsageStore,
    val history: ChatHistoryStore,
) : ViewModel()

/**
 * SYSTEM CONFIG — the organized settings hub (USER DIRECTIVE 2026-06-12).
 * Sections: AI Providers, Brain & Mesh, Voice, Interface FX, Launcher, About.
 */
@Composable
fun SettingsHubScreen(
    vm: SettingsHubViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onOpenAiTokens: () -> Unit = {},
    onOpenElection: () -> Unit = {},
) {
    val s = vm.settings
    val voice by s.voiceEnabled.collectAsState()
    val tts by s.ttsEnabled.collectAsState()
    val scan by s.scanLine.collectAsState()
    val aurora by s.aurora.collectAsState()
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ BACK", style = MaterialTheme.typography.labelLarge, color = JarvisColors.TextDim,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
            Text("SYSTEM CONFIG", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.CyanPrimary)
        }

        Section("INTELLIGENCE", 0) {
            NavRow("AI Providers", "API tokens, multi-key rotation", onOpenAiTokens)
            NavRow("Brain & Mesh", "election, pairing, mesh nodes", onOpenElection)
        }
        Section("VOICE", 1) {
            ToggleRow("Voice input", "wake Vision with the mic", voice) { s.set(VisionSettings.KEY_VOICE, it) }
            ToggleRow("Spoken replies (TTS)", "Vision reads answers aloud", tts) { s.set(VisionSettings.KEY_TTS, it) }
            val rate by s.speechRate.collectAsState()
            val pitch by s.voicePitch.collectAsState()
            StepperRow("Speech rate", rate, 0.1f) { s.setSpeechRate(it) }
            StepperRow("Voice pitch", pitch, 0.1f) { s.setVoicePitch(it) }
        }
        Section("PERSONA", 2) {
            val personaName by s.personaName.collectAsState()
            val humor by s.humorLevel.collectAsState()
            val formality by s.formalityLevel.collectAsState()
            val respLen by s.responseLength.collectAsState()
            PersonaNameRow(personaName) { s.setPersonaName(it) }
            SliderRow("Humor", humor) { s.setHumorLevel(it) }
            SliderRow("Formality", formality) { s.setFormalityLevel(it) }
            SliderRow("Response length", respLen) { s.setResponseLength(it) }
        }
        Section("INTERFACE FX", 3) {
            ToggleRow("Plasma aurora", "drifting background nebula", aurora) { s.set(VisionSettings.KEY_AURORA, it) }
            ToggleRow("Scan line", "moving HUD scan beam", scan) { s.set(VisionSettings.KEY_SCANLINE, it) }
        }
        Section("TRUST LEVEL", 4) {
            val trust by s.trustLevel.collectAsState()
            TrustSelector(trust, s::setTrustLevel)
        }
        Section("ACTIVATION", 5) {
            ActivationRow(vm.activation)
        }
        Section("PRIVACY MONITOR", 6) {
            val used = AiProvider.entries.map { it to vm.usage.usage(it) }.filter { it.second.calls > 0 }
            if (used.isEmpty()) {
                InfoRow("Cloud calls", "none — nothing has left this device")
            } else {
                used.forEach { (p, u) -> InfoRow(p.displayName, "${u.calls} calls · ${u.ok} ok · ${u.failed} failed") }
            }
            NavRow("Clear conversation memory", "forget all chat history") { vm.history.clear() }
        }
        Section("LAUNCHER", 7) {
            NavRow("Set as default home", "open Android home settings") {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        Section("ABOUT", 8) {
            InfoRow("Version", "VISION v${com.kianirani.jarvis.BuildConfig.VERSION_NAME} — Sovereign Intelligence")
            InfoRow("Source", "github.com/kian-irani/Jarvis-android")
        }
    }
}

@Composable
private fun Section(title: String, index: Int, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().visionEnter(index)
            .glassPanel(radius = 10.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanSecondary)
        content()
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
        }
        Text("›", style = MaterialTheme.typography.headlineMedium, color = JarvisColors.CyanPrimary)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextPrimary)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = JarvisColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
        }
        // HUD-styled switch track
        val track = if (checked) VisionColors.CyanPrimary.copy(alpha = 0.35f) else JarvisColors.GridLine
        val knob = if (checked) VisionColors.CyanPrimary else JarvisColors.TextDim
        Box(
            Modifier.width(42.dp).height(22.dp)
                .background(track, RoundedCornerShape(11.dp))
                .border(1.dp, knob.copy(alpha = 0.6f), RoundedCornerShape(11.dp))
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(16.dp).background(knob, CircleShape))
        }
    }
}

@Composable
private fun TrustSelector(current: Int, onSelect: (Int) -> Unit) {
    val levels = listOf(
        Triple(0, "SOVEREIGN", "local brain only — nothing leaves the device"),
        Triple(1, "BALANCED", "brain first, cloud fallback (default)"),
        Triple(2, "OPEN", "fastest answer wins, any provider"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        levels.forEach { (lvl, name, desc) ->
            val active = lvl == current
            Row(
                Modifier.fillMaxWidth()
                    .border(
                        1.dp,
                        if (active) VisionColors.CyanPrimary else JarvisColors.Border,
                        RoundedCornerShape(6.dp),
                    )
                    .background(
                        if (active) VisionColors.CyanPrimary.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(6.dp),
                    )
                    .clickable { onSelect(lvl) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.labelLarge,
                        color = if (active) VisionColors.CyanPrimary else JarvisColors.TextPrimary)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
                }
                if (active) Text("◉", color = VisionColors.CyanPrimary)
            }
        }
    }
}

@Composable
private fun ActivationRow(store: ActivationStore) {
    val code by store.code.collectAsState()
    var input by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    if (code != null) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("◉ ACTIVATED", style = MaterialTheme.typography.labelLarge, color = JarvisColors.NeonGreen)
            Spacer(Modifier.weight(1f))
            Text("…${code!!.takeLast(4)}", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            Text("  REMOVE", style = MaterialTheme.typography.labelSmall, color = JarvisColors.DangerRed,
                modifier = Modifier.clickable { store.deactivate() }.padding(4.dp))
        }
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.foundation.text.BasicTextField(
                value = input, onValueChange = { input = it }, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = JarvisColors.TextPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(JarvisColors.CyanPrimary),
                decorationBox = { inner ->
                    Row(Modifier.border(1.dp, JarvisColors.Border, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                        if (input.isEmpty()) Text("activation code from @bot", color = JarvisColors.TextDim, style = MaterialTheme.typography.bodySmall)
                        inner()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Text("ACTIVATE", style = MaterialTheme.typography.labelLarge, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable(enabled = input.isNotBlank()) { store.activate(input); input = "" }.padding(6.dp))
        }
    }
}

@Composable
private fun PersonaNameRow(name: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("AI name", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary, modifier = Modifier.weight(1f))
        BasicTextField(
            value = name,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = JarvisColors.CyanPrimary, textAlign = TextAlign.End),
            cursorBrush = SolidColor(JarvisColors.CyanPrimary),
            modifier = Modifier.width(140.dp),
            decorationBox = { inner ->
                Box {
                    if (name.isEmpty()) Text("VISION", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    inner()
                }
            },
        )
    }
}

@Composable
private fun SliderRow(title: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary, modifier = Modifier.weight(1f))
            Text("${value.toInt()}/10", style = MaterialTheme.typography.bodySmall, color = JarvisColors.CyanPrimary)
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = 0f..10f, steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = JarvisColors.CyanPrimary,
                activeTrackColor = JarvisColors.CyanPrimary,
                inactiveTrackColor = JarvisColors.GridLine,
            ),
        )
    }
}

@Composable
private fun StepperRow(title: String, value: Float, step: Float, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary, modifier = Modifier.weight(1f))
        Text("−", style = MaterialTheme.typography.headlineMedium, color = JarvisColors.CyanPrimary,
            modifier = Modifier.clickable { onChange(value - step) }.padding(horizontal = 12.dp))
        Text("%.1f".format(value), style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary)
        Text("+", style = MaterialTheme.typography.headlineMedium, color = JarvisColors.CyanPrimary,
            modifier = Modifier.clickable { onChange(value + step) }.padding(horizontal = 12.dp))
    }
}
