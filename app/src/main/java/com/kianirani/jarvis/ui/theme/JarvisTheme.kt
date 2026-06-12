package com.kianirani.jarvis.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * VISION identity palette (v16 design overhaul, 2026-06-12).
 *
 * Evolved from the legacy single-cyan "Jarvis" look into a dual-accent
 * sovereign-AI HUD: a deep void base, cyan as the primary signal, and an
 * electric violet/magenta secondary that gives plasma-grade depth to glows,
 * orbs and gradient sweeps. [JarvisColors] is kept as a compatibility alias so
 * existing screens compile unchanged while migrating to [VisionColors].
 */
object VisionColors {
    // Signal — cyan
    val CyanPrimary   = Color(0xFF22F5FF)
    val CyanSecondary = Color(0xFF0FBFE0)
    val CyanFaint     = Color(0x1A22F5FF)
    val CyanGlow      = Color(0x4D22F5FF)

    // Plasma — electric violet / magenta (the new "Vision" accent)
    val Violet        = Color(0xFF7C4DFF)
    val VioletDeep    = Color(0xFF4A1FB8)
    val Magenta       = Color(0xFFB04DFF)
    val MagentaGlow   = Color(0x40B04DFF)

    // Structure — blues
    val BlueDeep      = Color(0xFF1565FF)
    val BlueAccent    = Color(0xFF142A7A)
    val BlueMid       = Color(0xFF0A1140)

    // Status
    val NeonGreen     = Color(0xFF3DFFB0)
    val WarningAmber  = Color(0xFFFFC233)
    val DangerRed     = Color(0xFFFF3B6B)

    // Surfaces — deep void with a violet undertone
    val Background    = Color(0xFF03060F)
    val BackgroundAlt = Color(0xFF070B1C)
    val Surface       = Color(0xFF0A1024)
    val SurfaceGlass  = Color(0xCC0C1330)
    val Border        = Color(0x3322F5FF)
    val BorderViolet  = Color(0x335C4DFF)
    val GridLine      = Color(0x1422F5FF)

    // Text
    val TextPrimary   = Color(0xFFEAF6FF)
    val TextSecondary = Color(0xFF8FD3E6)
    val TextTerminal  = Color(0xFF22F5FF)
    val TextDim       = Color(0xFF4A6B85)

    // Reusable gradients ------------------------------------------------------

    /** Full-screen ambient backdrop — void with a faint plasma core. */
    val ScreenBackdrop = Brush.verticalGradient(
        0f to Background, 0.55f to BackgroundAlt, 1f to Background,
    )

    /** Plasma sweep used for orbs, primary buttons and active rails. */
    val PlasmaSweep = Brush.linearGradient(listOf(CyanPrimary, Violet, Magenta))

    /** Glass panel fill — translucent layered depth. */
    val GlassPanel = Brush.verticalGradient(
        listOf(Color(0xE60D1430), Color(0xF2060A18)),
    )

    /** Subtle inner highlight for raised glass surfaces. */
    val GlassSheen = Brush.verticalGradient(
        listOf(Color(0x1AFFFFFF), Color(0x00FFFFFF)),
    )
}

/** Backwards-compatible alias — legacy screens reference [JarvisColors]. */
typealias JarvisColors = VisionColors

val JarvisTypography = Typography(
    displayLarge   = TextStyle(fontFamily=FontFamily.Monospace, fontSize=32.sp, letterSpacing=8.sp,   color=VisionColors.CyanPrimary),
    headlineLarge  = TextStyle(fontFamily=FontFamily.Monospace, fontSize=18.sp, letterSpacing=3.sp,   color=VisionColors.CyanSecondary),
    headlineMedium = TextStyle(fontFamily=FontFamily.Monospace, fontSize=14.sp, letterSpacing=2.sp,   color=VisionColors.CyanSecondary),
    bodyLarge      = TextStyle(fontFamily=FontFamily.Monospace, fontSize=14.sp, letterSpacing=0.5.sp, color=VisionColors.TextPrimary,   lineHeight=22.sp),
    bodyMedium     = TextStyle(fontFamily=FontFamily.Monospace, fontSize=12.sp, letterSpacing=0.3.sp, color=VisionColors.TextSecondary, lineHeight=18.sp),
    bodySmall      = TextStyle(fontFamily=FontFamily.Monospace, fontSize=10.sp, letterSpacing=0.sp,   color=VisionColors.TextDim,       lineHeight=15.sp),
    labelLarge     = TextStyle(fontFamily=FontFamily.Monospace, fontSize=11.sp, letterSpacing=2.sp,   color=VisionColors.TextTerminal),
    labelMedium    = TextStyle(fontFamily=FontFamily.Monospace, fontSize=10.sp, letterSpacing=2.sp,   color=VisionColors.TextSecondary),
    labelSmall     = TextStyle(fontFamily=FontFamily.Monospace, fontSize=9.sp,  letterSpacing=2.5.sp, color=VisionColors.TextDim),
    titleLarge     = TextStyle(fontFamily=FontFamily.Monospace, fontSize=20.sp, letterSpacing=1.sp,   color=VisionColors.CyanPrimary),
)

private val JarvisColorScheme = darkColorScheme(
    primary=VisionColors.CyanPrimary, onPrimary=VisionColors.Background,
    primaryContainer=VisionColors.BlueAccent, onPrimaryContainer=VisionColors.CyanPrimary,
    secondary=VisionColors.Violet, onSecondary=VisionColors.Background,
    tertiary=VisionColors.NeonGreen, onTertiary=VisionColors.Background,
    background=VisionColors.Background, onBackground=VisionColors.TextPrimary,
    surface=VisionColors.Surface, onSurface=VisionColors.TextPrimary,
    outline=VisionColors.Border, outlineVariant=VisionColors.GridLine,
    error=VisionColors.DangerRed, onError=VisionColors.Background,
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme=JarvisColorScheme, typography=JarvisTypography, content=content)
}
