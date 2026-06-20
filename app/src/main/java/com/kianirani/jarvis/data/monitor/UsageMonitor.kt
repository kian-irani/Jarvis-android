package com.kianirani.jarvis.data.monitor

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.provider.Settings
import com.kianirani.jarvis.core.monitor.AppUsage
import com.kianirani.jarvis.core.monitor.UsageAggregator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MON2 — reads `UsageStatsManager` for app foreground time / last-used and feeds the pure
 * [UsageAggregator]. The PACKAGE_USAGE_STATS access is a special permission the user grants once
 * ([hasPermission]/[permissionIntent]); everything is read on demand, nothing persisted here, and
 * the whole thing is gated by the privacy switch (MON3). Best-effort — never crashes.
 */
@Singleton
class UsageMonitor @Inject constructor(@ApplicationContext private val context: Context) {

    /** True if the user has granted usage-access to Vision. */
    fun hasPermission(): Boolean = runCatching {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
        )
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** Intent to the system Usage Access settings so the user can grant it. */
    fun permissionIntent() = android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Raw per-app usage over the last [windowMs] (default 24h). Empty without permission. */
    fun usage(windowMs: Long = 24 * 60 * 60 * 1000L): List<AppUsage> {
        if (!hasPermission()) return emptyList()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyList()
        val now = System.currentTimeMillis()
        return runCatching {
            usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - windowMs, now)
                ?.filter { it.totalTimeInForeground > 0 || it.lastTimeUsed > 0 }
                ?.map { AppUsage(it.packageName, it.totalTimeInForeground, it.lastTimeUsed) }
                ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun mostUsed(limit: Int = 10): List<AppUsage> = UsageAggregator.mostUsed(usage(), limit)
    fun recent(limit: Int = 10): List<AppUsage> = UsageAggregator.recent(usage(), limit)
    fun screenTimeMs(): Long = UsageAggregator.totalScreenTimeMs(usage())
}
