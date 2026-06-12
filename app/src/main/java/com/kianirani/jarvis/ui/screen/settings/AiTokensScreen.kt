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
import com.kianirani.jarvis.data.ai.AiProvider
import com.kianirani.jarvis.data.ai.AiProviderStore
import com.kianirani.jarvis.ui.theme.JarvisColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AiTokensViewModel @Inject constructor(private val store: AiProviderStore) : ViewModel() {
    private val _saved = MutableStateFlow(snapshot())
    val saved = _saved.asStateFlow()

    private fun snapshot(): Map<AiProvider, Boolean> =
        AiProvider.entries.associateWith { store.token(it) != null }

    fun save(p: AiProvider, token: String) {
        store.setToken(p, token)
        _saved.value = snapshot()
    }

    fun clear(p: AiProvider) {
        store.setToken(p, null)
        _saved.value = snapshot()
    }
}

/** AI PROVIDERS settings — token slot per provider, encrypted at rest. */
@Composable
fun AiTokensScreen(vm: AiTokensViewModel = hiltViewModel(), onBack: () -> Unit = {}) {
    val saved by vm.saved.collectAsState()
    Column(
        Modifier.fillMaxSize().background(JarvisColors.Background).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ BACK", style = MaterialTheme.typography.labelLarge, color = JarvisColors.TextDim,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
            Text("AI PROVIDERS", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.CyanPrimary)
        }
        Text(
            "Add a token for any provider. Vision routes every question to the best configured provider — local brain first, then cloud.",
            style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim,
        )
        AiProvider.entries.forEach { p -> ProviderCard(p, saved[p] == true, vm::save, vm::clear) }
    }
}

@Composable
private fun ProviderCard(
    p: AiProvider,
    hasToken: Boolean,
    onSave: (AiProvider, String) -> Unit,
    onClear: (AiProvider) -> Unit,
) {
    var input by remember(p, hasToken) { mutableStateOf("") }
    val accent = if (hasToken) JarvisColors.NeonGreen else JarvisColors.Border
    Column(
        Modifier.fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .background(JarvisColors.Surface, RoundedCornerShape(6.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(p.displayName, style = MaterialTheme.typography.labelLarge, color = JarvisColors.TextPrimary)
            Spacer(Modifier.weight(1f))
            Text(
                if (hasToken) "◉ CONFIGURED" else "○ NO TOKEN",
                style = MaterialTheme.typography.labelSmall,
                color = if (hasToken) JarvisColors.NeonGreen else JarvisColors.TextDim,
            )
        }
        Text("model: ${p.defaultModel}", style = MaterialTheme.typography.labelSmall, color = JarvisColors.TextDim)
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
                                if (hasToken) "token saved — paste to replace" else "paste API token",
                                color = JarvisColors.TextDim, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Text("SAVE", style = MaterialTheme.typography.labelLarge, color = JarvisColors.CyanPrimary,
                modifier = Modifier.clickable(enabled = input.isNotBlank()) { onSave(p, input); input = "" }.padding(6.dp))
            if (hasToken) {
                Text("CLEAR", style = MaterialTheme.typography.labelLarge, color = JarvisColors.DangerRed,
                    modifier = Modifier.clickable { onClear(p) }.padding(6.dp))
            }
        }
    }
}
