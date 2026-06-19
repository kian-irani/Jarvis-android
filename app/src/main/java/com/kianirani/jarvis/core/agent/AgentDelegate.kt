package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.tools.ToolContext
import com.kianirani.jarvis.core.tools.ToolSpec
import com.kianirani.jarvis.core.tools.VisionTool
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * VCF-AGT — the **delegation tool** (CrewAI "Delegate work to coworker", PRD §5/§9).
 *
 * Where [AgentAsTool] wraps **one** fixed sub-agent and [Crew] runs **every** agent, this
 * exposes a whole *roster* of named coworkers as a single tool: a manager agent picks a
 * coworker **by name at run time** and hands off a self-contained sub-task; that coworker's
 * final answer returns as the tool result. This is the agent→agent delegation the AGT track
 * calls for — the manager decides *who*, dynamically.
 *
 * The per-agent runner is injected ([runAgent]) — in production a ReAct agent built from the
 * [AgentRole] (ReActAgentFactory + the VB model node), in tests a fake — so the
 * lookup/delegation logic is unit-tested with no model or network. Coworkers are matched
 * case-insensitively on either their [AgentRole.role] label or their [AgentRole.id]. An
 * unknown coworker or a missing argument yields an error result listing the valid names
 * (failure-as-data) so the model self-corrects, never a thrown exception. As with
 * [AgentAsTool], the [ToolNode] stamps the originating call id onto the result.
 */
class AgentDelegate(
    roster: List<AgentRole>,
    private val runAgent: suspend (role: AgentRole, task: String) -> String,
    toolName: String = "delegate_to_coworker",
) : VisionTool {

    /** Lowercased role label *and* id → role, so the manager may use either as the name. */
    private val index: Map<String, AgentRole> =
        roster.flatMap { r -> listOf(r.role.lowercase() to r, r.id.lowercase() to r) }.toMap()

    private val coworkerNames: List<String> = roster.map { it.role }

    override val spec: ToolSpec = ToolSpec(
        name = toolName,
        description = "Delegate a self-contained sub-task to a named coworker agent and get their answer. " +
            "coworker must be exactly one of: ${coworkerNames.joinToString(", ")}.",
        parameters = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("coworker") {
                    put("type", "string")
                    put("description", "the coworker to delegate to, one of: ${coworkerNames.joinToString(", ")}")
                }
                putJsonObject("task") {
                    put("type", "string")
                    put("description", "the self-contained sub-task to hand off, with all context the coworker needs")
                }
            }
            putJsonArray("required") { add("coworker"); add("task") }
        },
        trust = ActionRisk.AUTO,
    )

    override suspend fun execute(args: JsonObject, ctx: ToolContext): ContentPart.ToolResult {
        val coworker = args.strOrNull("coworker")
        val task = args.strOrNull("task")
        if (coworker.isNullOrBlank() || task.isNullOrBlank()) {
            return error("delegate needs both 'coworker' and 'task'. Valid coworkers: ${coworkerNames.joinToString(", ")}.")
        }
        val role = index[coworker.trim().lowercase()]
            ?: return error("Unknown coworker \"$coworker\". Valid coworkers: ${coworkerNames.joinToString(", ")}.")
        val answer = runCatching { runAgent(role, task) }
            .getOrElse { return error("Coworker \"${role.role}\" failed: ${it.message ?: "unknown error"}") }
        return result(answer, isError = false)
    }

    private fun error(message: String) = result(message, isError = true)

    private fun result(text: String, isError: Boolean) = ContentPart.ToolResult(
        callId = PLACEHOLDER_CALL_ID,
        name = spec.name,
        content = listOf(ContentPart.Text(text)),
        isError = isError,
    )

    private fun JsonObject.strOrNull(key: String): String? =
        runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()

    companion object {
        /** [ToolNode] stamps the real originating call id; matches [AgentAsTool]'s contract. */
        const val PLACEHOLDER_CALL_ID = "placeholder"
    }
}
