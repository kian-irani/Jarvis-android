package com.kianirani.jarvis.ui.screen.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kianirani.jarvis.ui.theme.ThemeStore
import com.kianirani.jarvis.ui.theme.VisionColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * THE VISION ORB (v12 reskin, 2026-06-14) — the soft glowing sphere from the
 * orb-launcher reference, replacing the angular reactor "eye" on the home.
 *
 * Layers (outside-in): a wide soft halo · the sphere body (radial accent →
 * violet → magenta gradient with a top-left highlight for a 3D read) · two
 * slow-drifting inner plasma blooms · a bright specular hotspot. When listening
 * the orb tightens and flares. All motion honours [ThemeStore.animations] so the
 * global reduced-motion switch freezes it in a calm pose. Colours read the
 * state-backed [VisionColors] so theme + accent recolour the orb live.
 */
@Composable
fun VisionOrb(listening: Boolean, modifier: Modifier = Modifier) {
    val animate = ThemeStore.animations
    val inf = rememberInfiniteTransition(label = "orb")
    val breathA by inf.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breath")
    val glowA by inf.animateFloat(0.45f, 0.85f, infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val driftA by inf.animateFloat(0f, (2f * Math.PI).toFloat(), infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "drift")
    val breath = if (animate) breathA else 1f
    val glow = if (animate) glowA else 0.65f
    val drift = if (animate) driftA else 0.6f

    val accent = VisionColors.CyanPrimary
    val violet = VisionColors.Violet
    val magenta = VisionColors.Magenta

    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val base = minOf(cx, cy)
        // Listening tightens the sphere slightly and brightens the halo.
        val r = base * (if (listening) 0.66f else 0.72f) * breath
        val haloAlpha = (if (listening) 0.5f else 0.32f) * glow

        // 1. Soft outer halo
        drawCircle(
            Brush.radialGradient(
                listOf(accent.copy(alpha = haloAlpha), violet.copy(alpha = haloAlpha * 0.6f), Color.Transparent),
                Offset(cx, cy), base * 0.98f,
            ),
            base * 0.98f, Offset(cx, cy),
        )

        // 2. Sphere body — offset radial gradient gives a lit-from-upper-left 3D look
        val lightCenter = Offset(cx - r * 0.28f, cy - r * 0.30f)
        drawCircle(
            Brush.radialGradient(
                0f to accent,
                0.45f to violet,
                0.85f to magenta,
                1f to magenta.copy(alpha = 0.85f),
                center = lightCenter, radius = r * 1.35f,
            ),
            r, Offset(cx, cy),
        )

        // 3. Inner plasma blooms — two counter-drifting soft lobes
        repeat(2) { i ->
            val ang = drift + i * Math.PI.toFloat()
            val bx = cx + cos(ang) * r * 0.30f
            val by = cy + sin(ang) * r * 0.24f
            drawCircle(
                Brush.radialGradient(
                    listOf(accent.copy(alpha = 0.55f * glow), Color.Transparent),
                    Offset(bx, by), r * 0.55f,
                ),
                r * 0.55f, Offset(bx, by),
            )
        }

        // 4. Specular hotspot
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.65f * glow), Color.Transparent),
                lightCenter, r * 0.45f,
            ),
            r * 0.45f, lightCenter,
        )

        // 5. Rim light — a thin bright arc on the lower-right edge
        drawCircle(
            Brush.radialGradient(
                0.82f to Color.Transparent,
                0.97f to accent.copy(alpha = 0.4f * glow),
                1f to Color.Transparent,
                center = Offset(cx, cy), radius = r,
            ),
            r, Offset(cx, cy),
        )
    }
}
