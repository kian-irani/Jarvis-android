package com.kianirani.jarvis.data.role

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default-assistant role (PRD §, "جایگزینی دستیار پیش‌فرض — Assistant role"). Lets Vision become
 * the device's default digital assistant so a long-press-home / assist gesture summons it. On
 * Android 10+ this is the `RoleManager.ROLE_ASSISTANT`; otherwise it falls back to the system
 * "Default apps ▸ Assist" settings screen. Best-effort, never crashes — the user grants it.
 */
@Singleton
class AssistantRole @Inject constructor(@ApplicationContext private val context: Context) {

    /** True if Vision currently holds the assistant role (Android 10+; false below). */
    fun isDefaultAssistant(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val rm = context.getSystemService(RoleManager::class.java) ?: return false
        return runCatching {
            rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT) && rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)
        }.getOrDefault(false)
    }

    /**
     * Intent to request the assistant role (Android 10+) or open the assist-settings screen as a
     * fallback. Caller launches it (with a result on 10+ to know if granted).
     */
    fun requestIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            val available = runCatching { rm?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true }.getOrDefault(false)
            if (available && rm != null) {
                return rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
            }
        }
        return Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
