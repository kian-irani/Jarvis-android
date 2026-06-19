package com.kianirani.jarvis.data.tools

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real SMS action (fixes the v20 bug where Vision *said* "message sent" but didn't).
 *
 * Parses target + body, resolves the contact, checks the runtime permission, and only
 * reports "sent" after [SmsManager] actually accepts the message. Missing body /
 * permission / contact → an honest reply, never a fake confirmation.
 */
@Singleton
class SmsTool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contacts: ContactResolver,
) : Tool {
    override val id = "sms"

    override fun matches(message: String): Boolean = parse(message) != null

    override fun run(message: String): ToolResult {
        val parsed = parse(message) ?: return ToolResult("Who should I message, and what should I say?")
        val (target, body) = parsed
        if (body.isBlank()) return ToolResult("What should I say to \"$target\"?")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return ToolResult("I need the SMS permission first — grant it to Vision, then ask again.")
        }
        return when (val r = contacts.find(target)) {
            is ContactResolver.Lookup.NoPermission ->
                ToolResult("I need the Contacts permission to find \"$target\" — grant it to Vision.")
            is ContactResolver.Lookup.NotFound ->
                ToolResult("I couldn't find \"$target\" in your contacts.")
            is ContactResolver.Lookup.Found -> ToolResult(
                runCatching {
                    @Suppress("DEPRECATION")
                    val sms = context.getSystemService(SmsManager::class.java)
                        ?: SmsManager.getDefault()
                    sms.sendTextMessage(r.number, null, body, null, null)
                    "Message sent to ${r.displayName}."
                }.getOrElse { "I couldn't send the message: ${it.message}" },
            )
        }
    }

    companion object {
        // EN: "text/message/sms <name> (saying|that|:) <body>"  | "send <name> a message (saying|:) <body>"
        private val EN = listOf(
            Regex("""^(?:text|message|sms)\s+(.+?)\s*(?:saying|that|:)\s+(.+)$"""),
            Regex("""^send\s+(.+?)\s+(?:a\s+)?(?:message|text|sms)\s*(?:saying|that|:)?\s+(.+)$"""),
        )

        // "به X بگو …" — the exact form TOOL_PROTOCOL tells the model to emit. "بگو" is a
        // general speech verb ("say/tell"), so it's only a message when X is a real
        // addressee, never the user themself or the assistant (those are chat requests
        // like "به من بگو ساعت چنده"). Kept separate so [parse] can apply that guard.
        private val BEGO = Regex("""^به\s+(.+?)\s+بگو\s*(?:که|:)?\s*(.+)$""")

        // Speech-verb (بگو) targets that are never an SMS recipient — the user or Vision.
        private val NON_SMS_TARGETS =
            setOf("من", "خودم", "خودت", "ویژن", "vision", "جارویس", "jarvis", "me", "myself", "yourself")

        // FA: به X پیام بده [که|:] body / به X بنویس body / پیام بده به X [که|:] body /
        //     به X بگو [که|:] body / به X بفرست body / برای X بنویس|بفرست body
        private val FA = listOf(
            Regex("""^به\s+(.+?)\s+(?:پیام|اس\s*ام\s*اس)\s+بده\s*(?:که|:)?\s*(.+)$"""),
            Regex("""^به\s+(.+?)\s+بنویس\s+(.+)$"""),
            Regex("""^(?:پیام|اس\s*ام\s*اس)\s+بده\s+به\s+(.+?)\s*(?:که|:)?\s*(.+)$"""),
            BEGO,
            Regex("""^به\s+(.+?)\s+بفرست\s+(.+)$"""),
            Regex("""^برای\s+(.+?)\s+(?:بنویس|بفرست)\s*(?:که|:)?\s*(.+)$"""),
        )

        /** Returns (target, body) for a send-message command, or null if not one. */
        fun parse(message: String): Pair<String, String>? {
            val m = message.trim().lowercase()
            if (m.isEmpty()) return null
            for (rx in EN + FA) {
                val g = rx.find(m)?.groupValues ?: continue
                val target = g.getOrNull(1)?.trim().orEmpty()
                val body = g.getOrNull(2)?.trim().orEmpty()
                if (target.isEmpty()) continue
                // "بگو" to self/assistant is a chat request, not an SMS — let it flow to AI.
                if (rx === BEGO && target in NON_SMS_TARGETS) continue
                return target to body
            }
            return null
        }
    }
}
