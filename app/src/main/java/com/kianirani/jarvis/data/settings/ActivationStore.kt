package com.kianirani.jarvis.data.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P4 (Activation & Licensing) v1 — stores the activation code issued by the
 * Vision Telegram bot. Codes are recorded locally now; online verification
 * against the licensing service arrives with the website model.
 */
@Singleton
class ActivationStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("vision_activation", Context.MODE_PRIVATE)

    private val _code = MutableStateFlow(prefs.getString(KEY_CODE, null))
    val code: StateFlow<String?> = _code

    fun activate(code: String) {
        val c = code.trim()
        if (c.isEmpty()) return
        prefs.edit().putString(KEY_CODE, c).apply()
        _code.value = c
    }

    fun deactivate() {
        prefs.edit().remove(KEY_CODE).apply()
        _code.value = null
    }

    companion object { private const val KEY_CODE = "activation_code" }
}
