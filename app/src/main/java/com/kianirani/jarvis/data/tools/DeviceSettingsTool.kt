package com.kianirani.jarvis.data.tools

import android.content.Context
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens the relevant system settings panel. Modern Android forbids silently
 * toggling Wi-Fi/Bluetooth from a normal app, so the agent takes the user
 * straight to the right screen — one tap instead of digging through menus.
 */
@Singleton
class DeviceSettingsTool @Inject constructor(
    @ApplicationContext private val context: Context,
) : Tool {
    override val id = "device-settings"

    override fun matches(message: String): Boolean = panelFor(message) != null

    override fun run(message: String): ToolResult {
        val (action, label) = panelFor(message) ?: return ToolResult("Unknown setting.")
        return ToolResult(
            runCatching {
                context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Opening $label settings."
            }.getOrElse { "Couldn't open $label settings." },
        )
    }

    private fun panelFor(m: String): Pair<String, String>? = when {
        m.contains("wifi") || m.contains("wi-fi") || m.contains("وای فای") || m.contains("وایفای") ->
            Settings.ACTION_WIFI_SETTINGS to "Wi-Fi"
        m.contains("bluetooth") || m.contains("بلوتوث") ->
            Settings.ACTION_BLUETOOTH_SETTINGS to "Bluetooth"
        m.contains("brightness") || m.contains("روشنایی") ->
            Settings.ACTION_DISPLAY_SETTINGS to "Display"
        m.contains("airplane") || m.contains("هواپیما") || m.contains("پرواز") ->
            Settings.ACTION_AIRPLANE_MODE_SETTINGS to "Airplane mode"
        m.contains("mobile data") || m.contains("دیتا") || m.contains("اینترنت همراه") ->
            Settings.ACTION_DATA_ROAMING_SETTINGS to "Mobile data"
        else -> null
    }
}
