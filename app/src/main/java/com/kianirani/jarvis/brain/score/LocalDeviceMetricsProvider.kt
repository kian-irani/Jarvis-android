package com.kianirani.jarvis.brain.score

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager

/** Supplies this device's [DeviceMetrics] for Brain election. */
fun interface LocalDeviceMetricsProvider {
    fun current(): DeviceMetrics
}

/** Real readings from ActivityManager / BatteryManager / PowerManager. */
class AndroidDeviceMetricsProvider(private val context: Context) : LocalDeviceMetricsProvider {
    override fun current(): DeviceMetrics {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also(am::getMemoryInfo)
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return DeviceMetrics(
            ramFreeGb = mem.availMem / (1024.0 * 1024.0 * 1024.0),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            batteryPercent = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100),
            isOnBattery = !bm.isCharging,
            thermalThrottling = pm.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE,
        )
    }
}
