package com.kianirani.jarvis.core.writing

/**
 * PAW — proactive writing assist (PRD example: helping the user word a chat message). When
 * Vision reads the active text field (via Accessibility), this pure step decides **whether it
 * should even offer a rewrite** — and builds the improvement prompt if so — before bothering
 * the model or the user. The gate avoids nagging on tiny inputs, read-only fields, and
 * sensitive fields (passwords/OTP). Pure → JVM-tested; reading the field and writing the
 * accepted suggestion back (`ACTION_SET_TEXT`) are the on-device half.
 */
data class FieldContext(
    val text: String,
    val isEditable: Boolean,
    /** Field hint/type, e.g. "text", "password", "emailAddress" (from the a11y node). */
    val fieldType: String = "text",
)

data class AssistDecision(val shouldSuggest: Boolean, val reason: String, val prompt: String?)

object WritingAssist {

    private val SENSITIVE = listOf("password", "passwd", "pin", "otp", "cvv", "card")

    private fun isSensitive(fieldType: String): Boolean {
        val t = fieldType.lowercase()
        return SENSITIVE.any { t.contains(it) }
    }

    private fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

    /**
     * Decide whether to offer a rewrite for [field]. Suggests only when the field is editable,
     * not sensitive, and has at least [minWords] words of real content. Returns the
     * [improvePrompt] when it will suggest, else null.
     */
    fun evaluate(field: FieldContext, minWords: Int = 3): AssistDecision {
        if (!field.isEditable) return AssistDecision(false, "field is read-only", null)
        if (isSensitive(field.fieldType)) return AssistDecision(false, "sensitive field", null)
        if (field.text.isBlank()) return AssistDecision(false, "empty field", null)
        if (wordCount(field.text) < minWords) return AssistDecision(false, "too short", null)
        return AssistDecision(true, "eligible", improvePrompt(field.text))
    }

    /** The prompt handed to the model to produce a cleaner version of [text]. */
    fun improvePrompt(text: String): String =
        "Rewrite the following message to be clearer and well-phrased, keeping the user's " +
            "language, meaning, and tone. Return only the rewritten text.\n\n$text"
}
