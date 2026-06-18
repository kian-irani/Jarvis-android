package com.kianirani.jarvis.core.agent

/** How a [Crew] coordinates its agents (PRD §9, CrewAI process). */
enum class Process { SEQUENTIAL, HIERARCHICAL }

/** The accumulated outputs of a crew run; [finalOutput] is the last agent's answer. */
data class CrewResult(val task: String, val outputs: List<String>) {
    val finalOutput: String get() = outputs.lastOrNull().orEmpty()
}

/**
 * VCF-X2 — multi-agent process control (PRD §9). SEQUENTIAL pipes each agent's output
 * into the next agent's context (a pipeline); HIERARCHICAL has a manager delegate the
 * task to every worker and then synthesize their results. The per-agent execution is
 * injected via [runAgent] (real impl: a ReAct agent built from the [AgentRole] by
 * [ReActAgentFactory] + the VB model node), so the orchestration is unit-tested with
 * fakes — no model or network needed.
 */
class Crew(private val runAgent: suspend (role: AgentRole, input: String) -> String) {

    suspend fun run(
        agents: List<AgentRole>,
        task: String,
        process: Process = Process.SEQUENTIAL,
        manager: AgentRole? = null,
    ): CrewResult = when (process) {
        Process.SEQUENTIAL -> runSequential(agents, task)
        Process.HIERARCHICAL ->
            runHierarchical(agents, requireNotNull(manager) { "hierarchical process requires a manager" }, task)
    }

    private suspend fun runSequential(agents: List<AgentRole>, task: String): CrewResult {
        var context = task
        val outputs = mutableListOf<String>()
        for (role in agents) {
            val out = runAgent(role, context)
            outputs += out
            context = "$context\n[${role.role}] $out" // feed this agent's result forward
        }
        return CrewResult(task, outputs)
    }

    private suspend fun runHierarchical(agents: List<AgentRole>, manager: AgentRole, task: String): CrewResult {
        val outputs = mutableListOf<String>()
        for (role in agents) {
            outputs += runAgent(role, task) // manager delegates the task to each worker
        }
        val synthesis = runAgent(manager, "Synthesize the workers' results for: $task\n${outputs.joinToString("\n")}")
        return CrewResult(task, outputs + synthesis)
    }
}
