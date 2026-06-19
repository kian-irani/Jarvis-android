package com.kianirani.jarvis.core.flags

/**
 * DS-X2 — feature flags (the deployment track's "کانال‌های dev/beta/prod + feature flags"). A
 * pure, deterministic gate so a feature can ship dark, light up on a channel, or roll out to a
 * percentage of installs without a new build. Unknown flags are **off** (fail-closed). The
 * rollout bucket is a stable hash of the flag key + a per-install id, so the same install
 * always sees the same answer and the split is reproducible.
 *
 * Pure → JVM-tested; loading the flag set (remote config / build channel) is the on-device half.
 */
enum class ReleaseChannel { DEV, BETA, PROD }

data class FeatureFlag(
    val key: String,
    val default: Boolean = false,
    /** Per-channel forced value (e.g. on in DEV/BETA, off in PROD). Overrides [default]. */
    val channelOverrides: Map<ReleaseChannel, Boolean> = emptyMap(),
    /** 0..100: deterministic percentage of installs that see it when otherwise enabled. */
    val rolloutPercent: Int = 100,
)

class FeatureFlags(
    flags: List<FeatureFlag>,
    private val channel: ReleaseChannel,
    /** A stable per-install identifier so the rollout bucket is consistent for this device. */
    private val stableId: String,
) {
    private val byKey = flags.associateBy { it.key }

    /** True if [key] is enabled for this channel + install. Unknown keys are off. */
    fun isEnabled(key: String): Boolean {
        val flag = byKey[key] ?: return false
        val base = flag.channelOverrides[channel] ?: flag.default
        if (!base) return false
        val pct = flag.rolloutPercent.coerceIn(0, 100)
        if (pct >= 100) return true
        if (pct <= 0) return false
        return bucket(key, stableId) < pct
    }

    /** All currently-enabled flag keys (useful for diagnostics / a debug screen). */
    fun enabledKeys(): Set<String> = byKey.keys.filterTo(mutableSetOf()) { isEnabled(it) }

    private companion object {
        /** FNV-1a → a stable 0..99 bucket, independent of JVM String.hashCode quirks. */
        fun bucket(key: String, stableId: String): Int {
            var h = 0x811c9dc5.toInt()
            for (c in "$key:$stableId") {
                h = h xor c.code
                h *= 0x01000193
            }
            return ((h % 100) + 100) % 100
        }
    }
}
