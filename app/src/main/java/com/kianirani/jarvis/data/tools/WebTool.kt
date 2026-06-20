package com.kianirani.jarvis.data.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PAB — opens web pages, web searches, and YouTube searches with a real `ACTION_VIEW`
 * intent (the system browser / YouTube app handle it). Routing lives in the pure
 * [WebIntents] parser; this only fires the intent and reports honestly. Registered in the
 * device [ToolRegistry], so it is reachable from the chat path AND from the VCF agent (via
 * the `device_command` bridge) — "find Shakira's new song on YouTube and play it".
 */
@Singleton
class WebTool @Inject constructor(@ApplicationContext private val context: Context) : Tool {
    override val id = "web"

    override fun matches(message: String): Boolean = WebIntents.parse(message) != null

    override fun run(message: String): ToolResult {
        val target = WebIntents.parse(message) ?: return ToolResult("Nothing to open.")
        return runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(target.url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            ToolResult(target.reply)
        }.getOrElse { ToolResult("Couldn't open that: ${it.message}") }
    }
}
