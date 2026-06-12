package com.kianirani.jarvis.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Phase 7.5 — gives Vision real device control (Home / Back / Recents) via the
 * Accessibility API. The user must enable it once in Settings > Accessibility;
 * tools reach the live instance through [instance]. We keep no event state —
 * this is a command surface, not a screen reader.
 */
class VisionAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) { /* command-only */ }

    override fun onInterrupt() { /* no-op */ }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun lockScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

    companion object {
        /** Set while the service is connected; null when disabled/not bound. */
        @Volatile
        var instance: VisionAccessibilityService? = null
            private set

        val isEnabled: Boolean get() = instance != null
    }
}
