package com.kianirani.jarvis.data.tools

import com.kianirani.jarvis.service.VisionAccessibilityService
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PAU / PAW — universal on-screen automation as a tool. Drives the Accessibility service to
 * **act inside any app**: tap an element by its visible text, scroll, type into the focused
 * field, or read it back. This is the executable half of "let Vision do the user's tasks in
 * every app" and the foundation of writing-assist (read field → improve → replace).
 *
 * Pure parse in [UiCommand] (JVM-testable); the side effects go through the live service. If
 * the service isn't enabled, returns a clear hint instead of failing — same contract as
 * [NavigationTool]. Registered in [ToolRegistry], so both the chat path and the VCF agent
 * (via the `device_command` bridge) can call it.
 */
@Singleton
class UiAutomationTool @Inject constructor() : Tool {
    override val id = "ui_automation"

    override fun matches(message: String): Boolean = UiCommand.parse(message) != null

    override fun run(message: String): ToolResult {
        val cmd = UiCommand.parse(message) ?: return ToolResult("Unknown UI command.")
        val svc = VisionAccessibilityService.instance
            ?: return ToolResult("Turn on Vision Device Control first: SYSTEM CONFIG → Device Control.")
        return when (cmd) {
            is UiCommand.Click ->
                ToolResult(if (svc.clickByText(cmd.target)) "Tapped \"${cmd.target}\"." else "Couldn't find \"${cmd.target}\" on screen.")
            is UiCommand.Scroll ->
                ToolResult(if (svc.scroll(cmd.forward)) "Scrolled ${if (cmd.forward) "down" else "up"}." else "Nothing to scroll here.")
            is UiCommand.Type ->
                ToolResult(if (svc.setFocusedText(cmd.text)) "Typed into the field." else "No text field is focused.")
            UiCommand.ReadField -> {
                val t = svc.focusedText()
                ToolResult(if (t.isNullOrBlank()) "No focused text field." else "The field says: $t")
            }
        }
    }
}

/** Pure parser for on-screen automation commands (EN/FA). */
sealed interface UiCommand {
    data class Click(val target: String) : UiCommand
    data class Scroll(val forward: Boolean) : UiCommand
    data class Type(val text: String) : UiCommand
    data object ReadField : UiCommand

    companion object {
        fun parse(message: String): UiCommand? {
            val m = message.trim()
            val low = m.lowercase()

            // Read the focused field (PAW "improve" reads first).
            if (low in setOf("read field", "read the field", "what's in the field", "متن فیلد", "فیلد رو بخون", "متنِ فیلد را بخوان")) {
                return ReadField
            }

            // Scroll up/down.
            scrollDir(low)?.let { return Scroll(it) }

            // Type / write into the focused field.
            forContent(m, low, listOf("type ", "write ", "enter ", "بنویس ", "تایپ کن ", "بنویس:"))?.let { return Type(it) }

            // Tap an on-screen element by its visible label.
            forContent(m, low, listOf("click ", "tap ", "press ", "کلیک کن روی ", "بزن روی ", "روی ", "لمس کن "))
                ?.let { return Click(it.removePrefix("on ").trim()) }

            return null
        }

        private fun scrollDir(low: String): Boolean? = when {
            low == "scroll down" || low == "scroll" || low.contains("اسکرول پایین") || low == "پایین برو" -> true
            low == "scroll up" || low.contains("اسکرول بالا") || low == "بالا برو" -> false
            else -> null
        }

        /** If [low] starts with one of [prefixes], return the trimmed remainder (the content). */
        private fun forContent(original: String, low: String, prefixes: List<String>): String? {
            for (p in prefixes) {
                if (low.startsWith(p)) {
                    val content = original.substring(p.length).trim().trim('"', '“', '”', '«', '»')
                    if (content.isNotEmpty()) return content
                }
            }
            return null
        }
    }
}
