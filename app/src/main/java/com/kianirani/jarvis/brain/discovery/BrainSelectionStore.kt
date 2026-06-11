package com.kianirani.jarvis.brain.discovery

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Persists the paired brain (spec: 2026-06-11-discovery-ux-design). Saved on a
 * successful wizard handshake; read by BrainLiteService to aim HeartbeatSender.
 */
interface BrainSelectionStore {
    fun save(p: JoinPayload)
    fun load(): JoinPayload?
    fun clear()
}

/**
 * Keystore-encrypted impl (review CRITICAL fix: pairing token must never sit in
 * plaintext prefs). Falls back to plain prefs only if the Keystore is broken,
 * so pairing keeps working on degraded devices.
 */
class PrefsBrainSelectionStore(context: Context) : BrainSelectionStore {
    private val prefs: SharedPreferences = runCatching {
        EncryptedSharedPreferences.create(
            context,
            "brain_lite_secure",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse { context.getSharedPreferences("brain_lite_secure", Context.MODE_PRIVATE) }

    override fun save(p: JoinPayload) {
        prefs.edit().putString("brain_host", p.host).putInt("brain_port", p.port).putString("brain_token", p.token).apply()
    }

    override fun load(): JoinPayload? {
        val host = prefs.getString("brain_host", null) ?: return null
        val token = prefs.getString("brain_token", null) ?: return null
        return JoinPayload(host, prefs.getInt("brain_port", 7799), token)
    }

    override fun clear() {
        prefs.edit().remove("brain_host").remove("brain_port").remove("brain_token").apply()
    }
}
