package com.kianirani.jarvis.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import android.view.accessibility.AccessibilityEvent
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
