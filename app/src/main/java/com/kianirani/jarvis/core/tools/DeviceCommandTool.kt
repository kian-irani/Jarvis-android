package com.kianirani.jarvis.core.tools

import com.kianirani.jarvis.core.agent.ActionRisk
import com.kianirani.jarvis.core.graph.ContentPart
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * VCF-LIVE-1 — the device/OS action exposed as a tool the agent can call mid-run.
 *
 * Bridges the already-working on-device command layer (`CommandInterpreter` — open app,
 * time/date/battery, arithmetic, settings panels, plugins) into a VCF [VisionTool] so the
 * ReAct agent can *do* things, not just talk. The interpreter is injected as a suspend
 * lambda (real impl calls `CommandInterpreter.tryHandle`), so the tool is pure of Android
 * and unit-tested without a device. The lambda returns the reply text, or null when the
 * text was not a recognized device command — which becomes an error observation so the
 * model self-corrects instead of the layer claiming a fake action ("never claim an action
 * without executing it").
 *
 * AUTO trust: these are the same local actions the chat already runs with zero confirmation;
 * launching an app mutates state so it is NOT [ToolSpec.readOnly].
 */
class DeviceCommandTool(private val onCommand: suspend (command: String) -> String?) : VisionTool {
    override val spec = ToolSpec(
        name = "device_command",
        description =
            "Perform an on-device action on the phone: open/launch an app, report the time, " +
                "date or battery level, do quick arithmetic, or toggle a system setting " +
                "(e.g. flashlight). Pass the natural-language command, e.g. \"open camera\", " +
                "\"battery\", \"12 * 7\".",
        parameters = stringParam("command", "The device command to run, in natural language."),
        trust = ActionRisk.AUTO,
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult {
        val command = args["command"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        if (command.isBlank()) return result("missing 'command'", isError = true)
        val reply = onCommand(command)
            ?: return result("Not a device command: \"$command\".", isError = true)
        return result(reply)
    }

    private fun result(text: String, isError: Boolean = false) =
        ContentPart.ToolResult("?", spec.name, listOf(ContentPart.Text(text)), isError = isError)

    private fun stringParam(name: String, description: String): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject(name) {
                put("type", "string")
                put("description", description)
            }
        }
    }
}
