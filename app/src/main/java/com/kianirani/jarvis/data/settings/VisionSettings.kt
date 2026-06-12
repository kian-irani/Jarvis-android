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
    }
}
