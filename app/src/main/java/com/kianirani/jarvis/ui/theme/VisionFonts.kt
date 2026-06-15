package com.kianirani.jarvis.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.kianirani.jarvis.R
import java.util.Locale

/**
 * FNT — Vision typography system (2026-06-15).
 *
 * All typefaces are **bundled** in `res/font/` (offline-first — the APK never
 * has to reach the network to render text). Most are variable fonts, so a
 * single `.ttf` covers every weight via [FontVariation]; Space Mono ships as
 * two static weights (it has no variable axis).
 *
 * Like [VisionColors], the active families are **snapshot-state backed**
 * ([mutableStateOf]) on [VisionFonts]: the moment [FontStore] swaps the chosen
 * font, every composable that reads the typography re-renders — so the whole
 * UI changes typeface live, with no screen touched. [JarvisTheme] rebuilds the
 * Material [androidx.compose.material3.Typography] from these states.
 */

/** Build a [FontFamily] from one variable `.ttf`, mapping weights to the wght axis. */
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int): FontFamily = FontFamily(
    Font(resId, weight = FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(resId, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(resId, weight = FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(resId, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(resId, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** The bundled families (lazy — Compose resolves them with the local context). */
object VisionFontFamilies {
    val SpaceGrotesk: FontFamily by lazy { variable(R.font.space_grotesk) }
    val Inter: FontFamily by lazy { variable(R.font.inter) }
    val DmSans: FontFamily by lazy { variable(R.font.dm_sans) }
    val Exo2: FontFamily by lazy { variable(R.font.exo_2) }
    val Vazirmatn: FontFamily by lazy { variable(R.font.vazirmatn) }
    val SpaceMono: FontFamily by lazy {
        FontFamily(
            Font(R.font.space_mono_regular, FontWeight.Normal),
            Font(R.font.space_mono_bold, FontWeight.Bold),
        )
    }
}

/**
 * Pure (Android-free) catalog of the selectable font packs and the resolution
 * rules. Kept separate from [FontStore] so the logic — id validity and the
 * auto-Persian default — is unit-testable on the JVM.
 *
 * A pack chooses the *display* + *body* family; the monospace family (Space
 * Mono) is constant because it carries the HUD's technical/data identity
 * (clock, stat numbers, chips) regardless of the prose typeface.
 */
object FontCatalog {
    const val AUTO = 0 // Space Grotesk + Inter, but Vazirmatn when the UI language is Persian
    const val SPACE_GROTESK = 1
    const val INTER = 2
    const val DM_SANS = 3
    const val EXO_2 = 4
    const val VAZIRMATN = 5

    /** Order shown in the picker. */
    val ids = listOf(AUTO, SPACE_GROTESK, INTER, DM_SANS, EXO_2, VAZIRMATN)

    fun name(id: Int): String = when (id) {
        SPACE_GROTESK -> "Space Grotesk"
        INTER -> "Inter"
        DM_SANS -> "DM Sans"
        EXO_2 -> "Exo 2"
        VAZIRMATN -> "Vazirmatn"
        else -> "Auto"
    }

    /** Clamp an arbitrary stored value to a known id (defaults to [AUTO]). */
    fun coerce(id: Int): Int = if (id in ids) id else AUTO

    /**
     * Resolve the chosen [id] to the effective pack id. [AUTO] becomes
     * [VAZIRMATN] when the UI language is Persian (Inter covers Persian poorly),
     * otherwise [SPACE_GROTESK]. An explicit choice is always honoured.
     */
    fun resolve(id: Int, persian: Boolean): Int = when {
        coerce(id) != AUTO -> coerce(id)
        persian -> VAZIRMATN
        else -> SPACE_GROTESK
    }
}

/** State-backed active families. Reads here recompose when [FontStore] applies a change. */
object VisionFonts {
    var display: FontFamily by mutableStateOf(VisionFontFamilies.SpaceGrotesk)
        internal set
    var body: FontFamily by mutableStateOf(VisionFontFamilies.Inter)
        internal set

    /** HUD data/numbers — constant across packs. */
    val mono: FontFamily get() = VisionFontFamilies.SpaceMono
}

/**
 * Persists + applies the font choice. Plain singleton (not Hilt) so [JarvisTheme]
 * and [VisionFonts] can read it without a ViewModel — same shape as [ThemeStore].
 * Call [init] once from MainActivity before setContent.
 */
object FontStore {
    private const val PREFS = "vision_fonts"
    private const val KEY_FONT = "font_id"

    private var prefs: android.content.SharedPreferences? = null

    /** Whether the UI language is Persian — drives the [FontCatalog.AUTO] default. */
    private var persian = false

    var fontId by mutableStateOf(FontCatalog.AUTO)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        persian = Locale.getDefault().language == "fa"
        fontId = FontCatalog.coerce(p.getInt(KEY_FONT, FontCatalog.AUTO))
        apply()
    }

    fun setFont(id: Int) {
        fontId = FontCatalog.coerce(id)
        prefs?.edit()?.putInt(KEY_FONT, fontId)?.apply()
        apply()
    }

    /** Re-evaluate the auto-Persian rule when the reply/UI language changes. */
    fun setPersian(isPersian: Boolean) {
        if (persian == isPersian) return
        persian = isPersian
        apply()
    }

    /** Reset to the auto default. */
    fun reset() = setFont(FontCatalog.AUTO)

    private fun apply() {
        when (FontCatalog.resolve(fontId, persian)) {
            FontCatalog.INTER -> set(VisionFontFamilies.Inter, VisionFontFamilies.Inter)
            FontCatalog.DM_SANS -> set(VisionFontFamilies.DmSans, VisionFontFamilies.DmSans)
            FontCatalog.EXO_2 -> set(VisionFontFamilies.Exo2, VisionFontFamilies.Inter)
            FontCatalog.VAZIRMATN -> set(VisionFontFamilies.Vazirmatn, VisionFontFamilies.Vazirmatn)
            else -> set(VisionFontFamilies.SpaceGrotesk, VisionFontFamilies.Inter) // SPACE_GROTESK
        }
    }

    private fun set(display: FontFamily, body: FontFamily) {
        VisionFonts.display = display
        VisionFonts.body = body
    }
}
