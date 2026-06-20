package com.kianirani.jarvis.core.automation

/**
 * Conversational multi-step OS commands (PRD §, "دستورات OS محاوره‌ای چندمرحله‌ای"). Splits a
 * compound spoken instruction — "open Maps then search for coffee and navigate" — into ordered
 * atomic steps the agent runs in turn, stopping if one fails. Pure splitting on natural
 * connectives (EN + FA) → JVM-tested; running each step is the agent/tool half.
 */
object CommandSequence {

    // Split on " then ", " and then ", " after that ", "; ", and Persian "سپس"/"بعد".
    private val SPLIT = Regex("""(?i)\s*(?:,?\s*and then\s+|\s+then\s+|\s+after that\s+|;\s*|\s+سپس\s+|\s+بعدش?\s+)\s*""")

    /** Split [input] into ordered, non-blank steps. A single command → one step. */
    fun split(input: String): List<String> =
        input.split(SPLIT).map { it.trim() }.filter { it.isNotEmpty() }

    /** True if the input has more than one step (worth running as a sequence). */
    fun isMultiStep(input: String): Boolean = split(input).size > 1

    /**
     * Plan the run: each step paired with its 1-based index, so progress can be reported
     * ("step 2 of 3: searching…").
     */
    fun plan(input: String): List<IndexedValue<String>> = split(input).mapIndexed { i, s -> IndexedValue(i + 1, s) }
}
