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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.kianirani.jarvis.data.settings.VisionSettings
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsHubViewModel @Inject constructor(val settings: VisionSettings) : ViewModel()

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
        }
        Section("INTERFACE FX", 2) {
            ToggleRow("Plasma aurora", "drifting background nebula", aurora) { s.set(VisionSettings.KEY_AURORA, it) }
            ToggleRow("Scan line", "moving HUD scan beam", scan) { s.set(VisionSettings.KEY_SCANLINE, it) }
        }
        Section("LAUNCHER", 3) {
            NavRow("Set as default home", "open Android home settings") {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        Section("ABOUT", 4) {
            InfoRow("Version", "VISION v16.0.0 — Sovereign Intelligence")
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
