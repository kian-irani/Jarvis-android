package com.kianirani.jarvis.ui.screen.settings

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.kianirani.jarvis.data.ai.AiProvider
import com.kianirani.jarvis.data.ai.AiUsageStore
import com.kianirani.jarvis.data.ai.ChatHistoryStore
import com.kianirani.jarvis.data.settings.ActivationStore
import com.kianirani.jarvis.data.settings.VisionSettings
import com.kianirani.jarvis.ui.theme.FontCatalog
import com.kianirani.jarvis.ui.theme.FontStore
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.ThemeStore
import com.kianirani.jarvis.ui.theme.VisionIcons
import com.kianirani.jarvis.ui.theme.VisionFontFamilies
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.VisionThemes
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
    private val voice: com.kianirani.jarvis.voice.VoiceController,
    private val launcher: com.kianirani.jarvis.data.launcher.LauncherStore,
) : ViewModel() {
    fun voicesFor(language: String) = voice.voicesFor(language)
    fun testVoice(language: String) = voice.speakSample(language)
    fun exportLayout(): String = launcher.exportJson()
    fun importLayout(text: String): Boolean = launcher.importJson(text)
    fun resetLayout() = launcher.reset()
    val launcherLayout = launcher.layout
    fun setGrid(cols: Int, rows: Int) = launcher.setGridReflow(cols, rows)
}

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
    val voice by s.voiceEnabled.collectAsStateWithLifecycle()
    val tts by s.ttsEnabled.collectAsStateWithLifecycle()
    val scan by s.scanLine.collectAsStateWithLifecycle()
    val aurora by s.aurora.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(JarvisColors.CyanFaint, CircleShape)
                    .border(1.dp, JarvisColors.CyanSecondary.copy(alpha = 0.5f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Icon(VisionIcons.Back, "Back", tint = JarvisColors.CyanPrimary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Settings", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.TextPrimary)
                Text("Appearance · Intelligence · Privacy", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
            }
        }

        Section("APPEARANCE", 0) {
            AppearanceControls()
        }
        Section("INTELLIGENCE", 1) {
            NavRow("AI Providers", "API tokens, multi-key rotation", onOpenAiTokens)
            NavRow("Brain & Mesh", "election, pairing, mesh nodes", onOpenElection)
        }
        Section("VOICE", 1) {
            ToggleRow("Voice input", "wake Vision with the mic", voice) { s.set(VisionSettings.KEY_VOICE, it) }
            ToggleRow("Spoken replies (TTS)", "Vision reads answers aloud", tts) { s.set(VisionSettings.KEY_TTS, it) }
            val neural by s.neuralVoice.collectAsStateWithLifecycle()
            ToggleRow("Neural voice (online)", "fluent free Persian via Edge neural — falls back offline", neural) { s.set(VisionSettings.KEY_NEURAL_VOICE, it) }
            val rate by s.speechRate.collectAsStateWithLifecycle()
            val pitch by s.voicePitch.collectAsStateWithLifecycle()
            StepperRow("Speech rate", rate, 0.1f) { s.setSpeechRate(it) }
            StepperRow("Voice pitch", pitch, 0.1f) { s.setVoicePitch(it) }
            // FV2 — pick the actual Persian / English voice + test it. Code-switch
            // replies speak each language with its own selected voice.
            val voiceFa by s.voiceNameFa.collectAsStateWithLifecycle()
            val voiceEn by s.voiceNameEn.collectAsStateWithLifecycle()
            VoicePickerRow("Persian voice", VisionSettings.LANG_FA, voiceFa, vm)
            VoicePickerRow("English voice", VisionSettings.LANG_EN, voiceEn, vm)
        }
        Section("PERSONA", 2) {
            val personaName by s.personaName.collectAsStateWithLifecycle()
            val userName by s.userName.collectAsStateWithLifecycle()
            val humor by s.humorLevel.collectAsStateWithLifecycle()
            val formality by s.formalityLevel.collectAsStateWithLifecycle()
            val respLen by s.responseLength.collectAsStateWithLifecycle()
            val lang by s.language.collectAsStateWithLifecycle()
            PersonaNameRow(userName, label = "Your name", placeholder = "Your name") { s.setUserName(it) }
            PersonaNameRow(personaName) { s.setPersonaName(it) }
            LanguageRow(lang) {
                s.setLanguage(it)
                // Keep the "Auto" typeface in sync: Persian UI → Vazirmatn.
                FontStore.setPersian(
                    it == VisionSettings.LANG_FA ||
                        (it == VisionSettings.LANG_AUTO && java.util.Locale.getDefault().language == "fa"),
                )
            }
            SliderRow("Humor", humor) { s.setHumorLevel(it) }
            SliderRow("Formality", formality) { s.setFormalityLevel(it) }
            SliderRow("Response length", respLen) { s.setResponseLength(it) }
        }
        Section("INTERFACE FX", 3) {
            ToggleRow("Plasma aurora", "drifting background nebula", aurora) { s.set(VisionSettings.KEY_AURORA, it) }
            ToggleRow("Scan line", "moving HUD scan beam", scan) { s.set(VisionSettings.KEY_SCANLINE, it) }
        }
        Section("TRUST LEVEL", 4) {
            val trust by s.trustLevel.collectAsStateWithLifecycle()
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
            val layout by vm.launcherLayout.collectAsStateWithLifecycle()
            GridSizeRow(layout.gridCols, layout.gridRows) { c, r -> vm.setGrid(c, r) }
            NavRow("Set as default home", "open Android home settings") {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            NavRow("Device Control", "enable Home/Back/Recents by voice (Accessibility)") {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            NavRow("Notification Access", "let Vision read your notifications when asked") {
                runCatching { ctx.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            // LR11 — back up / restore the home layout via the clipboard.
            val clipboard = ctx.getSystemService(android.content.ClipboardManager::class.java)
            NavRow("Back up home layout", "copy your layout to the clipboard") {
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Vision layout", vm.exportLayout()))
                android.widget.Toast.makeText(ctx, "Home layout copied", android.widget.Toast.LENGTH_SHORT).show()
            }
            NavRow("Restore home layout", "import a layout from the clipboard") {
                val text = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                val ok = text.isNotBlank() && vm.importLayout(text)
                android.widget.Toast.makeText(ctx, if (ok) "Home layout restored" else "No valid layout on the clipboard", android.widget.Toast.LENGTH_SHORT).show()
            }
            NavRow("Reset home layout", "remove all pinned apps (re-seeds on restart)") {
                vm.resetLayout()
                android.widget.Toast.makeText(ctx, "Home layout cleared", android.widget.Toast.LENGTH_SHORT).show()
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
            .glassPanel(radius = 18.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = JarvisColors.CyanSecondary)
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
    val code by store.code.collectAsStateWithLifecycle()
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
private fun PersonaNameRow(name: String, label: String = "AI name", placeholder: String = "VISION", onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary, modifier = Modifier.weight(1f))
        BasicTextField(
            value = name,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = JarvisColors.CyanPrimary, textAlign = TextAlign.End),
            cursorBrush = SolidColor(JarvisColors.CyanPrimary),
            modifier = Modifier.width(140.dp),
            decorationBox = { inner ->
                Box {
                    if (name.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextDim, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    inner()
                }
            },
        )
    }
}

/** Home grid density presets (re-flows icons safely). */
@Composable
private fun GridSizeRow(cols: Int, rows: Int, onChange: (Int, Int) -> Unit) {
    val presets = listOf(4 to 5, 5 to 5, 5 to 6, 6 to 6)
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text("Home grid", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { (c, r) ->
                val sel = c == cols && r == rows
                Text(
                    "$c×$r",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (sel) JarvisColors.CyanPrimary else JarvisColors.TextDim,
                    modifier = Modifier.clickable { onChange(c, r) }
                        .border(1.dp, if (sel) JarvisColors.CyanPrimary else JarvisColors.Border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun LanguageRow(lang: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Language", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary, modifier = Modifier.weight(1f))
        listOf(VisionSettings.LANG_AUTO to "Auto", VisionSettings.LANG_FA to "فا", VisionSettings.LANG_EN to "EN").forEach { (code, label) ->
            val sel = lang == code
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = if (sel) JarvisColors.CyanPrimary else JarvisColors.TextDim,
                modifier = Modifier.clickable { onChange(code) }
                    .border(1.dp, if (sel) JarvisColors.CyanPrimary else JarvisColors.Border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/**
 * FV2 voice picker — a labelled row with an "Auto" chip plus one chip per
 * installed voice for [language], and a TEST button that speaks a sample with the
 * current selection. Selecting a chip pins that voice; "Auto" clears the pin so
 * the controller uses the best installed voice.
 */
@Composable
private fun VoicePickerRow(title: String, language: String, selected: String, vm: SettingsHubViewModel) {
    val voices = remember(language) { vm.voicesFor(language) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                "TEST", style = MaterialTheme.typography.labelMedium, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable { vm.testVoice(language) }
                    .border(1.dp, JarvisColors.CyanPrimary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        if (voices.isEmpty()) {
            Text(
                "No installed voices — add one in the system TTS settings",
                style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VoiceChip("Auto", selected.isBlank()) { vm.settings.setVoiceName(language, "") }
                voices.forEach { v ->
                    VoiceChip(v.displayName, selected == v.id) { vm.settings.setVoiceName(language, v.id) }
                }
            }
        }
    }
}

@Composable
private fun VoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) JarvisColors.CyanPrimary else JarvisColors.TextDim,
        modifier = Modifier.clickable(onClick = onClick)
            .border(1.dp, if (selected) JarvisColors.CyanPrimary else JarvisColors.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
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

// ===========================================================================
// APPEARANCE (USER DIRECTIVE 2026-06-14: full theme switcher, accent picker,
// wallpaper, animation toggle, brain badge, reset). Everything applies live —
// the controls read [ThemeStore] snapshot state and re-render on every change.
// ===========================================================================

@Composable
private fun AppearanceControls() {
    Text("Theme", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary,
        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
    ThemeSelector()

    Text("Accent", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
    AccentPicker()

    Text("Wallpaper", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
    WallpaperPicker()

    Text("Typeface", style = MaterialTheme.typography.bodyMedium, color = JarvisColors.TextPrimary,
        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
    FontPicker()

    Spacer(Modifier.height(6.dp))
    ToggleRow("Animations", "motion, pulse & entrance effects", ThemeStore.animations) { ThemeStore.enableAnimations(it) }
    ToggleRow("Brain badge", "show the active brain node on home", ThemeStore.showBrainBadge) { ThemeStore.setBrainBadge(it) }
    NavRow("Reset appearance", "theme, accent, wallpaper & font to defaults") {
        ThemeStore.reset(); FontStore.reset()
    }
}

/**
 * FNT3 — typeface picker. Each chip previews its own face ("Aa") in that font so
 * the choice reads visually; "Auto" follows the UI language (Vazirmatn for
 * Persian, else Space Grotesk). Selecting one re-themes the whole UI live.
 */
@Composable
private fun FontPicker() {
    val current = FontStore.fontId
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FontCatalog.ids.forEach { id ->
            val sel = id == current
            Column(
                Modifier
                    .border(
                        if (sel) 2.dp else 1.dp,
                        if (sel) JarvisColors.CyanPrimary else JarvisColors.Border,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { FontStore.setFont(id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Aa",
                    fontFamily = fontPreviewFamily(id),
                    fontSize = 20.sp,
                    color = if (sel) JarvisColors.CyanPrimary else JarvisColors.TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    FontCatalog.name(id),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sel) JarvisColors.CyanPrimary else JarvisColors.TextDim,
                )
            }
        }
    }
}

/** Preview face for a picker id ("Auto" previews its default, Space Grotesk). */
private fun fontPreviewFamily(id: Int): FontFamily = when (id) {
    FontCatalog.INTER -> VisionFontFamilies.Inter
    FontCatalog.DM_SANS -> VisionFontFamilies.DmSans
    FontCatalog.EXO_2 -> VisionFontFamilies.Exo2
    FontCatalog.VAZIRMATN -> VisionFontFamilies.Vazirmatn
    else -> VisionFontFamilies.SpaceGrotesk
}

@Composable
private fun ThemeSelector() {
    val current = ThemeStore.themeId
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VisionThemes.names.forEachIndexed { id, name ->
            val p = VisionThemes.palette(id)
            val sel = id == current
            Column(
                Modifier.weight(1f)
                    .border(
                        if (sel) 2.dp else 1.dp,
                        if (sel) JarvisColors.CyanPrimary else JarvisColors.Border,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { ThemeStore.setTheme(id) }
                    .padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(36.dp)
                        .background(p.background, RoundedCornerShape(6.dp))
                        .border(1.dp, p.defaultAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) { Box(Modifier.size(16.dp).background(p.defaultAccent, CircleShape)) }
                Spacer(Modifier.height(5.dp))
                Text(
                    name.substringBefore(' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sel) JarvisColors.CyanPrimary else JarvisColors.TextDim,
                )
                if (sel) Text("◉ ACTIVE", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanPrimary)
            }
        }
    }
}

@Composable
private fun AccentPicker() {
    val presets = listOf(
        Color(0xFF22F5FF), Color(0xFF7C4DFF), Color(0xFFB04DFF), Color(0xFF0E8FA8),
        Color(0xFF3DFFB0), Color(0xFFFFC233), Color(0xFF1565FF), Color(0xFFFF3B6B),
    )
    val current = ThemeStore.accent
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ColorSwatch(VisionThemes.palette(ThemeStore.themeId).defaultAccent, current == null, "AUTO") { ThemeStore.chooseAccent(null) }
        presets.forEach { c -> ColorSwatch(c, current == c, null) { ThemeStore.chooseAccent(c) } }
    }
    HexAccentInput()
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, label: String?, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(40.dp) // ≥ touch comfort within scroll row
                .border(
                    if (selected) 2.dp else 1.dp,
                    if (selected) JarvisColors.CyanPrimary else JarvisColors.Border,
                    CircleShape,
                )
                .padding(3.dp)
                .background(color, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                val check = if (color.luminance() > 0.5f) Color.Black else Color.White
                Text("✓", color = check, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (label != null) Text(label, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
    }
}

@Composable
private fun HexAccentInput() {
    var hex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var error by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = hex, onValueChange = { hex = it.take(7); error = false }, singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = JarvisColors.TextPrimary),
            cursorBrush = SolidColor(JarvisColors.CyanPrimary),
            decorationBox = { inner ->
                Row(Modifier.border(1.dp, if (error) JarvisColors.DangerRed else JarvisColors.Border, RoundedCornerShape(4.dp)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                    if (hex.isEmpty()) Text("#RRGGBB custom accent", color = JarvisColors.TextDim, style = MaterialTheme.typography.bodySmall)
                    inner()
                }
            },
            modifier = Modifier.weight(1f),
        )
        Text("APPLY", style = MaterialTheme.typography.labelLarge, color = JarvisColors.CyanPrimary,
            modifier = Modifier.clickable {
                val parsed = parseHexColor(hex)
                if (parsed != null) { ThemeStore.chooseAccent(parsed); hex = "" } else error = true
            }.padding(6.dp))
    }
}

@Composable
private fun WallpaperPicker() {
    val presets: List<Pair<String, Color?>> = listOf(
        "GRADIENT" to null,
        "INK" to Color(0xFF05070F),
        "PLUM" to Color(0xFF160A2E),
        "TEAL" to Color(0xFF06232B),
        "SLATE" to Color(0xFF11151F),
        "SNOW" to Color(0xFFE8EEF6),
    )
    val current = ThemeStore.wallpaper
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        presets.forEach { (name, c) ->
            val sel = current == c
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val box = Modifier.width(54.dp).height(38.dp)
                    .border(
                        if (sel) 2.dp else 1.dp,
                        if (sel) JarvisColors.CyanPrimary else JarvisColors.Border,
                        RoundedCornerShape(7.dp),
                    )
                    .clickable { ThemeStore.chooseWallpaper(c) }
                Box(
                    if (c == null) box.background(VisionColors.PlasmaSweep, RoundedCornerShape(7.dp))
                    else box.background(c, RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) { if (sel) Text("✓", color = JarvisColors.CyanPrimary, style = MaterialTheme.typography.labelLarge) }
                Spacer(Modifier.height(4.dp))
                Text(name, style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
            }
        }
    }
}

/** Parse "#RRGGBB" or "RRGGBB" → [Color]; null if malformed. */
private fun parseHexColor(s: String): Color? {
    val t = s.trim().removePrefix("#")
    if (t.length != 6) return null
    return try {
        Color(android.graphics.Color.parseColor("#$t"))
    } catch (e: IllegalArgumentException) {
        null
    }
}
