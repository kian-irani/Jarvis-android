package com.kianirani.jarvis.data.tools

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real phone-call action (fixes the v20 bug where Vision *said* it called but didn't).
 *
 * It resolves the contact, checks the runtime permission, and only reports success
 * after `ACTION_CALL` actually fires. If anything is missing it says so plainly —
 * Vision never claims an action it didn't perform.
 */
@Singleton
class CallTool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contacts: ContactResolver,
) : Tool {
    override val id = "call"

    override fun matches(message: String): Boolean = parseTarget(message) != null

    override fun run(message: String): ToolResult {
        val target = parseTarget(message) ?: return ToolResult("Who should I call?")
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return ToolResult("I need the Phone (call) permission first — grant it to Vision, then ask again.")
        }
        return when (val r = contacts.find(target)) {
            is ContactResolver.Lookup.NoPermission ->
                ToolResult("I need the Contacts permission to find \"$target\" — grant it to Vision.")
            is ContactResolver.Lookup.NotFound ->
                ToolResult("I couldn't find \"$target\" in your contacts.")
            is ContactResolver.Lookup.Found -> ToolResult(
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_CALL, Uri.parse("tel:${r.number}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                    "Calling ${r.displayName}…"
                }.getOrElse { "I couldn't place the call: ${it.message}" },
            )
        }
    }

    companion object {
        // EN: "call/phone/dial/ring <name> [now|please]"  |  "give <name> a call"
        private val EN = listOf(
            Regex("""^(?:call|phone|dial|ring)\s+(.+?)(?:\s+(?:now|please|right now))?$"""),
            Regex("""^give\s+(.+?)\s+a\s+call$"""),
        )

        // FA variants (word order differs). Order matters: more specific forms first so a
        // looser one (e.g. bare "زنگ بزن X") can't swallow the qualified version.
        private val FA = listOf(
            Regex("""^به\s+(.+?)\s+(?:یه|یک)\s+زنگ\s+بزن$"""), // "به X یه زنگ بزن"
            Regex("""^با\s+(.+?)\s+تماس\s+بگیر$"""),
            Regex("""^به\s+(.+?)\s+زنگ\s+بزن$"""),
            Regex("""^تماس\s+بگیر\s+با\s+(.+)$"""),
            Regex("""^تماس\s+با\s+(.+?)\s+بگیر$"""),
            Regex("""^زنگ\s+بزن\s+به\s+(.+)$"""),
            Regex("""^زنگ\s+بزن\s+(.+)$"""), // bare "زنگ بزن X" (after the "به X" form above)
            Regex("""^شماره\s+(.+?)\s+(?:رو|را)\s+بگیر$"""), // "شماره X رو بگیر"
        )

        /** Extract the call target ("mother") from a command, or null if not a call. */
        fun parseTarget(message: String): String? {
            val m = message.trim().lowercase()
            if (m.isEmpty()) return null
            for (rx in EN) rx.find(m)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
            for (rx in FA) rx.find(m)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
            return null
        }
    }
}
