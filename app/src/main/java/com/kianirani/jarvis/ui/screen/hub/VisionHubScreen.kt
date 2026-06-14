package com.kianirani.jarvis.ui.screen.hub

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter

/**
 * VISION HUB (v12) — a home for upcoming Layer-3/4 workspaces. Currently entries
 * are placeholders that toast "coming soon"; each becomes a real screen in a
 * later phase (Vision Lab, Vision Notes, Files, Servers).
 */
@Composable
fun VisionHubScreen(onBack: () -> Unit = {}) {
    val ctx = LocalContext.current
    val entries = listOf(
        "Vision Lab" to "experiment with agents & prompts",
        "Vision Notes" to "quick notes with semantic recall",
        "Files" to "device-to-device fast transfer",
        "Servers" to "remote nodes & compute (WAN mesh)",
    )
    Column(
        Modifier.fillMaxSize().background(JarvisColors.ScreenBackdrop).systemBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("‹ BACK", style = MaterialTheme.typography.labelLarge, color = JarvisColors.TextDim,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 12.dp))
            Text("VISION HUB", style = MaterialTheme.typography.headlineLarge, color = JarvisColors.CyanPrimary)
        }
        entries.forEachIndexed { i, (title, sub) ->
            Row(
                Modifier.fillMaxWidth().visionEnter(i).glassPanel(radius = 16.dp)
                    .clickable { Toast.makeText(ctx, "$title — coming soon", Toast.LENGTH_SHORT).show() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(40.dp).background(JarvisColors.CyanFaint, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                    Text("✦", style = MaterialTheme.typography.titleMedium, color = JarvisColors.CyanPrimary)
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = JarvisColors.TextPrimary)
                    Text(sub, style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
                }
                Text("›", style = MaterialTheme.typography.headlineMedium, color = JarvisColors.CyanPrimary)
            }
        }
    }
}
