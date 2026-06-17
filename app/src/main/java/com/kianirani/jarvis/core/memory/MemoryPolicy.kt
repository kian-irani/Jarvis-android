package com.kianirani.jarvis.core.memory

/**
 * CF4.2 — quality gate for auto-captured memories. Avoids polluting recall with
 * trivia ("hi", "ok", short commands, a pasted URL): only reasonably substantial,
 * multi-word user messages are worth keeping as EPISODIC memory. Pure → unit-tested.
 * (A richer, model-judged importance is future work; this is the cheap heuristic.)
 */
object MemoryPolicy {
    const val MIN_CHARS = 20
    const val MIN_WORDS = 4

    fun worthRemembering(text: String): Boolean {
        val t = text.trim()
        if (t.length < MIN_CHARS) return false
        return t.split(Regex("\\s+")).count { it.isNotBlank() } >= MIN_WORDS
    }
}
