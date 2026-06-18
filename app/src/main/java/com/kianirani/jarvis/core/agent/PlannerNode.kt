package com.kianirani.jarvis.core.agent

import com.kianirani.jarvis.core.graph.ActionPlan
import com.kianirani.jarvis.core.graph.GraphState
import com.kianirani.jarvis.core.graph.Node
import com.kianirani.jarvis.core.graph.NodeContext
import com.kianirani.jarvis.core.graph.NodeResult
import com.kianirani.jarvis.core.graph.PlanStep
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.StateUpdate
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * VCF-A3 — model-backed task decomposition (PRD §6.3), the typed successor to the
 * string-split TaskPlanner. Asks the model for a JSON plan, parses it into a typed
 * [ActionPlan], and writes it to state (the REPLACE plan channel). Robust: a missing or
 * garbled plan falls back to a single-step plan for the goal, so the agent always has
 * something to act on.
 */
class PlannerNode(
    private val client: ModelClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : Node {
    override suspend fun run(state: GraphState, ctx: NodeContext): NodeResult {
        val goal = state.messages.lastOrNull { it.role == Role.USER }?.text().orEmpty()
        if (goal.isBlank()) return NodeResult.Continue()
        val raw = runCatching { client.complete(listOf(planPrompt(goal)), null).message.text() }.getOrDefault("")
        return NodeResult.Continue(StateUpdate(plan = parsePlan(goal, raw)))
    }

    private fun planPrompt(goal: String): VisionMessage = VisionMessage.text(
        Role.USER,
        "Break this goal into an ordered JSON plan and reply with ONLY JSON of the form " +
            "{\"steps\":[{\"id\":\"s1\",\"instruction\":\"...\"}]}. Goal: $goal",
    )

    /** Parse `{"steps":[{id,instruction}]}`; fall back to a single-step plan. Pure + tested. */
    fun parsePlan(goal: String, raw: String): ActionPlan {
        val steps = runCatching {
            val obj = json.parseToJsonElement(extractJson(raw)).jsonObject
            obj.getValue("steps").jsonArray.mapIndexedNotNull { i, element ->
                val o = element.jsonObject
                val instruction = o["instruction"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
                PlanStep(id = o["id"]?.jsonPrimitive?.contentOrNull ?: "s${i + 1}", instruction = instruction)
            }
        }.getOrDefault(emptyList())
        return if (steps.isEmpty()) ActionPlan(goal, listOf(PlanStep("s1", goal))) else ActionPlan(goal, steps)
    }

    private fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else raw
    }
}
