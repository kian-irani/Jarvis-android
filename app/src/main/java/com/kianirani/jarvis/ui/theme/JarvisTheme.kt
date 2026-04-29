package com.kianirani.jarvis.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

object JarvisColors {
    val CyanPrimary   = Color(0xFF00E5FF)
    val CyanSecondary = Color(0xFF00B0CC)
    val CyanFaint     = Color(0x1400E5FF)
    val CyanGlow      = Color(0x4000E5FF)
    val BlueDeep      = Color(0xFF0077FF)
    val BlueAccent    = Color(0xFF003899)
    val BlueMid       = Color(0xFF001240)
    val NeonGreen     = Color(0xFF00FF9D)
    val WarningAmber  = Color(0xFFFFAB00)
    val DangerRed     = Color(0xFFFF1744)
    val Background    = Color(0xFF020B18)
    val Surface       = Color(0xFF061525)
    val Border        = Color(0x3300E5FF)
    val GridLine      = Color(0x1200E5FF)
    val TextPrimary   = Color(0xFFE8F4FD)
    val TextSecondary = Color(0xFF7ABDD4)
    val TextTerminal  = Color(0xFF00E5FF)
    val TextDim       = Color(0xFF3D6B80)
}

val JarvisTypography = Typography(
    displayLarge   = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 32.sp, letterSpacing = 8.sp,   color = JarvisColors.CyanPrimary),
    headlineLarge  = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 18.sp, letterSpacing = 3.sp,   color = JarvisColors.CyanSecondary),
    headlineMedium = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, letterSpacing = 2.sp,   color = JarvisColors.CyanSecondary),
    bodyLarge      = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp, letterSpacing = 0.5.sp, color = JarvisColors.TextPrimary,   lineHeight = 22.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 0.3.sp, color = JarvisColors.TextSecondary, lineHeight = 18.sp),
    bodySmall      = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 0.sp,   color = JarvisColors.TextDim,       lineHeight = 15.sp),
    labelLarge     = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, letterSpacing = 2.sp,   color = JarvisColors.TextTerminal),
    labelMedium    = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, letterSpacing = 2.sp,   color = JarvisColors.TextSecondary),
    labelSmall     = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp,  letterSpacing = 2.5.sp, color = JarvisColors.TextDim),
    titleLarge     = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 20.sp, letterSpacing = 1.sp,   color = JarvisColors.CyanPrimary),
)

private val JarvisColorScheme = darkColorScheme(
    primary             = JarvisColors.CyanPrimary,
    onPrimary           = JarvisColors.Background,
    primaryContainer    = JarvisColors.BlueAccent,
    onPrimaryContainer  = JarvisColors.CyanPrimary,
    secondary           = JarvisColors.BlueDeep,
    onSecondary         = JarvisColors.Background,
    tertiary            = JarvisColors.NeonGreen,
    onTertiary          = JarvisColors.Background,
    background          = JarvisColors.Background,
    onBackground        = JarvisColors.TextPrimary,
    surface             = JarvisColors.Surface,
    onSurface           = JarvisColors.TextPrimary,
    outline             = JarvisColors.Border,
    outlineVariant      = JarvisColors.GridLine,
    error               = JarvisColors.DangerRed,
    onError             = JarvisColors.Background,
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = JarvisColorScheme, typography = JarvisTypography, content = content)
}
