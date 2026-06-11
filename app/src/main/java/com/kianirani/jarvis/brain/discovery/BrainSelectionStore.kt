package com.kianirani.jarvis.brain.discovery

import android.content.Context

/**
 * Persists the paired brain (spec: 2026-06-11-discovery-ux-design). Saved on a
 * successful wizard handshake; read by BrainLiteService to aim HeartbeatSender.
 */
interface BrainSelectionStore {
    fun save(p: JoinPayload)
    fun load(): JoinPayload?
    fun clear()
}

/** SharedPreferences-backed impl (decision: no new DataStore dependency needed for 3 keys). */
class PrefsBrainSelectionStore(context: Context) : BrainSelectionStore {
    private val prefs = context.getSharedPreferences("brain_lite", Context.MODE_PRIVATE)

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
