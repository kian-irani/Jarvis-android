package com.kianirani.jarvis.ui.screen.onboarding

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
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
class OnboardingViewModel @Inject constructor(val settings: VisionSettings) : ViewModel()

/**
 * First-run setup (USER DIRECTIVE 2026-06-12): the very first thing the user
 * configures is the assistant itself — its name, language, voice, and trust —
 * before reaching the HUD. Everything here is also editable later in SYSTEM CONFIG.
 */
@Composable
fun OnboardingScreen(
    vm: OnboardingViewModel = hiltViewModel(),
    onFinished: () -> Unit = {},
) {
    val s = vm.settings
    val name by s.personaName.collectAsState()
    val lang by s.language.collectAsState()
    val voice by s.voiceEnabled.collectAsState()
    val trust by s.trustLevel.collectAsState()

    Column(
        Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop).systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text("VISION", style = MaterialTheme.typography.displayLarge, color = JarvisColors.CyanPrimary,
            modifier = Modifier.visionEnter(0))
        Text("Sovereign personal intelligence — let's set it up.",
            style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim,
            modifier = Modifier.visionEnter(1))

        Card("ASSISTANT NAME", 2) {
            Text("What should it answer to? You can call it by this name.",
                style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            Spacer(Modifier.height(8.dp))
            BasicTextField(
                value = name,
                onValueChange = s::setPersonaName,
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = JarvisColors.CyanPrimary),
                cursorBrush = SolidColor(JarvisColors.CyanPrimary),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box { if (name.isEmpty()) Text("VISION", style = MaterialTheme.typography.headlineMedium, color = JarvisColors.TextDim); inner() }
                },
            )
        }

        Card("LANGUAGE", 3) {
            Text("Vision is multilingual — Persian and English.",
                style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Auto", lang == VisionSettings.LANG_AUTO, Modifier.weight(1f)) { s.setLanguage(VisionSettings.LANG_AUTO) }
                Pill("فارسی", lang == VisionSettings.LANG_FA, Modifier.weight(1f)) { s.setLanguage(VisionSettings.LANG_FA) }
                Pill("English", lang == VisionSettings.LANG_EN, Modifier.weight(1f)) { s.setLanguage(VisionSettings.LANG_EN) }
            }
        }

        Card("VOICE", 4) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Talk to Vision and hear replies", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary)
                Pill(if (voice) "ON" else "OFF", voice, Modifier) { s.set(VisionSettings.KEY_VOICE, !voice) }
            }
        }

        Card("PRIVACY", 5) {
            Text("Trust level — how much may leave this device.",
                style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill("Sovereign", trust == 0, Modifier.weight(1f)) { s.setTrustLevel(0) }
                Pill("Balanced", trust == 1, Modifier.weight(1f)) { s.setTrustLevel(1) }
                Pill("Open", trust == 2, Modifier.weight(1f)) { s.setTrustLevel(2) }
            }
        }

        Spacer(Modifier.weight(1f))
        Box(
            Modifier.fillMaxWidth().visionEnter(6)
                .border(1.dp, JarvisColors.CyanPrimary, RoundedCornerShape(8.dp))
                .background(JarvisColors.CyanPrimary.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                .clickable(onClick = onFinished).padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("ENTER VISION ›", style = MaterialTheme.typography.titleMedium, color = JarvisColors.CyanPrimary)
        }
    }
}

@Composable
private fun Card(title: String, index: Int, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().visionEnter(index).glassPanel(radius = 10.dp).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = JarvisColors.CyanSecondary)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .border(1.dp, if (selected) JarvisColors.CyanPrimary else JarvisColors.Border, RoundedCornerShape(6.dp))
            .background(if (selected) JarvisColors.CyanPrimary.copy(alpha = 0.18f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = if (selected) JarvisColors.CyanPrimary else JarvisColors.TextDim)
    }
}
