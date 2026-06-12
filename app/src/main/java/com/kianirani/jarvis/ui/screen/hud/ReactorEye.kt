package com.kianirani.jarvis.ui.screen.hud

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.kianirani.jarvis.ui.theme.VisionColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * THE EYE OF VISION — the reactor-iris centerpiece of the home HUD
 * (USER DIRECTIVE 2026-06-12: a central eye/reactor, digital-art quality).
 *
 * Layers, outside in: segmented arc-reactor ring → counter-rotating energy
 * blades → tick ring → iris (plasma gradient with radial striations) → pupil
 * (drifts when idle, locks center + flares when listening) → eyelid blink.
 */
@Composable
fun ReactorEye(listening: Boolean, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "eye")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(24000, easing = LinearEasing)), label = "spin")
    val spin2 by inf.animateFloat(360f, 0f, infiniteRepeatable(tween(15000, easing = LinearEasing)), label = "spin2")
    val breath by inf.animateFloat(0.95f, 1.05f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")
    val glow by inf.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    // Idle gaze drift: slow figure-eight wander of the pupil.
    val gazeT by inf.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "gaze")
    // Blink: eyelid sweeps closed and open once per cycle, mostly open.
    val blink by inf.animateFloat(
        0f, 0f,
        infiniteRepeatable(
            keyframes {
                durationMillis = 6200
                0f at 0; 0f at 5600; 1f at 5800; 1f at 5860; 0f at 6080; 0f at 6200
            },
        ),
        label = "blink",
    )

    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(cx, cy) * 0.96f

        // ambient halo
        drawCircle(
            Brush.radialGradient(
                listOf(VisionColors.Violet.copy(alpha = 0.20f * glow * 2f), Color.Transparent),
                Offset(cx, cy), maxR * 1.1f,
            ),
            maxR * 1.1f, Offset(cx, cy),
        )

        // ── arc-reactor segment ring (outer) ──
        val segR = maxR * 0.92f
        rotate(spin, Offset(cx, cy)) {
            repeat(12) { i ->
                val a0 = i * 30f + 4f
                drawArc(
                    color = VisionColors.CyanSecondary.copy(alpha = if (i % 3 == 0) 0.85f else 0.35f),
                    startAngle = a0, sweepAngle = 22f, useCenter = false,
                    topLeft = Offset(cx - segR, cy - segR), size = Size(segR * 2, segR * 2),
                    style = Stroke(width = (if (i % 3 == 0) 3.dp else 1.5.dp).toPx()),
                )
            }
        }

        // ── counter-rotating energy blades ──
        val bladeR = maxR * 0.74f
        rotate(spin2, Offset(cx, cy)) {
            repeat(8) { i ->
                val a = (i * 45f) * (PI / 180f).toFloat()
                val inner = bladeR * 0.78f
                val p = Path().apply {
                    moveTo(cx + inner * cos(a - 0.06f), cy + inner * sin(a - 0.06f))
                    lineTo(cx + bladeR * cos(a), cy + bladeR * sin(a))
                    lineTo(cx + inner * cos(a + 0.06f), cy + inner * sin(a + 0.06f))
                    close()
                }
                drawPath(p, VisionColors.Magenta.copy(alpha = 0.55f * glow))
            }
            drawCircle(
                VisionColors.BorderViolet.copy(alpha = 0.7f), bladeR, Offset(cx, cy),
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)),
            )
        }

        // ── tick ring ──
        val tickR = maxR * 0.60f
        repeat(60) { i ->
            val a = (i * 6f) * (PI / 180f).toFloat()
            val len = if (i % 5 == 0) 7.dp.toPx() else 3.dp.toPx()
            drawLine(
                VisionColors.CyanSecondary.copy(alpha = if (i % 5 == 0) 0.7f else 0.3f),
                Offset(cx + tickR * cos(a), cy + tickR * sin(a)),
                Offset(cx + (tickR - len) * cos(a), cy + (tickR - len) * sin(a)),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // ── iris ──
        val irisR = maxR * 0.46f * breath
        drawCircle(
            Brush.radialGradient(
                listOf(VisionColors.VioletDeep, VisionColors.Violet, VisionColors.CyanPrimary.copy(alpha = 0.9f)),
                Offset(cx, cy), irisR,
            ),
            irisR, Offset(cx, cy),
        )
        // radial striations give the iris its fibrous texture
        repeat(48) { i ->
            val a = (i * 7.5f) * (PI / 180f).toFloat()
            drawLine(
                Color.Black.copy(alpha = 0.22f),
                Offset(cx + irisR * 0.38f * cos(a), cy + irisR * 0.38f * sin(a)),
                Offset(cx + irisR * 0.96f * cos(a), cy + irisR * 0.96f * sin(a)),
                strokeWidth = 1.2.dp.toPx(),
            )
        }
        drawCircle(
            VisionColors.CyanPrimary.copy(alpha = glow), irisR + 6.dp.toPx(), Offset(cx, cy),
            style = Stroke(2.dp.toPx()),
        )

        // ── pupil: wanders when idle, locks + flares when listening ──
        val wander = irisR * 0.16f
        val px = if (listening) cx else cx + wander * sin(gazeT) * cos(gazeT)
        val py = if (listening) cy else cy + wander * sin(gazeT * 2f) * 0.6f
        val pupilR = irisR * (if (listening) 0.40f else 0.30f)
        drawCircle(
            Brush.radialGradient(
                listOf(
                    if (listening) VisionColors.NeonGreen else VisionColors.CyanPrimary,
                    VisionColors.Background,
                ),
                Offset(px, py), pupilR * 1.4f,
            ),
            pupilR, Offset(px, py),
        )
        // specular glint
        drawCircle(Color.White.copy(alpha = 0.85f), pupilR * 0.18f, Offset(px - pupilR * 0.35f, py - pupilR * 0.35f))

        // ── eyelid blink: shutters sweep over the iris, CLIPPED to the eye's
        // circle so it reads as an eyelid — not a black rectangle (bug fix).
        if (blink > 0.01f) {
            val lidR = irisR + 8.dp.toPx()
            val sweep = lidR * blink // each shutter covers up to half the circle at full blink
            val eye = Path().apply { addOval(Rect(cx - lidR, cy - lidR, cx + lidR, cy + lidR)) }
            clipPath(eye) {
                drawRect(VisionColors.Background, Offset(cx - lidR, cy - lidR), Size(lidR * 2f, sweep))
                drawRect(VisionColors.Background, Offset(cx - lidR, cy + lidR - sweep), Size(lidR * 2f, sweep))
            }
        }
    }
}
