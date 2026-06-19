package com.kianirani.jarvis.core.palette

/**
 * DS-WIN2 — the command palette matcher (a global, Spotlight/Copilot-style launcher). Given a
 * query and the registered [Command]s, it returns the best matches using a layered fuzzy score
 * (exact > prefix > token-prefix > keyword > substring > subsequence) so a few keystrokes —
 * even an abbreviation like "tdm" for "Toggle Dark Mode" — find the right action fast.
 *
 * Pure & deterministic → JVM-tested. The desktop/Android shell that captures the global hotkey
 * and runs the chosen command is the on-device half (shared across launcher + Windows shell).
 */
data class Command(
    val id: String,
    val title: String,
    val keywords: List<String> = emptyList(),
    val category: String = "",
)

object CommandPalette {

    private val WS = Regex("\\s+")

    /** True if every char of [needle] appears in [haystack] in order (abbreviation match). */
    private fun isSubsequence(needle: String, haystack: String): Boolean {
        if (needle.isEmpty()) return true
        var i = 0
        for (c in haystack) if (c == needle[i] && ++i == needle.length) return true
        return false
    }

    /** Fuzzy relevance of [command] for [query] in `0f..1f`; 0 means no match. Case-insensitive. */
    fun score(query: String, command: Command): Float {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return 0f
        val title = command.title.trim().lowercase()
        val keywords = command.keywords.map { it.lowercase() }

        if (title == q) return 1.0f
        if (title.startsWith(q)) return 0.9f

        val qTokens = q.split(WS).filter { it.isNotBlank() }
        val tTokens = title.split(WS).filter { it.isNotBlank() }
        if (qTokens.isNotEmpty() && qTokens.all { tk -> tTokens.any { it.startsWith(tk) } }) return 0.75f

        if (keywords.any { it == q }) return 0.7f
        if (keywords.any { it.startsWith(q) }) return 0.6f
        if (title.contains(q)) return 0.5f
        if (isSubsequence(q.replace(" ", ""), title.replace(" ", ""))) return 0.4f
        if (keywords.any { it.contains(q) }) return 0.35f
        return 0f
    }

    /**
     * The matching commands for [query], best first. A blank query returns the full list (a
     * browse) capped at [limit]; otherwise only positive-scoring commands appear. Ties break by
     * title then id for stability. [limit] ≤ 0 returns all.
     */
    fun match(query: String, commands: List<Command>, limit: Int = 8): List<Command> {
        val q = query.trim()
        if (q.isEmpty()) return if (limit > 0) commands.take(limit) else commands
        val ranked = commands.map { it to score(q, it) }
            .filter { it.second > 0f }
            .sortedWith(
                compareByDescending<Pair<Command, Float>> { it.second }
                    .thenBy { it.first.title.lowercase() }
                    .thenBy { it.first.id },
            )
            .map { it.first }
        return if (limit > 0) ranked.take(limit) else ranked
    }
}
