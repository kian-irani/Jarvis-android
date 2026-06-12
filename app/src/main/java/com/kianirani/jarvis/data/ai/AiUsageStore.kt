package com.kianirani.jarvis.data.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P2 R.4 (usage dashboard) + P13 (privacy monitor) v1 — counts every cloud
 * call per provider so the user can see exactly which services received data.
 */
data class ProviderUsage(val calls: Int, val ok: Int, val failed: Int, val lastUsed: Long)

@Singleton
class AiUsageStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("vision_ai_usage", Context.MODE_PRIVATE)

    fun record(p: AiProvider, success: Boolean) {
        prefs.edit()
            .putInt("${p.name}_calls", prefs.getInt("${p.name}_calls", 0) + 1)
            .putInt(if (success) "${p.name}_ok" else "${p.name}_fail",
                prefs.getInt(if (success) "${p.name}_ok" else "${p.name}_fail", 0) + 1)
            .putLong("${p.name}_last", System.currentTimeMillis())
            .apply()
    }

    fun usage(p: AiProvider) = ProviderUsage(
        calls = prefs.getInt("${p.name}_calls", 0),
        ok = prefs.getInt("${p.name}_ok", 0),
        failed = prefs.getInt("${p.name}_fail", 0),
        lastUsed = prefs.getLong("${p.name}_last", 0L),
    )

    fun reset() = prefs.edit().clear().apply()
}
