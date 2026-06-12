package com.kianirani.jarvis.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * VISION motion + glass vocabulary (shared by every screen).
 *
 * - [visionEnter] — spring-based fade + slide-up + scale entrance, driven by
 *   [graphicsLayer] so it animates on the render thread (no recomposition loop).
 * - [glassPanel] — layered translucent glass with a glow border + corner sheen.
 *
 * 60fps note: entrance uses a single [animateFloatAsState], glow is drawn in
 * [drawBehind] (no extra layout passes).
 */

/**
 * Staggered spring entrance. Place on any composable; pass an increasing
 * [index] for list/grid stagger. Animates from below + slightly scaled-down +
 * transparent into resting position.
 */
@Composable
fun Modifier.visionEnter(index: Int = 0): Modifier {
    var shown by remember { mutableStateOf(false) }
    val p by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow,
            visibilityThreshold = 0.001f,
        ),
        label = "visionEnter",
    )
    // Kick off on first composition; stagger by index.
    LaunchedEffect(index) { shown = true }
    return this.graphicsLayer {
        alpha = p
        translationY = (1f - p) * 42f
        val s = 0.92f + 0.08f * p
        scaleX = s
        scaleY = s
    }
}

/** Soft outer glow + glass fill + thin accent border with corner ticks. */
fun Modifier.glassPanel(
    radius: Dp = 14.dp,
    glow: Color = VisionColors.CyanGlow,
    border: Color = VisionColors.Border,
): Modifier = this
    .drawBehind {
        // ambient glow halo
        drawRoundRect(
            color = glow,
            topLeft = Offset(-6f, -6f),
            size = androidx.compose.ui.geometry.Size(size.width + 12f, size.height + 12f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx() + 6f),
            alpha = 0.35f,
        )
    }
    .clip(RoundedCornerShape(radius))
    .background(VisionColors.GlassPanel)
    .border(1.dp, border, RoundedCornerShape(radius))
    .drawBehind {
        // top sheen highlight
        drawRoundRect(
            brush = VisionColors.GlassSheen,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius.toPx()),
            size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.5f),
        )
        // corner ticks
        val s = 10.dp.toPx(); val p = 6.dp.toPx(); val w = 1.4.dp.toPx()
        val c = VisionColors.CyanSecondary.copy(alpha = 0.55f)
        val W = size.width; val H = size.height
        listOf(
            Triple(Offset(p, p), Offset(p + s, p), Offset(p, p + s)),
            Triple(Offset(W - p, p), Offset(W - p - s, p), Offset(W - p, p + s)),
            Triple(Offset(p, H - p), Offset(p + s, H - p), Offset(p, H - p - s)),
            Triple(Offset(W - p, H - p), Offset(W - p - s, H - p), Offset(W - p, H - p - s)),
        ).forEach { (o, h, v) ->
            drawLine(c, o, h, w); drawLine(c, o, v, w)
        }
    }
