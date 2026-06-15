package com.kianirani.jarvis.core.agent

/**
 * CF2 — Tool-calling parser ("never claim an action without executing it").
 *
 * Instead of letting the cloud model *say* "I called your mum" (a lie — nothing
 * ran), the system prompt tells it to emit a single JSON object when the user
 * asks for a device action:
 *
 * ```json
 * {"tool":"action","args":"call mum"}
 * ```
 *
 * [parse] extracts that invocation from the model's reply (tolerating code
 * fences and surrounding prose). The caller then runs `args` through the real
 * tool layer (`CommandInterpreter`) and speaks the **actual** result — so a
 * claimed action is always a performed action.
 *
 * Pure logic, no Android dependencies → fully unit-testable on the JVM.
 */
object ToolCaller {

    data class ToolInvocation(val tool: String, val args: String)

    private val TOOL = Regex(""""tool"\s*:\s*"([^"]+)"""")
    private val ARGS = Regex(""""args"\s*:\s*"((?:[^"\\]|\\.)*)"""")

    /**
     * Returns the tool invocation embedded in [modelText], or null when the
     * reply is an ordinary answer (no action requested). Requires both a
     * non-blank `tool` and `args`.
     */
    fun parse(modelText: String?): ToolInvocation? {
        if (modelText.isNullOrBlank()) return null
        val tool = TOOL.find(modelText)?.groupValues?.get(1)?.trim().orEmpty()
        val rawArgs = ARGS.find(modelText)?.groupValues?.get(1) ?: return null
        if (tool.isEmpty()) return null
        val args = unescape(rawArgs).trim()
        if (args.isEmpty()) return null
        return ToolInvocation(tool, args)
    }

    /** Undo the minimal JSON string escaping we expect in a tool call. */
    private fun unescape(s: String): String =
        s.replace("\\\"", "\"").replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\")
}
