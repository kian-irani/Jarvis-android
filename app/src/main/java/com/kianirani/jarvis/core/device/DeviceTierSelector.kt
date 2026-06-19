package com.kianirani.jarvis.core.device

/**
 * LM3 — device-tier gating (PRD §, "انتخاب واریانت بر اساس RAM/CPU"). Classifies the phone
 * into a capability [DeviceTier] and picks the **largest on-device model that actually fits**,
 * so a low-RAM device gets a smaller model (or a warning) instead of OOM-ing on a model it
 * can't run. Pure → JVM-tested; reading the real RAM/CPU and loading/unloading the model are
 * the on-device half.
 */
enum class DeviceTier { NANO, LITE, FULL }

object DeviceTierSelector {

    /** Below this much RAM (GB) Vision warns and steers to the smallest model. */
    const val LOW_MEMORY_GB: Float = 3f

    /**
     * Classify a device. FULL needs ample RAM and cores (comfortably runs the 1B tier); NANO is
     * the constrained bucket (small model only); everything between is LITE.
     */
    fun tierFor(ramGb: Float, cpuCores: Int): DeviceTier = when {
        ramGb >= 6f && cpuCores >= 8 -> DeviceTier.FULL
        ramGb < LOW_MEMORY_GB || cpuCores < 4 -> DeviceTier.NANO
        else -> DeviceTier.LITE
    }

    /** True on a constrained device — the UI should warn and prefer the smallest model. */
    fun warnLowMemory(ramGb: Float): Boolean = ramGb < LOW_MEMORY_GB

    /**
     * Pick the best model that fits in [ramGb]: the candidate with the **largest** RAM
     * requirement that is still ≤ available RAM. Returns null if even the smallest doesn't fit.
     * Ties (equal requirement) break by the candidate's natural order via [minRamOf] stability.
     */
    fun <T> selectModel(ramGb: Float, candidates: List<T>, minRamOf: (T) -> Float): T? =
        candidates
            .filter { minRamOf(it) <= ramGb }
            .maxByOrNull { minRamOf(it) }
}
