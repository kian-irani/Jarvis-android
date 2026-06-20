package com.kianirani.jarvis.service

/**
 * PAU — the pure node-matching logic for universal app automation. Decides whether an
 * accessibility node's label matches a target the agent wants to click ("tap Send", "press
 * Login"), independent of Android so it is JVM-tested. The service does the tree walk and the
 * real click; this normalizes + compares text. Match is forgiving: exact, then contains, on
 * whitespace/case-normalized strings.
 */
object AccessibilityMatch {

    private val WS = Regex("\\s+")

    /** Lowercase + collapse whitespace + trim, so "  Send  Now " ≈ "send now". */
    fun normalize(s: String?): String = s?.lowercase()?.replace(WS, " ")?.trim().orEmpty()

    /** A node's effective label: its text, else its content description. */
    fun label(text: String?, contentDescription: String?): String =
        text?.takeIf { it.isNotBlank() } ?: contentDescription.orEmpty()

    /**
     * Does [nodeLabel] match the already-normalized [target]? Exact match wins; otherwise the
     * label containing the target (whole-target substring) counts — so "Send message" matches a
     * target of "send". Empty target never matches.
     */
    fun matches(nodeLabel: String?, target: String): Boolean {
        if (target.isBlank()) return false
        val label = normalize(nodeLabel)
        if (label.isEmpty()) return false
        return label == target || label.contains(target)
    }
}
