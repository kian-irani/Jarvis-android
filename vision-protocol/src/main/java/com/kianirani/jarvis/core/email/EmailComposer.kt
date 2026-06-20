package com.kianirani.jarvis.core.email

/**
 * PAE — email assist (compose path). The pure part: build the `mailto:` URI an `ACTION_SENDTO`
 * intent fires so Vision can draft an email to a contact ("email Ali about the meeting") without
 * any API/OAuth — the user's mail app opens pre-filled, and they confirm the send (honest: Vision
 * never claims to have sent without the user). Reading inbox/OAuth is the heavier follow-up.
 *
 * Pure (RFC-6068 mailto with percent-encoded query) → JVM-tested; the `Intent` launch is the
 * device half.
 */
data class EmailDraft(val to: String, val subject: String = "", val body: String = "")

object EmailComposer {

    private const val UNRESERVED = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~"

    /** RFC-3986 percent-encoding for a mailto query value (space → %20). */
    fun encode(s: String): String = buildString {
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            if (c.toChar() in UNRESERVED) append(c.toChar()) else append('%').append("%02X".format(c))
        }
    }

    /**
     * Build the `mailto:` URI for [draft]. The recipient is in the path; subject/body go in the
     * query (omitted when blank). e.g. `mailto:ali@x.com?subject=Hi&body=...`.
     */
    fun mailtoUri(draft: EmailDraft): String {
        val params = buildList {
            if (draft.subject.isNotBlank()) add("subject=${encode(draft.subject)}")
            if (draft.body.isNotBlank()) add("body=${encode(draft.body)}")
        }
        val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        return "mailto:${draft.to.trim()}$query"
    }

    /** Basic sanity for a recipient address (one `@`, a dot after it). */
    fun isValidRecipient(to: String): Boolean {
        val t = to.trim()
        val at = t.indexOf('@')
        return at > 0 && t.indexOf('@', at + 1) == -1 && t.indexOf('.', at) > at && !t.endsWith('.')
    }
}
