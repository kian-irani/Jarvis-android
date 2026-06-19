package com.kianirani.jarvis.core.eval

import com.kianirani.jarvis.core.agent.ModelClient
import com.kianirani.jarvis.core.agent.VisionAgent
import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.tools.ToolRegistry

/** One agentic eval case: a prompt, the (fake or real) model/tools, and what we expect. */
data class AgentEvalCase(
    val name: String,
    val prompt: String,
    val model: ModelClient,
    val tools: ToolRegistry,
    val expectedTool: String? = null,
    val mustComplete: Boolean = true,
)

data class AgentEvalResult(val case: String, val passed: Boolean, val detail: String)

data class AgentEvalReport(val results: List<AgentEvalResult>) {
    val total: Int get() = results.size
    val passed: Int get() = results.count { it.passed }
    val passRate: Double get() = if (total == 0) 1.0 else passed.toDouble() / total

    fun describeFailures(): String =
        results.filterNot { it.passed }.joinToString("\n") { "✗ ${it.case}: ${it.detail}" }
}

/**
 * VCF-E1 — golden eval for the assembled agent (PRD §12). Runs each case through a
 * [VisionAgent], traces it ([RunTrace]), and checks that the run completed and the
 * expected tool actually executed. Injected (fake) models let this run in CI without a
 * network — a regression net for the ReAct loop and tool wiring, complementing the
 * intent-classification [EvalHarness].
 */
object AgentEvalHarness {
    suspend fun run(cases: List<AgentEvalCase>): AgentEvalReport =
        AgentEvalReport(cases.map { evaluate(it) })

    private suspend fun evaluate(case: AgentEvalCase): AgentEvalResult {
        val trace = TraceRecorder.record(case.name, VisionAgent(case.model, case.tools).run(case.prompt, case.name))
        return when {
            case.mustComplete && !trace.completed ->
                AgentEvalResult(case.name, false, "did not complete (${endLabel(trace)})")
            case.expectedTool != null && !ranTool(trace, case.expectedTool) ->
                AgentEvalResult(case.name, false, "expected tool '${case.expectedTool}' was not called")
            else -> AgentEvalResult(case.name, true, "ok")
        }
    }

    private fun ranTool(trace: RunTrace, name: String): Boolean =
        trace.finalState?.messages.orEmpty().any { message ->
            message.role == Role.TOOL && message.content.any { it is ContentPart.ToolResult && it.name == name }
        }

    private fun endLabel(trace: RunTrace): String = when {
        trace.interrupted -> "interrupted"
        trace.failed -> "failed"
        else -> "incomplete"
    }
}
