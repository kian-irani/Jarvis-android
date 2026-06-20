package com.kianirani.jarvis.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kianirani.jarvis.core.gesture.Gesture
import com.kianirani.jarvis.core.gesture.GestureAction
import com.kianirani.jarvis.core.gesture.GestureMap
import com.kianirani.jarvis.core.orb.OrbState
import com.kianirani.jarvis.ui.theme.ThemeStore
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.VisionFonts
import com.kianirani.jarvis.ui.theme.VisionIcons
import com.kianirani.jarvis.ui.theme.glassPanel

/** DS-W4 — quick actions on the expanded mini-panel. */
enum class PanelAction { ASK, VOICE, NOTE, OPEN_VISION, CLOSE }

/**
 * DS-W2/W3/W4 — the floating widget's Compose content: a glowing **orb** whose look is driven
 * by the [OrbState] machine, gesture handling routed through the user's [GestureMap], and a
 * glass **mini-panel** of quick actions when expanded. Reuses the app's azure→violet
 * [VisionColors], [VisionIcons], and [glassPanel], and honours reduced-motion
 * ([ThemeStore.animations]). Hosted by `FloatingWidgetService` in a WindowManager overlay.
 *
 * Drag (to reposition the window) is reported via [onDrag]; taps map through [gestures] to a
 * [GestureAction] delivered on [onGestureAction]; panel buttons fire [onPanelAction].
 */
@Composable
fun FloatingOrb(
    state: OrbState,
    expanded: Boolean,
    gestures: GestureMap,
    onGestureAction: (GestureAction) -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onPanelAction: (PanelAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.9f),
            exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.9f),
        ) {
            MiniPanel(onPanelAction)
        }

        OrbVisual(
            state = state,
            modifier = Modifier
                .pointerInput(gestures) {
                    detectTapGestures(
                        onTap = { onGestureAction(gestures.actionFor(Gesture.TAP)) },
                        onDoubleTap = { onGestureAction(gestures.actionFor(Gesture.DOUBLE_TAP)) },
                        onLongPress = { onGestureAction(gestures.actionFor(Gesture.LONG_PRESS)) },
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        onDrag(drag.x, drag.y)
                    }
                },
        )
    }
}

/** A compact glowing orb keyed to [state] — collapses to a small dot when sleeping. */
@Composable
private fun OrbVisual(state: OrbState, modifier: Modifier = Modifier) {
    val animate = ThemeStore.animations
    val accent = VisionColors.CyanPrimary
    val violet = VisionColors.Violet
    val magenta = VisionColors.Magenta

    val stateColor = when (state) {
        OrbState.LISTENING -> VisionColors.NeonGreen
        OrbState.THINKING, OrbState.EXECUTING -> accent
        OrbState.SPEAKING -> violet
        OrbState.NOTIFICATION -> VisionColors.WarningAmber
        OrbState.ERROR -> VisionColors.DangerRed
        OrbState.IDLE, OrbState.SLEEPING -> accent
    }
    val glowMul = when (state) {
        OrbState.SPEAKING -> 1.15f
        OrbState.LISTENING -> 1.1f
        OrbState.SLEEPING -> 0.5f
        OrbState.ERROR -> 0.7f
        else -> 1f
    }
    val sizeDp by animateDpAsState(if (state == OrbState.SLEEPING) 34.dp else 64.dp, tween(220), label = "orbSize")

    val inf = rememberInfiniteTransition(label = "orb")
    val breathA by inf.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")
    val glowA by inf.animateFloat(0.5f, 0.95f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val spinA by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "spin")

    val breath = if (animate) breathA else 1f
    val glow = (if (animate) glowA else 0.75f) * glowMul
    val spin = if (animate) spinA else 18f

    Box(modifier.size(sizeDp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = minOf(cx, cy)
            val ringR = base * 0.66f * breath
            val stroke = base * 0.13f
            val haloA = (0.5f * glow).coerceIn(0f, 1f)

            // soft halo bloom
            drawCircle(
                Brush.radialGradient(
                    0f to stateColor.copy(alpha = haloA * 0.6f),
                    0.6f to violet.copy(alpha = haloA * 0.3f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy), radius = base,
                ),
                base, Offset(cx, cy),
            )
            // translucent core depth
            drawCircle(
                Brush.radialGradient(
                    0f to VisionColors.BlueDeep.copy(alpha = 0.22f),
                    0.6f to Color.Black.copy(alpha = 0.3f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy), radius = ringR,
                ),
                ringR, Offset(cx, cy),
            )
            // luminous rotating ring (sweep), drawn twice for bloom
            val sweep = Brush.sweepGradient(
                0f to stateColor, 0.4f to violet, 0.7f to magenta, 1f to stateColor,
                center = Offset(cx, cy),
            )
            rotate(spin, Offset(cx, cy)) {
                drawCircle(sweep, ringR, Offset(cx, cy), alpha = 0.45f * glow, style = Stroke(stroke * 1.6f))
                drawCircle(sweep, ringR, Offset(cx, cy), alpha = 0.95f, style = Stroke(stroke * 0.7f))
            }
            // inner highlight
            drawCircle(
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.18f * glow),
                    1f to Color.Transparent,
                    center = Offset(cx - ringR * 0.3f, cy - ringR * 0.34f), radius = ringR * 0.55f,
                ),
                ringR * 0.55f, Offset(cx - ringR * 0.3f, cy - ringR * 0.34f),
            )
        }
    }
}

@Composable
private fun MiniPanel(onPanelAction: (PanelAction) -> Unit) {
    Column(
        Modifier
            .glassPanel(radius = 18.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "VISION",
            fontFamily = VisionFonts.display,
            fontWeight = FontWeight.Light,
            fontSize = 13.sp,
            letterSpacing = 5.sp,
            color = VisionColors.TextPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelButton(VisionIcons.Spark, "Ask") { onPanelAction(PanelAction.ASK) }
            PanelButton(VisionIcons.Mic, "Voice") { onPanelAction(PanelAction.VOICE) }
            PanelButton(VisionIcons.Memory, "Note") { onPanelAction(PanelAction.NOTE) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelButton(VisionIcons.Home, "Open") { onPanelAction(PanelAction.OPEN_VISION) }
            PanelButton(VisionIcons.Close, "Close") { onPanelAction(PanelAction.CLOSE) }
        }
    }
}

@Composable
private fun PanelButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        Modifier
            .size(64.dp)
            .clip(CircleShape)
            .pointerInput(label) { detectTapGestures(onTap = { onClick() }) }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = VisionColors.CyanPrimary, modifier = Modifier.size(22.dp))
        Text(label, fontFamily = VisionFonts.body, fontSize = 10.sp, color = VisionColors.TextSecondary)
    }
}
