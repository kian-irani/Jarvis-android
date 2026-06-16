package com.kianirani.jarvis.ui.screen.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kianirani.jarvis.ui.theme.ThemeStore
import com.kianirani.jarvis.ui.theme.VisionColors
import com.kianirani.jarvis.ui.theme.VisionFonts
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * THE VISION AI-CORE ORB (RD1 redesign, 2026-06-15) — rebuilt to the reference
 * images in `Example/`: a luminous **ring / portal**, not a solid sphere.
 *
 * Layers (outside-in): a wide soft halo bloom · a bright circumferential **ring**
 * stroked with a rotating cyan→blue→violet→magenta sweep (drawn several times at
 * decreasing alpha to fake a bloom) · a dark translucent core so the wordmark
 * reads through it · drifting **neural particles** in the ring band · faint
 * **reflection ripples** below. The centred "VISION / AI CORE ONLINE" wordmark
 * is laid over the canvas in the bundled display face.
 *
 * Listening tightens the ring and brightens the halo. All motion honours
 * [ThemeStore.animations] (reduced-motion → calm frozen pose). Colours read the
 * state-backed [VisionColors] so theme + accent recolour the core live.
 */
@Composable
fun VisionOrb(listening: Boolean, modifier: Modifier = Modifier) {
    val animate = ThemeStore.animations
    val inf = rememberInfiniteTransition(label = "orb")
    val breathA by inf.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")
    val glowA by inf.animateFloat(0.5f, 0.98f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val spinA by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(20000, easing = LinearEasing)), label = "spin")
    val spin2A by inf.animateFloat(360f, 0f, infiniteRepeatable(tween(28000, easing = LinearEasing)), label = "spin2")
    val twinkleA by inf.animateFloat(0f, (2f * Math.PI).toFloat(), infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "twinkle")

    val breath = if (animate) breathA else 1f
    val glow = if (animate) glowA else 0.75f
    val spin = if (animate) spinA else 24f
    val spin2 = if (animate) spin2A else -16f
    val twinkle = if (animate) twinkleA else 1.2f

    val accent = VisionColors.CyanPrimary
    val violet = VisionColors.Violet
    val magenta = VisionColors.Magenta
    val blue = VisionColors.BlueDeep

    // Stable particle field (polar within the ring band) — regenerated only if seed changes.
    val particles = remember {
        val rnd = Random(7)
        List(58) {
            Particle(
                angle = rnd.nextFloat() * (2f * Math.PI).toFloat(),
                radiusFrac = 0.34f + rnd.nextFloat() * 0.74f, // some inside, most near the ring
                sizeFrac = 0.004f + rnd.nextFloat() * 0.011f,
                phase = rnd.nextFloat() * (2f * Math.PI).toFloat(),
            )
        }
    }

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = minOf(cx, cy)
            val ringR = base * (if (listening) 0.62f else 0.68f) * breath
            val stroke = base * 0.085f
            val haloAlpha = (if (listening) 0.6f else 0.4f) * glow

            // 1. Layered glow — a wide soft bloom + a tighter brighter halo (depth).
            drawCircle(
                Brush.radialGradient(
                    0f to accent.copy(alpha = haloAlpha * 0.55f),
                    0.45f to violet.copy(alpha = haloAlpha * 0.5f),
                    0.8f to magenta.copy(alpha = haloAlpha * 0.18f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy), radius = base * 1.05f,
                ),
                base * 1.05f, Offset(cx, cy),
            )
            drawCircle(
                Brush.radialGradient(
                    0f to accent.copy(alpha = haloAlpha * 0.5f),
                    0.7f to blue.copy(alpha = haloAlpha * 0.35f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy), radius = ringR * 1.25f,
                ),
                ringR * 1.25f, Offset(cx, cy),
            )

            // 2. Inner core — a translucent blue depth gradient with a top-left
            //    light reflection so it reads as a polished sphere, not a hole.
            drawCircle(
                Brush.radialGradient(
                    0f to blue.copy(alpha = 0.22f),
                    0.55f to Color.Black.copy(alpha = 0.34f),
                    1f to Color.Transparent,
                    center = Offset(cx, cy - ringR * 0.04f), radius = ringR,
                ),
                ringR, Offset(cx, cy),
            )
            drawCircle( // inner reflection highlight (top-left)
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.16f * glow),
                    1f to Color.Transparent,
                    center = Offset(cx - ringR * 0.32f, cy - ringR * 0.36f), radius = ringR * 0.6f,
                ),
                ringR * 0.6f, Offset(cx - ringR * 0.32f, cy - ringR * 0.36f),
            )

            // 3. Energy rings — concentric thin rings, counter-rotating, at decreasing alpha.
            val sweep = Brush.sweepGradient(
                0f to accent, 0.3f to blue, 0.55f to violet, 0.78f to magenta, 1f to accent,
                center = Offset(cx, cy),
            )
            rotate(spin2, Offset(cx, cy)) {
                drawCircle(accent, ringR * 1.16f, Offset(cx, cy), alpha = 0.14f * glow, style = Stroke(base * 0.006f))
                drawCircle(violet, ringR * 0.5f, Offset(cx, cy), alpha = 0.20f * glow, style = Stroke(base * 0.008f))
            }

            // 4. The luminous main ring — rotating sweep, drawn 3× for a bloom.
            rotate(spin, Offset(cx, cy)) {
                drawCircle(sweep, ringR, Offset(cx, cy), alpha = 0.22f * glow, style = Stroke(stroke * 2.4f))
                drawCircle(sweep, ringR, Offset(cx, cy), alpha = 0.5f * glow, style = Stroke(stroke * 1.4f))
                drawCircle(sweep, ringR, Offset(cx, cy), alpha = 0.95f, style = Stroke(stroke * 0.7f))
            }

            // 5. Neural particles + connection lines (a living network). Precompute
            //    positions, link near pairs with faint glowing threads, then draw nodes.
            val pts = particles.map { p ->
                val pr = ringR * p.radiusFrac
                Triple(cx + cos(p.angle) * pr, cy + sin(p.angle) * pr, p)
            }
            val linkDist = base * 0.26f
            for (i in pts.indices) {
                for (j in i + 1 until pts.size) {
                    val dx = pts[i].first - pts[j].first
                    val dy = pts[i].second - pts[j].second
                    val d = kotlin.math.hypot(dx, dy)
                    if (d < linkDist) {
                        val a = (1f - d / linkDist) * 0.28f * glow
                        drawLine(accent.copy(alpha = a), Offset(pts[i].first, pts[i].second), Offset(pts[j].first, pts[j].second), strokeWidth = base * 0.004f)
                    }
                }
            }
            pts.forEach { (px, py, p) ->
                val tw = 0.35f + 0.65f * ((sin(twinkle + p.phase) + 1f) / 2f)
                val ps = base * p.sizeFrac
                drawCircle(Color.White.copy(alpha = 0.9f * tw), ps, Offset(px, py))
                drawCircle(accent.copy(alpha = 0.30f * tw), ps * 2.6f, Offset(px, py)) // soft glow
            }

            // 6. Reflection ripples below the core — concentric flattened arcs
            repeat(3) { i ->
                val rr = ringR * (0.9f + i * 0.34f)
                val a = (0.18f - i * 0.05f) * glow
                drawArc(
                    color = accent.copy(alpha = a),
                    startAngle = 18f, sweepAngle = 144f, useCenter = false,
                    topLeft = Offset(cx - rr, cy + ringR * 0.58f - rr * 0.18f),
                    size = Size(rr * 2f, rr * 0.36f),
                    style = Stroke(width = base * 0.012f),
                )
            }
        }

        // Centred wordmark (bundled display face; mono caption for the HUD status line)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                "VISION",
                fontFamily = VisionFonts.display,
                fontWeight = FontWeight.Light,
                fontSize = 34.sp,
                letterSpacing = 10.sp,
                color = VisionColors.TextPrimary,
            )
            Text(
                if (listening) "LISTENING…" else "AI CORE ONLINE",
                fontFamily = VisionFonts.mono,
                fontSize = 10.sp,
                letterSpacing = 4.sp,
                color = if (listening) VisionColors.NeonGreen else VisionColors.TextSecondary,
            )
        }
    }
}

/** A single neural particle, placed in polar coords relative to the ring radius. */
private data class Particle(
    val angle: Float,
    val radiusFrac: Float,
    val sizeFrac: Float,
    val phase: Float,
)
