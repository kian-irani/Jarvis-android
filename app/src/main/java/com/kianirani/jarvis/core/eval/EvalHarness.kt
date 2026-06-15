package com.kianirani.jarvis.core.eval

import com.kianirani.jarvis.router.orchestrator.Intent
import com.kianirani.jarvis.router.orchestrator.IntentClassifier

/**
 * CF6 — evaluation harness.
 *
 * Runs a fixed set of golden prompts through the on-device [IntentClassifier]
 * and scores the result, so a regression in intent routing — especially the
 * **Persian + code-switch** handling behind the v20 language bug — fails CI
 * instead of shipping. Pure and deterministic: no network, no model.
 *
 * It deliberately evaluates the *classifier* (the part reproducible on a server /
 * in CI), not live STT/TTS, which can only be checked on a device.
 */
class EvalHarness(private val classifier: IntentClassifier = IntentClassifier()) {

    fun run(cases: List<EvalCase> = GoldenPrompts.cases): EvalReport {
        val results = cases.map { c ->
            val (intent, _) = classifier.classify(c.prompt, hasImage = c.hasImage, isVoice = c.isVoice)
            EvalResult(case = c, actual = intent, passed = intent == c.expectedIntent)
        }
        return EvalReport(results)
    }
}

/** One golden expectation: a prompt (with optional hard signals) and its intent. */
data class EvalCase(
    val name: String,
    val prompt: String,
    val expectedIntent: Intent,
    val hasImage: Boolean = false,
    val isVoice: Boolean = false,
)

data class EvalResult(val case: EvalCase, val actual: Intent, val passed: Boolean)

data class EvalReport(val results: List<EvalResult>) {
    val total: Int get() = results.size
    val passed: Int get() = results.count { it.passed }
    val passRate: Double get() = if (total == 0) 0.0 else passed.toDouble() / total
    val failures: List<EvalResult> get() = results.filter { !it.passed }

    /** Compact, copy-pasteable summary of any failures (for CI logs). */
    fun describeFailures(): String =
        failures.joinToString("\n") { "✗ ${it.case.name}: '${it.case.prompt}' expected ${it.case.expectedIntent} got ${it.actual}" }
}

/**
 * The golden corpus. Mixes English, Persian, and code-switched prompts across
 * every intent so the classifier's language coverage is pinned. Add a case
 * whenever a real misclassification is found (turn the bug into a test).
 */
object GoldenPrompts {
    val cases: List<EvalCase> = listOf(
        // CODE — EN / FA / code-switch
        EvalCase("code-en", "fix this kotlin compile error", Intent.CODE),
        EvalCase("code-fa", "این کد را دیباگ کن", Intent.CODE),
        EvalCase("code-switch", "این function رو refactor کن", Intent.CODE),
        // ACTION — EN / FA / code-switch
        EvalCase("action-en", "turn on the flashlight", Intent.ACTION),
        EvalCase("action-fa", "چراغ قوه را روشن کن", Intent.ACTION),
        EvalCase("action-switch", "wifi رو خاموش کن", Intent.ACTION),
        // REASONING — EN / FA
        EvalCase("reason-en", "explain why the sky is blue in detail", Intent.REASONING),
        EvalCase("reason-fa", "چرا آسمان آبی است را توضیح بده", Intent.REASONING),
        // QUICK (short lookups) — EN / FA
        EvalCase("quick-en", "weather today", Intent.QUICK),
        EvalCase("quick-fa", "ساعت چنده", Intent.QUICK),
        // CHAT (longer, no strong signal) — EN / FA
        EvalCase("chat-en", "tell me a story about a brave little robot please", Intent.CHAT),
        EvalCase("chat-fa", "یک داستان کوتاه برایم تعریف کن درباره یک سفر", Intent.CHAT),
        // Hard signals override text
        EvalCase("image-signal", "what is in this picture", Intent.IMAGE, hasImage = true),
        EvalCase("voice-keeps-intent", "turn on the flashlight", Intent.ACTION, isVoice = true),
    )
}
