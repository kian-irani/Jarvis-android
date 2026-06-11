package com.kianirani.jarvis.ui.screen.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.kianirani.jarvis.brain.discovery.BrainCandidate
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kianirani.jarvis.ui.theme.JarvisColors

/**
 * Setup Wizard — 4 steps: 1 Welcome/name · 2 Brain discovery (mDNS/QR/Token)
 * · 3 Connect & verify · 4 Done. Forward slides left, back slides right.
 */
@Composable
fun SetupWizardScreen(
    viewModel: SetupWizardViewModel = hiltViewModel(),
    onFinished: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .background(JarvisColors.Background)
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        StepIndicator(current = state.step)
        Spacer(Modifier.height(28.dp))
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                if (targetState > initialState)
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                else
                    (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
            },
            label = "wizardStep",
            modifier = Modifier.weight(1f),
        ) { step ->
            when (step) {
                0 -> StepWelcome(state.deviceName, viewModel::onDeviceNameChanged)
                1 -> StepDiscovery(
                    state.discoveryMethod, viewModel::onDiscoveryMethodSelected,
                    state.token, viewModel::onTokenChanged,
                    state.candidates, state.selectedCandidate, viewModel::onCandidateSelected,
                )
                2 -> StepConnect(state.connectStatus)
                else -> StepDone(state.deviceName)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.step in 1..2) {
                WizardButton("BACK", primary = false, Modifier.weight(1f)) { viewModel.back() }
            }
            WizardButton(
                when (state.step) { 2 -> "CONNECT"; 3 -> "ENTER VISION"; else -> "NEXT" },
                primary = true,
                Modifier.weight(2f),
                enabled = state.canAdvance,
            ) { if (state.step == 3) onFinished() else viewModel.next() }
        }
    }
}

@Composable
private fun StepIndicator(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i <= current) JarvisColors.CyanPrimary else JarvisColors.GridLine)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text("STEP ${current + 1}/4", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StepWelcome(name: String, onName: (String) -> Unit) {
    Column {
        Text("VISION OS", style = MaterialTheme.typography.displayLarge)
        Text("SOVEREIGN INTELLIGENCE", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(28.dp))
        Text("Name this device for the brain mesh.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        WizardField(name, onName, label = "DEVICE NAME", helper = "Shown in node registry and election.")
    }
}

@Composable
private fun StepDiscovery(
    selected: DiscoveryMethod,
    onSelect: (DiscoveryMethod) -> Unit,
    token: String,
    onToken: (String) -> Unit,
    candidates: List<BrainCandidate> = emptyList(),
    selectedCandidate: BrainCandidate? = null,
    onCandidate: (BrainCandidate) -> Unit = {},
) {
    Column {
        Text("FIND YOUR BRAIN", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        DiscoveryMethod.entries.forEach { m ->
            val active = m == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, if (active) JarvisColors.CyanPrimary else JarvisColors.Border, RoundedCornerShape(6.dp))
                    .background(if (active) JarvisColors.CyanFaint else JarvisColors.Surface)
                    .clickable { onSelect(m) }
                    .padding(16.dp)
                    .heightIn(min = 44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (active) "◉" else "○", color = if (active) JarvisColors.CyanPrimary else JarvisColors.TextDim)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(m.title, style = MaterialTheme.typography.bodyLarge)
                    Text(m.subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (selected == DiscoveryMethod.TOKEN) {
            Spacer(Modifier.height(12.dp))
            WizardField(token, onToken, label = "PAIRING TOKEN", helper = "From your Brain's setup output.")
        }
        if (selected == DiscoveryMethod.MDNS) {
            Spacer(Modifier.height(14.dp))
            if (candidates.isEmpty()) ScanningPulse() else CandidateList(candidates, selectedCandidate, onCandidate)
        }
    }
}

/** HUD scanning indicator while mDNS has found nothing yet. */
@Composable
private fun ScanningPulse() {
    val transition = rememberInfiniteTransition(label = "scan")
    val alpha by transition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scanAlpha",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("◉", color = JarvisColors.CyanPrimary.copy(alpha = alpha))
        Spacer(Modifier.width(10.dp))
        Text("SCANNING MESH FOR BRAINS…", style = MaterialTheme.typography.labelLarge, color = JarvisColors.TextDim)
    }
}

@Composable
private fun CandidateList(candidates: List<BrainCandidate>, selected: BrainCandidate?, onSelect: (BrainCandidate) -> Unit) {
    Column {
        Text("BRAINS IN RANGE — ${candidates.size}", style = MaterialTheme.typography.labelMedium, color = JarvisColors.CyanSecondary)
        candidates.forEach { c ->
            val active = c.name == selected?.name
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, if (active) JarvisColors.NeonGreen else JarvisColors.Border, RoundedCornerShape(6.dp))
                    .background(if (active) JarvisColors.CyanFaint else JarvisColors.Surface)
                    .clickable { onSelect(c) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (active) "◈" else "◇", color = if (active) JarvisColors.NeonGreen else JarvisColors.TextDim)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(c.name, style = MaterialTheme.typography.bodyLarge)
                    Text("${c.host}:${c.port}", style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
                }
            }
        }
    }
}

@Composable
private fun StepConnect(status: ConnectStatus) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Text(
            when (status) {
                ConnectStatus.IDLE -> "READY TO CONNECT"
                ConnectStatus.CONNECTING -> "HANDSHAKE…"
                ConnectStatus.OK -> "LINK ESTABLISHED"
                ConnectStatus.FAILED -> "CONNECTION FAILED"
            },
            style = MaterialTheme.typography.headlineLarge,
            color = when (status) {
                ConnectStatus.OK -> JarvisColors.NeonGreen
                ConnectStatus.FAILED -> JarvisColors.DangerRed
                else -> JarvisColors.CyanSecondary
            },
        )
        if (status == ConnectStatus.FAILED) {
            Spacer(Modifier.height(10.dp))
            Text("Check that the Brain is online, then tap CONNECT to retry.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StepDone(name: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp))
        Text("◉", style = MaterialTheme.typography.displayLarge, color = JarvisColors.NeonGreen)
        Spacer(Modifier.height(16.dp))
        Text("$name IS PART OF THE MESH", style = MaterialTheme.typography.headlineLarge)
        Text("Brain election runs automatically.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WizardField(value: String, onValue: (String) -> Unit, label: String, helper: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        supportingText = { Text(helper, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = JarvisColors.CyanPrimary,
            unfocusedBorderColor = JarvisColors.Border,
            focusedTextColor = JarvisColors.TextPrimary,
            unfocusedTextColor = JarvisColors.TextPrimary,
            cursorColor = JarvisColors.CyanPrimary,
        ),
    )
}

@Composable
private fun WizardButton(label: String, primary: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    !enabled -> JarvisColors.Surface
                    primary -> JarvisColors.CyanPrimary
                    else -> JarvisColors.Surface
                }
            )
            .border(1.dp, if (primary && enabled) JarvisColors.CyanPrimary else JarvisColors.Border, RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !enabled -> JarvisColors.TextDim
                primary -> JarvisColors.Background
                else -> JarvisColors.TextSecondary
            },
        )
    }
}
