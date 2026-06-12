package com.kianirani.jarvis.ui.screen.election

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.Brush
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.glassPanel
import com.kianirani.jarvis.ui.theme.visionEnter

/**
 * Brain Election — live mesh view: every node's Brain Score, the elected
 * primary Brain, and manual-override control (ADR-006).
 */
@Composable
fun BrainElectionScreen(viewModel: BrainElectionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .background(VisionColors.ScreenBackdrop)
            .systemBarsPadding()
            .padding(20.dp)
    ) {
        Text("BRAIN ELECTION", style = MaterialTheme.typography.headlineLarge, color = VisionColors.CyanPrimary)
        Text(
            if (state.manualOverrideId != null) "MODE · MANUAL OVERRIDE" else "MODE · AUTO (HIGHEST SCORE)",
            style = MaterialTheme.typography.labelSmall,
            color = if (state.manualOverrideId != null) JarvisColors.WarningAmber else JarvisColors.TextDim,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(state.nodes, key = { _, n -> n.id }) { i, node ->
                NodeScoreCard(
                    node = node,
                    maxScore = state.nodes.maxOfOrNull { it.score }?.coerceAtLeast(1) ?: 1,
                    index = i,
                    onTap = { viewModel.onNodeTapped(node.id) },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PairDeviceSection()
        Spacer(Modifier.weight(1f))
        if (state.manualOverrideId != null) {
            Text(
                "TAP ELECTED NODE TO RETURN TO AUTO",
                style = MaterialTheme.typography.labelSmall,
                color = JarvisColors.TextDim,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun NodeScoreCard(node: BrainNodeUi, maxScore: Int, index: Int, onTap: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "elect")
    val electGlow by inf.animateFloat(0.3f, 0.9f, infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "eg")
    val borderColor = when {
        node.isElected -> VisionColors.CyanPrimary.copy(alpha = electGlow)
        !node.online -> VisionColors.TextDim
        else -> VisionColors.Border
    }
    val glowColor = if (node.isElected) VisionColors.CyanGlow else VisionColors.Border.copy(alpha = 0.15f)
    val fillFraction by animateFloatAsState(
        targetValue = node.score / maxScore.toFloat(),
        animationSpec = tween(durationMillis = 250),
        label = "scoreBar",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .visionEnter(index)
            .glassPanel(radius = 10.dp, glow = glowColor, border = borderColor)
            .clickable(onClick = onTap)
            .semantics { contentDescription = "Node ${node.name}, score ${node.score}" + if (node.isElected) ", elected brain" else "" }
            .padding(14.dp)
            .heightIn(min = 44.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(node.name.uppercase(), style = MaterialTheme.typography.bodyLarge,
                color = if (node.online) VisionColors.TextPrimary else VisionColors.TextDim)
            if (node.isElected) {
                Spacer(Modifier.width(8.dp))
                Text("◉ BRAIN", style = MaterialTheme.typography.labelLarge, color = VisionColors.NeonGreen)
            }
            Spacer(Modifier.weight(1f))
            Text(if (node.online) "${node.score}" else "OFFLINE",
                style = MaterialTheme.typography.titleLarge,
                color = if (node.online) VisionColors.CyanPrimary else VisionColors.DangerRed)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(VisionColors.GridLine)) {
            Box(
                Modifier
                    .fillMaxWidth(fillFraction)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (node.isElected) VisionColors.PlasmaSweep else Brush.horizontalGradient(listOf(VisionColors.BlueDeep, VisionColors.CyanSecondary)))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(node.detail, style = MaterialTheme.typography.bodySmall)
    }
}
