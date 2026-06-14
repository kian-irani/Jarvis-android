package com.kianirani.jarvis.ui.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kianirani.jarvis.data.ai.AiProvider
import com.kianirani.jarvis.data.ai.AiProviderStore
import com.kianirani.jarvis.data.ai.CloudChatRouter
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Live reachability of a single key (user directive 2026-06-14: show active ✓). */
enum class KeyState { TESTING, ACTIVE, FAILED }
data class KeyStatus(val state: KeyState, val detail: String = "")

@HiltViewModel
class AiTokensViewModel @Inject constructor(
    private val store: AiProviderStore,
    private val router: CloudChatRouter,
) : ViewModel() {
    private val _saved = MutableStateFlow(snapshot())
    val saved = _saved.asStateFlow()

    private fun snapshot(): Map<AiProvider, List<String>> =
        AiProvider.entries.associateWith { store.tokens(it) }

    private val _models = MutableStateFlow(modelSnapshot())
    val models = _models.asStateFlow()

    private fun modelSnapshot(): Map<AiProvider, String> =
        AiProvider.entries.associateWith { store.model(it) }

    /** token string → live status (TESTING / ACTIVE / FAILED + reason). */
    private val _status = MutableStateFlow<Map<String, KeyStatus>>(emptyMap())
    val status = _status.asStateFlow()

    init { validateAll() }

    fun add(p: AiProvider, token: String) {
        val t = token.trim()
        store.addToken(p, t)
        _saved.value = snapshot()
        validate(p, t) // immediately ping the provider so the user sees active/✗
    }

    fun remove(p: AiProvider, token: String) {
        store.removeToken(p, token)
        _saved.value = snapshot()
        _status.value = _status.value - token
    }

    fun setModel(p: AiProvider, model: String) {
        store.setModel(p, model)
        _models.value = modelSnapshot()
    }

    /** Manual re-test (TEST button). */
    fun test(p: AiProvider, token: String) = validate(p, token)

    private fun validateAll() {
        snapshot().forEach { (p, tokens) -> tokens.forEach { validate(p, it) } }
    }

    private fun validate(p: AiProvider, token: String) {
        if (token.isBlank()) return
        _status.value = _status.value + (token to KeyStatus(KeyState.TESTING))
        viewModelScope.launch {
            val r = router.test(p, token)
            _status.value = _status.value + (
                token to if (r.isSuccess) {
                    KeyStatus(KeyState.ACTIVE)
                } else {
                    KeyStatus(KeyState.FAILED, r.exceptionOrNull()?.message?.take(70) ?: "no response")
                }
                )
        }
    }
}

@Composable
private fun KeyStatusBadge(status: KeyStatus?) {
    val (label, color) = when (status?.state) {
        KeyState.ACTIVE -> "✓ ACTIVE" to VisionColors.NeonGreen
        KeyState.TESTING -> "⟳ TESTING" to VisionColors.WarningAmber
        KeyState.FAILED -> "✗ FAILED" to VisionColors.DangerRed
        null -> "" to VisionColors.TextDim
    }
    if (label.isNotEmpty()) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(end = 6.dp))
    }
}

/** Last characters of a key, enough to tell keys apart without exposing them. */
private fun mask(token: String): String =
    if (token.length <= 8) "•••" else "…${token.takeLast(6)}"

/** AI PROVIDERS settings — any number of tokens per provider, encrypted at rest. */
@Composable
fun AiTokensScreen(vm: AiTokensViewModel = hiltViewModel(), onBack: () -> Unit = {}) {
    val saved by vm.saved.collectAsState()
    val models by vm.models.collectAsState()
    val statuses by vm.status.collectAsState()
    Column(
        Modifier.fillMaxSize().background(VisionColors.ScreenBackdrop).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ BACK", style = MaterialTheme.typography.labelLarge, color = JarvisColors.TextDim,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
            Text("AI PROVIDERS", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.CyanPrimary)
        }
        Text(
            "Add as many tokens per provider as you like — Vision rotates between them " +
                "automatically on rate limits. Local brain answers first, then cloud.",
            style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim,
        )
        AiProvider.entries.forEachIndexed { i, p ->
            ProviderCard(p, saved[p].orEmpty(), models[p] ?: p.defaultModel, statuses, i, vm::add, vm::remove, vm::setModel, vm::test)
        }
    }
}

@Composable
private fun ProviderCard(
    p: AiProvider,
    tokens: List<String>,
    model: String,
    statuses: Map<String, KeyStatus>,
    index: Int,
    onAdd: (AiProvider, String) -> Unit,
    onRemove: (AiProvider, String) -> Unit,
    onSetModel: (AiProvider, String) -> Unit,
    onTest: (AiProvider, String) -> Unit,
) {
    var input by remember(p) { mutableStateOf("") }
    var modelInput by remember(p, model) { mutableStateOf(model) }
    val hasToken = tokens.isNotEmpty()
    val accent = if (hasToken) VisionColors.NeonGreen else VisionColors.Border
    val glow = if (hasToken) VisionColors.NeonGreen.copy(alpha = 0.25f) else VisionColors.CyanGlow
    Column(
        Modifier.fillMaxWidth()
            .visionEnter(index)
            .glassPanel(radius = 10.dp, glow = glow, border = accent.copy(alpha = 0.55f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(p.displayName, style = MaterialTheme.typography.labelLarge, color = JarvisColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(
                if (hasToken) "◉ ${tokens.size} KEY${if (tokens.size > 1) "S" else ""}" else "○ NO TOKEN",
                style = MaterialTheme.typography.labelSmall,
                color = if (hasToken) JarvisColors.NeonGreen else JarvisColors.TextDim,
            )
        }
        // Editable model id — e.g. OpenRouter needs a real model ("openrouter/auto"
        // or a ":free" slug); blank restores the provider default.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("model", style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
            BasicTextField(
                value = modelInput, onValueChange = { modelInput = it }, singleLine = true,
                textStyle = TextStyle(color = JarvisColors.TextPrimary),
                cursorBrush = SolidColor(JarvisColors.CyanPrimary),
                decorationBox = { inner ->
                    Row(
                        Modifier.border(1.dp, JarvisColors.Border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        if (modelInput.isEmpty()) {
                            Text(p.defaultModel, color = JarvisColors.TextDim, style = MaterialTheme.typography.bodySmall)
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Text("SET", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable { onSetModel(p, modelInput) }.padding(4.dp))
        }
        tokens.forEach { t ->
            Row(
                Modifier.fillMaxWidth()
                    .border(1.dp, JarvisColors.Border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(mask(t), style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                KeyStatusBadge(statuses[t])
                Text("TEST", style = MaterialTheme.typography.labelSmall, color = JarvisColors.CyanSecondary,
                    modifier = Modifier.clickable { onTest(p, t) }.padding(4.dp))
                Text("REMOVE", style = MaterialTheme.typography.labelSmall, color = JarvisColors.DangerRed,
                    modifier = Modifier.clickable { onRemove(p, t) }.padding(4.dp))
            }
            statuses[t]?.takeIf { it.state == KeyState.FAILED && it.detail.isNotBlank() }?.let {
                Text("✗ ${it.detail}", style = MaterialTheme.typography.labelSmall, color = JarvisColors.DangerRed,
                    modifier = Modifier.padding(start = 10.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = input, onValueChange = { input = it }, singleLine = true,
                textStyle = TextStyle(color = JarvisColors.TextPrimary),
                cursorBrush = SolidColor(JarvisColors.CyanPrimary),
                decorationBox = { inner ->
                    Row(
                        Modifier.border(1.dp, JarvisColors.Border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        if (input.isEmpty()) {
                            Text(
                                if (hasToken) "paste another token" else "paste API token",
                                color = JarvisColors.TextDim, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Text("ADD", style = MaterialTheme.typography.labelLarge, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable(enabled = input.isNotBlank()) { onAdd(p, input); input = "" }.padding(6.dp))
        }
    }
}
