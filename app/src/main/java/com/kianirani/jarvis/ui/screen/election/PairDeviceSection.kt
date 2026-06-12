package com.kianirani.jarvis.ui.screen.election

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.kianirani.jarvis.brain.discovery.JoinPayload
import com.kianirani.jarvis.brain.discovery.LocalPairingInfoProvider
import com.kianirani.jarvis.brain.discovery.QrPairing
import com.kianirani.jarvis.ui.theme.JarvisColors
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.glassPanel

/**
 * "PAIR NEW DEVICE" — renders this brain's vision://join QR (spec §5) so other
 * devices can scan or copy it. Collapsed by default at the bottom of the
 * Election screen.
 */
@Composable
fun PairDeviceSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val provider = remember { LocalPairingInfoProvider(context) }
    var open by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .glassPanel(radius = 10.dp, glow = VisionColors.MagentaGlow, border = VisionColors.BorderViolet)
            .clickable { open = !open }
            .padding(14.dp),
    ) {
        Text(
            if (open) "▼ PAIR NEW DEVICE" else "▶ PAIR NEW DEVICE",
            style = MaterialTheme.typography.labelLarge,
            color = VisionColors.Magenta,
        )
        if (open) {
            Spacer(Modifier.height(12.dp))
            val payload = remember { provider.payload() }
            if (payload == null) {
                Text("No LAN connection — join a Wi-Fi network to pair.", style = MaterialTheme.typography.bodySmall)
            } else {
                val matrix = remember(payload) { QrPairing.matrix(payload, size = 33) }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.background(Color.White).padding(8.dp)) {
                        Canvas(Modifier.size(220.dp)) {
                            val n = matrix.size
                            val cell = size.minDimension / n
                            for (y in 0 until n) for (x in 0 until n) {
                                if (matrix[y][x]) {
                                    drawRect(
                                        Color.Black,
                                        topLeft = Offset(x * cell, y * cell),
                                        size = Size(cell, cell),
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(payload.encode(), style = MaterialTheme.typography.bodySmall, color = JarvisColors.TextDim)
                    Text("Scan with another Vision device, or paste as PAIRING TOKEN.", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                MeshNodeScript(payload)
            }
        }
    }
}

/**
 * "ADD MESH NODE" — copyable terminal one-liner built from this brain's live
 * pairing info. The user pastes it into ANOTHER device's terminal (Linux/macOS/
 * server) and that device joins this brain as a mesh node via node-agent/agent.py.
 */
@Composable
private fun MeshNodeScript(payload: JoinPayload) {
    val clipboard = LocalClipboardManager.current
    val script = remember(payload) {
        "curl -sL https://raw.githubusercontent.com/kian-irani/Jarvis-android/main/node-agent/agent.py " +
            "-o vision-node.py && nohup python3 vision-node.py " +
            "--host ${payload.host} --port ${payload.port} --token ${payload.token} " +
            "> vision-node.log 2>&1 & sleep 2 && tail -2 vision-node.log"
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            "ADD MESH NODE",
            style = MaterialTheme.typography.labelLarge,
            color = VisionColors.Magenta,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x33000000))
                .border(1.dp, VisionColors.BorderViolet, RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) {
            Text(
                script,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = JarvisColors.TextDim,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(VisionColors.Magenta.copy(alpha = 0.18f))
                .border(1.dp, VisionColors.Magenta, RoundedCornerShape(8.dp))
                .clickable { clipboard.setText(AnnotatedString(script)) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text("COPY", style = MaterialTheme.typography.labelLarge, color = VisionColors.Magenta)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Paste in the other device's terminal (needs python3).",
            style = MaterialTheme.typography.bodySmall,
            color = JarvisColors.TextDim,
        )
    }
}
