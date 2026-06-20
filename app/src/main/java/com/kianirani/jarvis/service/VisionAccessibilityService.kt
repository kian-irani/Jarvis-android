package com.kianirani.jarvis.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // DS-W5 — track the foreground app package so the ContextEngine can ground answers in
        // "the user is in <app>". Only window-state changes; nothing is stored beyond the name.
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.takeIf { it.isNotBlank() }?.let { foregroundPackage = it }
        }
    }

    override fun onInterrupt() { /* no-op */ }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    fun goHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun openRecents(): Boolean = performGlobalAction(GLOBAL_ACTION_RECENTS)
    fun lockScreen(): Boolean = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)

    /**
     * App tool — close the current app: open Recents and (on API 28+) the launcher can swipe it
     * away. Simplest reliable cross-version behavior is Back-to-exit then Home; we expose both a
     * soft close (home) and recents so the agent can pick. Returns true if the action dispatched.
     */
    fun closeCurrentApp(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME) // soft close to launcher
    fun showQuickSettings(): Boolean = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    fun showNotificationsShade(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    /**
     * PAU — universal app automation. Find the first clickable node whose text/description matches
     * [text] (case-insensitive, normalized) and click it. Returns true if something was clicked.
     */
    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = AccessibilityMatch.normalize(text)
        val node = findNode(root) { n ->
            val label = AccessibilityMatch.label(n.text?.toString(), n.contentDescription?.toString())
            n.isClickable && AccessibilityMatch.matches(label, target)
        } ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /** PAU — scroll the first scrollable container forward or backward. */
    fun scroll(forward: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNode(root) { it.isScrollable } ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return node.performAction(action)
    }

    /**
     * PAW — set the text of the currently focused editable field (the real "rewrite and replace"
     * write-back). Returns true if a focused editable node accepted the new [value].
     */
    fun setFocusedText(value: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findNode(root) { it.isEditable && it.isFocused }
            ?: return false
        val args = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    /** The text of the currently focused editable field, or null (read for PAW improve). */
    fun focusedText(): String? {
        val root = rootInActiveWindow ?: return null
        val node = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        return node.text?.toString()
    }

    /** Depth-first search for the first node satisfying [predicate]. */
    private fun findNode(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        node ?: return null
        if (runCatching { predicate(node) }.getOrDefault(false)) return node
        for (i in 0 until node.childCount) {
            findNode(node.getChild(i), predicate)?.let { return it }
        }
        return null
    }

    /**
     * VCF-M2 — capture the current screen as PNG bytes via the Accessibility screenshot API
     * (Android 11+). Returns null if unavailable or it fails. Used by `VisualPerception` to feed
     * the screen to a vision model. Requires `canTakeScreenshot` in the service config.
     */
    suspend fun captureScreenshot(): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return suspendCancellableCoroutine { cont ->
            runCatching {
                takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val bytes = runCatching {
                            val bmp = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                            screenshot.hardwareBuffer.close()
                            bmp?.let {
                                val out = ByteArrayOutputStream()
                                it.compress(Bitmap.CompressFormat.PNG, 90, out)
                                out.toByteArray()
                            }
                        }.getOrNull()
                        if (cont.isActive) cont.resume(bytes)
                    }

                    override fun onFailure(errorCode: Int) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }
    }

    companion object {
        /** Set while the service is connected; null when disabled/not bound. */
        @Volatile
        var instance: VisionAccessibilityService? = null
            private set

        /** DS-W5 — last foreground app package seen, or null. Read by [DeviceContextProvider]. */
        @Volatile
        var foregroundPackage: String? = null
            private set

        val isEnabled: Boolean get() = instance != null
    }
}
