package com.kianirani.jarvis.router.cost

import com.kianirani.jarvis.router.registry.ModelBackend
import com.kianirani.jarvis.router.registry.ModelSpec
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — Module 7 (Adaptive Cost Controller / VB7).
 *
 * Spending modes the user picks, plus a pre-flight estimate and a budget cap so the
 * router can prefer cheaper models and stop before a surprise bill.
 *
 *  - **Economy** — only near-free models (local, or very cheap cloud).
 *  - **Balanced** — moderate cost allowed.
 *  - **Premium** — expensive top-tier models allowed.
 *  - **Unlimited** — no gating at all (the default, so behaviour is unchanged until the
 *    user opts into a budget).
 *
 * Cost is expressed in abstract **units** derived from each model's inverted
 * `scores.cost` (10 = essentially free → 0 units; 1 = very expensive → 9 units) times
 * the token volume, so a real price table can replace it later without touching callers.
 * On-device models are always free, so they pass every mode and never touch the budget —
 * the anti-"can't afford to answer" guarantee that pairs with VB5's local fallback.
 */
enum class CostMode(val maxUnitsPerRequest: Double?) {
    ECONOMY(2.0),
    BALANCED(6.0),
    PREMIUM(20.0),
    UNLIMITED(null),
}

/** Outcome of a pre-flight check: whether to allow [estimateUnits] and why. */
data class CostDecision(val allowed: Boolean, val estimateUnits: Double, val reason: String)

@Singleton
class CostController @Inject constructor() {

    private val modeRef = AtomicReference(CostMode.UNLIMITED)

    /** Monthly budget in cost units; null = no cap. */
    private val budgetRef = AtomicReference<Double?>(null)

    /** Spend accumulated this period, in cost units. */
    @Volatile private var spent: Double = 0.0

    var mode: CostMode
        get() = modeRef.get()
        set(value) = modeRef.set(value)

    var budgetUnits: Double?
        get() = budgetRef.get()
        set(value) = budgetRef.set(value)

    val spentUnits: Double get() = spent

    /** Pre-flight cost estimate for running [spec] over the given token volume. */
    fun estimate(spec: ModelSpec, promptTokens: Int, completionTokens: Int): Double {
        if (spec.backend != ModelBackend.CLOUD) return 0.0 // local/mesh are free here
        val perK = (10 - spec.scores.cost).coerceAtLeast(0).toDouble()
        return (promptTokens + completionTokens) / 1000.0 * perK
    }

    /**
     * Whether [spec] may run for this token volume under the current mode and budget.
     * Free models always pass. Otherwise the estimate must fit the mode's per-request
     * ceiling and must not push period spend over the budget (Unlimited bypasses both).
     */
    fun check(spec: ModelSpec, promptTokens: Int, completionTokens: Int): CostDecision {
        val est = estimate(spec, promptTokens, completionTokens)
        if (est == 0.0) return CostDecision(true, 0.0, "free")
        val m = mode
        m.maxUnitsPerRequest?.let { cap ->
            if (est > cap) return CostDecision(false, est, "exceeds ${m.name} per-request cap ($cap)")
        }
        budgetUnits?.let { budget ->
            if (m != CostMode.UNLIMITED && spent + est > budget) {
                return CostDecision(false, est, "would exceed budget ($budget, spent $spent)")
            }
        }
        return CostDecision(true, est, "ok under ${m.name}")
    }

    /** Record actual spend after a successful cloud call. */
    fun record(units: Double) {
        if (units > 0) spent += units
    }

    /** True once spend reaches 80% of the budget (warn the user). */
    fun budgetWarning(): Boolean {
        val budget = budgetUnits ?: return false
        return budget > 0 && spent >= 0.8 * budget
    }

    /** Reset the period spend (e.g. new month / user reset). */
    fun resetPeriod() { spent = 0.0 }
}
