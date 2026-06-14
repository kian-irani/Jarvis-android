package com.kianirani.jarvis.router.orchestrator

import javax.inject.Inject
import javax.inject.Singleton

/**
 * VISION BRAIN V1 — lightweight, on-device intent + modality detection.
 *
 * A fast heuristic pass (no network, no model) good enough to pick the right
 * capability for routing. It is intentionally cheap: a wrong guess only changes
 * *ranking*, never correctness — the Substitution Engine (VB5) still falls back
 * across the whole candidate list. A future version may defer ambiguous cases
 * to the local model.
 */
@Singleton
class IntentClassifier @Inject constructor() {

    private val codeHints = listOf(
        "code", "function", "compile", "regex", "stack trace", "exception", "bug",
        "kotlin", "java", "python", "javascript", "typescript", "rust", "sql", "bash",
        "gradle", "npm", "git ", "build error", "refactor", "stacktrace",
        "کد", "برنامه نویسی", "برنامه‌نویسی", "تابع", "کامپایل", "باگ", "خطای",
    )

    private val reasoningHints = listOf(
        "why", "explain", "compare", "analyze", "analyse", "plan", "strategy",
        "prove", "derive", "step by step", "reason", "trade-off", "tradeoff",
        "چرا", "توضیح بده", "مقایسه", "تحلیل", "اثبات", "قدم به قدم", "استدلال",
    )

    private val actionHints = listOf(
        "turn on", "turn off", "open ", "set ", "enable", "disable", "toggle",
        "روشن کن", "خاموش کن", "باز کن", "تنظیم کن", "فعال کن", "غیرفعال کن",
    )

    /**
     * Classify a text [message]; [hasImage]/[isVoice] are hard signals from the
     * caller (an attached screenshot, a spoken request) that override text.
     */
    fun classify(message: String, hasImage: Boolean = false, isVoice: Boolean = false): Pair<Intent, Modality> {
        if (hasImage) return Intent.IMAGE to Modality.IMAGE
        val text = message.trim().lowercase()
        val modality = if (isVoice) Modality.AUDIO else Modality.TEXT

        val intent = when {
            text.isEmpty() -> Intent.CHAT
            codeHints.any { text.contains(it) } -> Intent.CODE
            actionHints.any { text.contains(it) } -> Intent.ACTION
            reasoningHints.any { text.contains(it) } -> Intent.REASONING
            isShortLookup(text) -> Intent.QUICK
            else -> Intent.CHAT
        }
        return intent to modality
    }

    /** Short, single-clause questions favour a fast model over a deep one. */
    private fun isShortLookup(text: String): Boolean {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.size <= 6
    }
}
