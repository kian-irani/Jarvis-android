package com.kianirani.jarvis.data.plugin

import android.content.Context
import android.os.Build
import android.text.format.Formatter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * P12 (Plugin Ecosystem) v1 — in-process plugin contract. A plugin inspects a
 * message and either handles it (returning the reply) or passes (null).
 * External/MCP plugin loading builds on this same contract later.
 */
interface VisionPlugin {
    val id: String
    val description: String
    fun handle(message: String): String?
}

/** Built-in: answers device/system questions locally. */
class SystemInfoPlugin(private val context: Context) : VisionPlugin {
    override val id = "system-info"
    override val description = "device model, Android version, memory"

    override fun handle(message: String): String? {
        val m = message.trim().lowercase()
        if (m in setOf("device", "device info", "system info", "مشخصات گوشی", "اطلاعات دستگاه")) {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val mi = android.app.ActivityManager.MemoryInfo().also(am::getMemoryInfo)
            val free = Formatter.formatShortFileSize(context, mi.availMem)
            val total = Formatter.formatShortFileSize(context, mi.totalMem)
            return "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} · RAM $free free of $total."
        }
        return null
    }
}

@Singleton
class PluginRegistry @Inject constructor(@ApplicationContext context: Context) {
    private val plugins: List<VisionPlugin> = listOf(SystemInfoPlugin(context))

    fun all(): List<VisionPlugin> = plugins

    /** First plugin that claims the message wins. */
    fun dispatch(message: String): String? =
        plugins.firstNotNullOfOrNull { it.handle(message) }
}
