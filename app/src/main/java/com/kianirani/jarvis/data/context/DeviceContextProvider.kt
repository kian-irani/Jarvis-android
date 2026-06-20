package com.kianirani.jarvis.data.context

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import com.kianirani.jarvis.core.protocol.DeviceContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CTX (PRD §8.2) — fills a [DeviceContext] snapshot from the live device so the chat/agent can
 * ground its answers (battery, network, time-of-day, locale). Reads only what's free of extra
 * permissions: battery + time + locale need none, network uses the already-granted
 * ACCESS_NETWORK_STATE. The foreground app (needs Accessibility, opt-in) is left null here and
 * filled by the Accessibility service when enabled. Best-effort — every read is guarded so it
 * never crashes the chat path. [ContextEngine][com.kianirani.jarvis.core.context.ContextEngine]
 * turns the snapshot into the prompt block.
 */
@Singleton
class DeviceContextProvider @Inject constructor(@ApplicationContext private val context: Context) {

    /** A best-effort snapshot of the current device state. */
    fun snapshot(): DeviceContext = DeviceContext(
        // DS-W5 — the foreground app (Accessibility, opt-in; null when the service is off).
        foregroundApp = com.kianirani.jarvis.service.VisionAccessibilityService.foregroundPackage,
        batteryPercent = battery(),
        charging = charging(),
        network = network(),
        locale = Locale.getDefault().toLanguageTag(),
        timeOfDay = timeOfDay(),
    )

    private fun battery(): Int? = runCatching {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
    }.getOrNull()

    private fun charging(): Boolean? = runCatching {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING, BatteryManager.BATTERY_STATUS_FULL -> true
            BatteryManager.BATTERY_STATUS_DISCHARGING, BatteryManager.BATTERY_STATUS_NOT_CHARGING -> false
            else -> null
        }
    }.getOrNull()

    private fun network(): String? = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "offline"
        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "online"
        }
    }.getOrNull()

    private fun timeOfDay(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..20 -> "evening"
        else -> "night"
    }
}
