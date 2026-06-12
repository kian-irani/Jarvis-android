package com.kianirani.jarvis.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * General Vision settings (USER DIRECTIVE 2026-06-12: organized, fully
 * configurable settings sections). Plain prefs — nothing here is secret.
 */
@Singleton
class VisionSettings @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("vision_settings", Context.MODE_PRIVATE)

    private fun flow(key: String, default: Boolean) = MutableStateFlow(prefs.getBoolean(key, default))

    private val _voiceEnabled = flow(KEY_VOICE, true)
    private val _ttsEnabled = flow(KEY_TTS, true)
    private val _scanLine = flow(KEY_SCANLINE, true)
    private val _aurora = flow(KEY_AURORA, true)

    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled
    val scanLine: StateFlow<Boolean> = _scanLine
    val aurora: StateFlow<Boolean> = _aurora

    // P7 voice persona: TTS delivery style.
    private val _speechRate = MutableStateFlow(prefs.getFloat(KEY_RATE, 1.0f))
    private val _voicePitch = MutableStateFlow(prefs.getFloat(KEY_PITCH, 0.9f))
    val speechRate: StateFlow<Float> = _speechRate
    val voicePitch: StateFlow<Float> = _voicePitch

    fun setSpeechRate(v: Float) {
        val r = v.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_RATE, r).apply(); _speechRate.value = r
    }

    fun setVoicePitch(v: Float) {
        val r = v.coerceIn(0.5f, 1.5f)
        prefs.edit().putFloat(KEY_PITCH, r).apply(); _voicePitch.value = r
    }

    // P7 persona: identity + answer-style sliders (0..10) the cloud system
    // prompt is built from. Kept here so both TTS and CloudChatRouter read one source.
    private val _personaName = MutableStateFlow(prefs.getString(KEY_PERSONA, "VISION") ?: "VISION")
    private val _humor = MutableStateFlow(prefs.getFloat(KEY_HUMOR, 5f))
    private val _formality = MutableStateFlow(prefs.getFloat(KEY_FORMALITY, 5f))
    private val _responseLength = MutableStateFlow(prefs.getFloat(KEY_RESP_LEN, 5f))
    val personaName: StateFlow<String> = _personaName
    val humorLevel: StateFlow<Float> = _humor
    val formalityLevel: StateFlow<Float> = _formality
    val responseLength: StateFlow<Float> = _responseLength

    fun setPersonaName(v: String) {
        val n = v.take(24)
        prefs.edit().putString(KEY_PERSONA, n).apply(); _personaName.value = n
    }
    // Language: "auto" follows the device + the language the user types/speaks;
    // "fa"/"en" pin replies. Read by CloudChatRouter (prompt) and VoiceController (TTS/STT).
    private val _language = MutableStateFlow(prefs.getString(KEY_LANG, LANG_AUTO) ?: LANG_AUTO)
    val language: StateFlow<String> = _language
    fun setLanguage(v: String) {
        val l = if (v in setOf(LANG_AUTO, LANG_FA, LANG_EN)) v else LANG_AUTO
        prefs.edit().putString(KEY_LANG, l).apply(); _language.value = l
    }

    fun setHumorLevel(v: Float) { val r = v.coerceIn(0f, 10f); prefs.edit().putFloat(KEY_HUMOR, r).apply(); _humor.value = r }
    fun setFormalityLevel(v: Float) { val r = v.coerceIn(0f, 10f); prefs.edit().putFloat(KEY_FORMALITY, r).apply(); _formality.value = r }
    fun setResponseLength(v: Float) { val r = v.coerceIn(0f, 10f); prefs.edit().putFloat(KEY_RESP_LEN, r).apply(); _responseLength.value = r }

    /** P4.5 Trust Level: 0 = SOVEREIGN (local only, no cloud), 1 = BALANCED, 2 = OPEN. */
    private val _trustLevel = MutableStateFlow(prefs.getInt(KEY_TRUST, 1))
    val trustLevel: StateFlow<Int> = _trustLevel

    fun setTrustLevel(level: Int) {
        val l = level.coerceIn(0, 2)
        prefs.edit().putInt(KEY_TRUST, l).apply()
        _trustLevel.value = l
    }

    fun set(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        when (key) {
            KEY_VOICE -> _voiceEnabled.value = value
            KEY_TTS -> _ttsEnabled.value = value
            KEY_SCANLINE -> _scanLine.value = value
            KEY_AURORA -> _aurora.value = value
        }
    }

    companion object {
        const val KEY_VOICE = "voice_enabled"
        const val KEY_TTS = "tts_enabled"
        const val KEY_SCANLINE = "fx_scanline"
        const val KEY_AURORA = "fx_aurora"
        const val KEY_TRUST = "trust_level"
        const val KEY_RATE = "voice_rate"
        const val KEY_PITCH = "voice_pitch"
        const val KEY_PERSONA = "persona_name"
        const val KEY_HUMOR = "persona_humor"
        const val KEY_FORMALITY = "persona_formality"
        const val KEY_RESP_LEN = "persona_resp_len"
        const val KEY_LANG = "reply_language"
        const val LANG_AUTO = "auto"
        const val LANG_FA = "fa"
        const val LANG_EN = "en"
    }
}
