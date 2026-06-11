package com.kianirani.jarvis.ui.screen.election

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.kianirani.jarvis.ui.theme.JarvisColors

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
            .background(JarvisColors.Background)
            .systemBarsPadding()
            .padding(20.dp)
    ) {
        Text("BRAIN ELECTION", style = MaterialTheme.typography.headlineLarge)
        Text(
            if (state.manualOverrideId != null) "MODE · MANUAL OVERRIDE" else "MODE · AUTO (HIGHEST SCORE)",
            style = MaterialTheme.typography.labelSmall,
            color = if (state.manualOverrideId != null) JarvisColors.WarningAmber else JarvisColors.TextDim,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.nodes, key = { it.id }) { node ->
                NodeScoreCard(
                    node = node,
                    maxScore = state.nodes.maxOfOrNull { it.score }?.coerceAtLeast(1) ?: 1,
                    onTap = { viewModel.onNodeTapped(node.id) },
                )
            }
        }
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
private fun NodeScoreCard(node: BrainNodeUi, maxScore: Int, onTap: () -> Unit) {
    val borderColor = when {
        node.isElected -> JarvisColors.CyanPrimary
        !node.online -> JarvisColors.TextDim
        else -> JarvisColors.Border
    }
    val fillFraction by animateFloatAsState(
        targetValue = node.score / maxScore.toFloat(),
        animationSpec = tween(durationMillis = 250),
        label = "scoreBar",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .background(if (node.isElected) JarvisColors.CyanFaint else JarvisColors.Surface)
            .clickable(onClick = onTap)
            .semantics { contentDescription = "Node ${node.name}, score ${node.score}" + if (node.isElected) ", elected brain" else "" }
            .padding(14.dp)
            .heightIn(min = 44.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(node.name.uppercase(), style = MaterialTheme.typography.bodyLarge,
                color = if (node.online) JarvisColors.TextPrimary else JarvisColors.TextDim)
            if (node.isElected) {
                Spacer(Modifier.width(8.dp))
                Text("◉ BRAIN", style = MaterialTheme.typography.labelLarge, color = JarvisColors.NeonGreen)
            }
            Spacer(Modifier.weight(1f))
            Text(if (node.online) "${node.score}" else "OFFLINE",
                style = MaterialTheme.typography.titleLarge,
                color = if (node.online) JarvisColors.CyanPrimary else JarvisColors.DangerRed)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(JarvisColors.GridLine)) {
            Box(
                Modifier
                    .fillMaxWidth(fillFraction)
                    .height(4.dp)
                    .background(if (node.isElected) JarvisColors.CyanPrimary else JarvisColors.BlueDeep)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(node.detail, style = MaterialTheme.typography.bodySmall)
    }
}
