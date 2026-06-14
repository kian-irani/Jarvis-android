package com.kianirani.jarvis.ui.theme

import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/**
 * VISION identity palette (v16 design overhaul, 2026-06-12; theme engine v11,
 * 2026-06-14).
 *
 * The HUD look is a deep-void base, an accent "signal" colour and a plasma
 * violet/magenta secondary. As of v11 every colour here is **snapshot-state
 * backed** ([mutableStateOf]): the moment [ThemeStore] swaps a theme, accent or
 * wallpaper, every composable / draw pass that reads a [VisionColors] field
 * re-renders automatically — so all existing screens become theme-reactive
 * without being touched. [JarvisColors] is kept as a compatibility alias.
 */
object VisionColors {
    // Signal — accent (driven by ThemeStore accent + theme)
    var CyanPrimary   by mutableStateOf(Color(0xFF22F5FF))
    var CyanSecondary by mutableStateOf(Color(0xFF0FBFE0))
    var CyanFaint     by mutableStateOf(Color(0x1A22F5FF))
    var CyanGlow      by mutableStateOf(Color(0x4D22F5FF))

    // Plasma — electric violet / magenta secondary
    var Violet        by mutableStateOf(Color(0xFF7C4DFF))
    var VioletDeep    by mutableStateOf(Color(0xFF4A1FB8))
    var Magenta       by mutableStateOf(Color(0xFFB04DFF))
    var MagentaGlow   by mutableStateOf(Color(0x40B04DFF))

    // Structure — blues
    var BlueDeep      by mutableStateOf(Color(0xFF1565FF))
    var BlueAccent    by mutableStateOf(Color(0xFF142A7A))
    var BlueMid       by mutableStateOf(Color(0xFF0A1140))

    // Status (constant across themes — semantic)
    val NeonGreen     = Color(0xFF3DFFB0)
    val WarningAmber  = Color(0xFFFFC233)
    val DangerRed     = Color(0xFFFF3B6B)

    // Surfaces — set per theme
    var Background    by mutableStateOf(Color(0xFF03060F))
    var BackgroundAlt by mutableStateOf(Color(0xFF070B1C))
    var Surface       by mutableStateOf(Color(0xFF0A1024))
    var SurfaceGlass  by mutableStateOf(Color(0xCC0C1330))
    var Border        by mutableStateOf(Color(0x3322F5FF))
    var BorderViolet  by mutableStateOf(Color(0x335C4DFF))
    var GridLine      by mutableStateOf(Color(0x1422F5FF))

    // Text — set per theme
    var TextPrimary   by mutableStateOf(Color(0xFFEAF6FF))
    var TextSecondary by mutableStateOf(Color(0xFF8FD3E6))
    var TextTerminal  by mutableStateOf(Color(0xFF22F5FF))
    var TextDim       by mutableStateOf(Color(0xFF4A6B85))

    // Glass surface fill colours (set per theme — light themes need a near-opaque
    // light glass so cards read against a light backdrop).
    private var glassTop by mutableStateOf(Color(0xE60D1430))
    private var glassBot by mutableStateOf(Color(0xF2060A18))
    private var sheen    by mutableStateOf(Color(0x1AFFFFFF))

    // User-chosen wallpaper colour (null = theme gradient).
    var wallpaper: Color? by mutableStateOf(null)

    // Reusable gradients — getters so each read recomputes from current state. ---

    /** Full-screen ambient backdrop — wallpaper colour, else void plasma core. */
    val ScreenBackdrop: Brush
        get() = wallpaper?.let { w ->
            Brush.verticalGradient(listOf(w, w.darken(0.7f), w.darken(0.45f)))
        } ?: Brush.verticalGradient(0f to Background, 0.55f to BackgroundAlt, 1f to Background)

    /** Plasma sweep used for orbs, primary buttons and active rails. */
    val PlasmaSweep: Brush
        get() = Brush.linearGradient(listOf(CyanPrimary, Violet, Magenta))

    /** Glass panel fill — translucent layered depth (theme aware). */
    val GlassPanel: Brush
        get() = Brush.verticalGradient(listOf(glassTop, glassBot))

    /** Subtle inner highlight for raised glass surfaces. */
    val GlassSheen: Brush
        get() = Brush.verticalGradient(listOf(sheen, Color(0x00FFFFFF)))

    // ---------------------------------------------------------------------------
    // Theme application. Called by [ThemeStore]; mutating these states triggers
    // recomposition everywhere they're read.
    // ---------------------------------------------------------------------------

    internal fun applyBase(p: Palette) {
        Violet = p.violet; VioletDeep = p.violetDeep; Magenta = p.magenta
        MagentaGlow = p.magenta.copy(alpha = 0.25f)
        BlueDeep = p.blueDeep; BlueAccent = p.blueAccent; BlueMid = p.blueMid
        Background = p.background; BackgroundAlt = p.backgroundAlt
        Surface = p.surface; SurfaceGlass = p.surfaceGlass
        BorderViolet = p.violet.copy(alpha = 0.20f)
        TextPrimary = p.textPrimary; TextSecondary = p.textSecondary; TextDim = p.textDim
        glassTop = p.glassTop; glassBot = p.glassBot; sheen = p.sheen
        // accent applied separately so a custom accent survives a theme swap
    }

    internal fun applyAccent(accent: Color) {
        CyanPrimary = accent
        CyanSecondary = accent.darken(0.82f)
        CyanFaint = accent.copy(alpha = 0.10f)
        CyanGlow = accent.copy(alpha = 0.30f)
        TextTerminal = accent
        Border = accent.copy(alpha = 0.20f)
        GridLine = accent.copy(alpha = 0.08f)
    }
}

/** Multiply RGB by [f] (0..1) to darken a colour while keeping its hue/alpha. */
fun Color.darken(f: Float): Color = copy(red = red * f, green = green * f, blue = blue * f)

/** Backwards-compatible alias — legacy screens reference [JarvisColors]. */
typealias JarvisColors = VisionColors

/** A full surface/text palette for one theme (accent is chosen independently). */
data class Palette(
    val background: Color,
    val backgroundAlt: Color,
    val surface: Color,
    val surfaceGlass: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDim: Color,
    val violet: Color,
    val violetDeep: Color,
    val magenta: Color,
    val blueDeep: Color,
    val blueAccent: Color,
    val blueMid: Color,
    val glassTop: Color,
    val glassBot: Color,
    val sheen: Color,
    val defaultAccent: Color,
    val isLight: Boolean,
)

/** The three shipped themes (USER DIRECTIVE 2026-06-14: full theme switcher). */
object VisionThemes {
    const val DEEP_SPACE = 0
    const val AURORA_DARK = 1
    const val LIGHT_FUTURE = 2

    val names = listOf("DEEP SPACE", "AURORA DARK", "LIGHT FUTURE")

    /** Default void — cyan signal on near-black with a violet undertone. */
    val deepSpace = Palette(
        background = Color(0xFF03060F), backgroundAlt = Color(0xFF070B1C),
        surface = Color(0xFF0A1024), surfaceGlass = Color(0xCC0C1330),
        textPrimary = Color(0xFFEAF6FF), textSecondary = Color(0xFF8FD3E6), textDim = Color(0xFF4A6B85),
        violet = Color(0xFF7C4DFF), violetDeep = Color(0xFF4A1FB8), magenta = Color(0xFFB04DFF),
        blueDeep = Color(0xFF1565FF), blueAccent = Color(0xFF142A7A), blueMid = Color(0xFF0A1140),
        glassTop = Color(0xE60D1430), glassBot = Color(0xF2060A18), sheen = Color(0x1AFFFFFF),
        defaultAccent = Color(0xFF22F5FF), isLight = false,
    )

    /** Purple nebula — magenta signal on a deep violet-black. */
    val auroraDark = Palette(
        background = Color(0xFF0B0518), backgroundAlt = Color(0xFF150A30),
        surface = Color(0xFF1A0F38), surfaceGlass = Color(0xCC1C123E),
        textPrimary = Color(0xFFF4ECFF), textSecondary = Color(0xFFC9B3E6), textDim = Color(0xFF7A6699),
        violet = Color(0xFF9D6BFF), violetDeep = Color(0xFF5A23C8), magenta = Color(0xFFE05BFF),
        blueDeep = Color(0xFF6A3DFF), blueAccent = Color(0xFF2E1A6E), blueMid = Color(0xFF160A38),
        glassTop = Color(0xE61C1142), glassBot = Color(0xF20E0626), sheen = Color(0x1AFFFFFF),
        defaultAccent = Color(0xFFC77BFF), isLight = false,
    )

    /** Daylight HUD — dark ink on light surfaces; accent darkened for AA contrast. */
    val lightFuture = Palette(
        background = Color(0xFFECF1F8), backgroundAlt = Color(0xFFE0E7F2),
        surface = Color(0xFFFFFFFF), surfaceGlass = Color(0xF2FFFFFF),
        textPrimary = Color(0xFF0B1530), textSecondary = Color(0xFF2E4668), textDim = Color(0xFF5B6B85),
        violet = Color(0xFF6D28D9), violetDeep = Color(0xFF4C1D95), magenta = Color(0xFFB5179E),
        blueDeep = Color(0xFF1D4ED8), blueAccent = Color(0xFFBFD2F0), blueMid = Color(0xFFD7E2F5),
        glassTop = Color(0xF2FFFFFF), glassBot = Color(0xF2EEF3FA), sheen = Color(0x14002040),
        defaultAccent = Color(0xFF0E8FA8), isLight = true,
    )

    fun palette(id: Int): Palette = when (id) {
        AURORA_DARK -> auroraDark
        LIGHT_FUTURE -> lightFuture
        else -> deepSpace
    }
}

/**
 * Persists + applies appearance (theme / accent / wallpaper / animations /
 * brain badge). Plain singleton (not Hilt) so [JarvisTheme] and [VisionColors]
 * can read it without a ViewModel. Call [init] once from MainActivity before
 * setContent. All setters persist and apply immediately.
 */
object ThemeStore {
    private const val PREFS = "vision_theme"
    private const val KEY_THEME = "theme_id"
    private const val KEY_ACCENT = "accent_argb" // Long; NONE = follow theme
    private const val KEY_WALLPAPER = "wallpaper_argb" // Long; NONE = none
    private const val KEY_ANIM = "animations"
    private const val KEY_BADGE = "brain_badge"
    private const val NONE = Long.MIN_VALUE

    private var prefs: android.content.SharedPreferences? = null

    var themeId by mutableStateOf(VisionThemes.DEEP_SPACE)
        private set

    /** null = use the theme's default accent. */
    var accent: Color? by mutableStateOf(null)
        private set

    var animations by mutableStateOf(true)
        private set

    var showBrainBadge by mutableStateOf(true)
        private set

    var wallpaper: Color? by mutableStateOf(null)
        private set

    /** Exposed so [JarvisTheme] can build the right Material colour scheme. */
    var isLight by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        themeId = p.getInt(KEY_THEME, VisionThemes.DEEP_SPACE)
        accent = p.getLong(KEY_ACCENT, NONE).let { if (it == NONE) null else Color(it.toULong()) }
        wallpaper = p.getLong(KEY_WALLPAPER, NONE).let { if (it == NONE) null else Color(it.toULong()) }
        animations = p.getBoolean(KEY_ANIM, true)
        showBrainBadge = p.getBoolean(KEY_BADGE, true)
        apply()
    }

    fun setTheme(id: Int) {
        themeId = id.coerceIn(0, 2)
        prefs?.edit()?.putInt(KEY_THEME, themeId)?.apply()
        apply()
    }

    /** Pass null to fall back to the theme's default accent. */
    fun chooseAccent(color: Color?) {
        accent = color
        prefs?.edit()?.putLong(KEY_ACCENT, color?.value?.toLong() ?: NONE)?.apply()
        apply()
    }

    /** Pass null for the theme gradient. */
    fun chooseWallpaper(color: Color?) {
        wallpaper = color
        prefs?.edit()?.putLong(KEY_WALLPAPER, color?.value?.toLong() ?: NONE)?.apply()
        VisionColors.wallpaper = color
    }

    fun enableAnimations(on: Boolean) {
        animations = on
        prefs?.edit()?.putBoolean(KEY_ANIM, on)?.apply()
    }

    fun setBrainBadge(on: Boolean) {
        showBrainBadge = on
        prefs?.edit()?.putBoolean(KEY_BADGE, on)?.apply()
    }

    /** Reset all appearance settings to defaults (Reset Appearance). */
    fun reset() {
        setTheme(VisionThemes.DEEP_SPACE)
        chooseAccent(null)
        chooseWallpaper(null)
        enableAnimations(true)
        setBrainBadge(true)
    }

    private fun apply() {
        val palette = VisionThemes.palette(themeId)
        VisionColors.applyBase(palette)
        VisionColors.applyAccent(accent ?: palette.defaultAccent)
        VisionColors.wallpaper = wallpaper
        isLight = palette.isLight
    }
}

// v12 reskin (2026-06-14): soft sans for prose/headings (matches the orb-launcher
// reference); monospace kept for label* + display roles that carry the HUD's
// technical/data identity (clock, stat numbers, chips). Colours come from the
// state-backed [VisionColors] so themes/accent still recolour text.
private val Sans = FontFamily.SansSerif
private val Mono = FontFamily.Monospace
val JarvisTypography = Typography(
    displayLarge   = TextStyle(fontFamily=Mono, fontSize=32.sp, letterSpacing=4.sp,   color=VisionColors.CyanPrimary),
    displaySmall   = TextStyle(fontFamily=Mono, fontSize=26.sp, letterSpacing=2.sp,   color=VisionColors.CyanPrimary),
    headlineLarge  = TextStyle(fontFamily=Sans, fontSize=22.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold, letterSpacing=0.sp, color=VisionColors.TextPrimary),
    headlineMedium = TextStyle(fontFamily=Sans, fontSize=16.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.Medium,   letterSpacing=0.sp, color=VisionColors.TextPrimary),
    titleLarge     = TextStyle(fontFamily=Sans, fontSize=20.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold, letterSpacing=0.5.sp, color=VisionColors.TextPrimary),
    titleMedium    = TextStyle(fontFamily=Sans, fontSize=16.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.Medium,   letterSpacing=0.5.sp, color=VisionColors.TextPrimary),
    bodyLarge      = TextStyle(fontFamily=Sans, fontSize=15.sp, letterSpacing=0.2.sp, color=VisionColors.TextPrimary,   lineHeight=22.sp),
    bodyMedium     = TextStyle(fontFamily=Sans, fontSize=13.sp, letterSpacing=0.2.sp, color=VisionColors.TextSecondary, lineHeight=19.sp),
    bodySmall      = TextStyle(fontFamily=Sans, fontSize=11.sp, letterSpacing=0.1.sp, color=VisionColors.TextDim,       lineHeight=16.sp),
    labelLarge     = TextStyle(fontFamily=Mono, fontSize=11.sp, letterSpacing=2.sp,   color=VisionColors.TextTerminal),
    labelMedium    = TextStyle(fontFamily=Mono, fontSize=10.sp, letterSpacing=2.sp,   color=VisionColors.TextSecondary),
    labelSmall     = TextStyle(fontFamily=Mono, fontSize=9.sp,  letterSpacing=2.5.sp, color=VisionColors.TextDim),
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    // Rebuilt on every theme change because it reads VisionColors snapshot state.
    val scheme = if (ThemeStore.isLight) {
        lightColorScheme(
            primary = VisionColors.CyanPrimary, onPrimary = VisionColors.Surface,
            primaryContainer = VisionColors.BlueAccent, onPrimaryContainer = VisionColors.TextPrimary,
            secondary = VisionColors.Violet, onSecondary = androidx.compose.ui.graphics.Color.White,
            tertiary = VisionColors.NeonGreen, onTertiary = VisionColors.Background,
            background = VisionColors.Background, onBackground = VisionColors.TextPrimary,
            surface = VisionColors.Surface, onSurface = VisionColors.TextPrimary,
            outline = VisionColors.Border, outlineVariant = VisionColors.GridLine,
            error = VisionColors.DangerRed, onError = androidx.compose.ui.graphics.Color.White,
        )
    } else {
        darkColorScheme(
            primary = VisionColors.CyanPrimary, onPrimary = VisionColors.Background,
            primaryContainer = VisionColors.BlueAccent, onPrimaryContainer = VisionColors.CyanPrimary,
            secondary = VisionColors.Violet, onSecondary = VisionColors.Background,
            tertiary = VisionColors.NeonGreen, onTertiary = VisionColors.Background,
            background = VisionColors.Background, onBackground = VisionColors.TextPrimary,
            surface = VisionColors.Surface, onSurface = VisionColors.TextPrimary,
            outline = VisionColors.Border, outlineVariant = VisionColors.GridLine,
            error = VisionColors.DangerRed, onError = VisionColors.Background,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = JarvisTypography, content = content)
}
