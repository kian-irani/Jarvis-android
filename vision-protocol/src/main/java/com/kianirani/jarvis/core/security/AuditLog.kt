package com.kianirani.jarvis.core.security

import kotlinx.serialization.Serializable

/**
 * Audit log (PRD §, "رمزنگاری end-to-end mesh · audit log"). An append-only, bounded record of
 * security-relevant actions Vision took — tool runs, mesh joins, permission grants, critical
 * confirmations — so the user can review "what did it do, and when". Pure & serializable;
 * sensitive payloads are redacted on write so the log itself never leaks a token/secret. The
 * actual at-rest encryption (Android Keystore) is the device half.
 */
@Serializable
enum class AuditCategory { TOOL_RUN, PERMISSION, MESH, CONFIRMATION, PRIVACY, ERROR }

@Serializable
data class AuditEntry(
    val atMillis: Long,
    val category: AuditCategory,
    val action: String,
    val detail: String = "",
)

class AuditLog(private val capacity: Int = DEFAULT_CAPACITY) {

    private val entries = ArrayDeque<AuditEntry>() // newest at index 0

    /** Record an action; [detail] is redacted of token-like substrings before storing. */
    fun record(category: AuditCategory, action: String, detail: String = "", atMillis: Long) {
        entries.addFirst(AuditEntry(atMillis, category, action, redact(detail)))
        while (entries.size > capacity) entries.removeLast()
    }

    /** Entries newest-first, optionally filtered by [category]. */
    fun entries(category: AuditCategory? = null): List<AuditEntry> =
        entries.filter { category == null || it.category == category }

    fun clear() = entries.clear()

    private companion object {
        const val DEFAULT_CAPACITY = 500

        // Redact long token-ish runs (20+ url-safe chars) and bearer/key phrases.
        val TOKEN = Regex("""(?i)(bearer\s+|sk-|key[=:]\s*)?[A-Za-z0-9_\-]{20,}""")

        fun redact(s: String): String = TOKEN.replace(s) { m ->
            val prefix = m.groupValues[1]
            prefix + "***redacted***"
        }
    }
}
