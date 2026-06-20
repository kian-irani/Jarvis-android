package com.kianirani.jarvis.core.privacy

/**
 * MON3 — monitoring privacy/consent (PRD §, "همه‌چیز on-device؛ تحت Trust gate؛ سوییچِ شفافِ
 * روشن/خاموش — کاربر کنترل کامل دارد"). The pure gate that decides whether each sensitive
 * monitoring source (notifications, usage, screen, call/SMS log) may be read — every source is
 * **off by default** and the user opts each one in. So no monitoring happens unless explicitly
 * consented. Pure → JVM-tested; the persisted switches + the actual readers are the device half.
 */
enum class MonitorSource { NOTIFICATIONS, USAGE, SCREEN, CALL_LOG, SMS, LOCATION }

data class MonitoringConsent(
    /** Master kill-switch — when false, nothing is monitored regardless of per-source flags. */
    val masterEnabled: Boolean = false,
    val allowed: Set<MonitorSource> = emptySet(),
) {
    /** May [source] be read right now? Requires the master switch AND the per-source opt-in. */
    fun canMonitor(source: MonitorSource): Boolean = masterEnabled && source in allowed

    /** Grant a source (no-op effect until [masterEnabled]). */
    fun grant(source: MonitorSource): MonitoringConsent = copy(allowed = allowed + source)

    /** Revoke a source. */
    fun revoke(source: MonitorSource): MonitoringConsent = copy(allowed = allowed - source)

    /** The sources actually active (master on + granted) — for a transparent status list. */
    fun activeSources(): Set<MonitorSource> = if (masterEnabled) allowed else emptySet()
}
