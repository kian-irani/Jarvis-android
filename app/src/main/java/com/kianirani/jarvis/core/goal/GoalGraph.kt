package com.kianirani.jarvis.core.goal

import kotlinx.serialization.Serializable

/**
 * A4 — Goal system + autonomous planner (Agent-OS §, "Goal را خودکار به فاز/تسک/زیرتسک با
 * وابستگی بشکند و progress را دنبال کند"). A dependency graph of tasks: it orders them so
 * prerequisites run first, surfaces what's actionable now (all deps done), and tracks progress.
 * Distinct from `TaskPlanner` (CF3, which turns one goal into a flat step list) — this adds the
 * dependency/topology + progress layer. Pure → JVM-tested; turning a natural goal into tasks
 * (the model) and executing them (the agent) are the on-device half.
 */
enum class TaskStatus { TODO, IN_PROGRESS, DONE, BLOCKED }

@Serializable
data class GoalTask(
    val id: String,
    val title: String,
    val deps: Set<String> = emptySet(),
    val status: TaskStatus = TaskStatus.TODO,
)

class GoalGraph(tasks: List<GoalTask>) {

    private val byId: Map<String, GoalTask> = tasks.associateBy { it.id }
    private val tasks: List<GoalTask> = tasks

    fun task(id: String): GoalTask? = byId[id]

    fun all(): List<GoalTask> = tasks

    /** True if every *known* dependency of [t] is DONE (unknown ids are ignored, like topoSort). */
    private fun depsSatisfied(t: GoalTask): Boolean =
        t.deps.all { byId[it] == null || byId[it]?.status == TaskStatus.DONE }

    /** Tasks that can start now: still TODO and every dependency is DONE. */
    fun nextActionable(): List<GoalTask> =
        tasks.filter { it.status == TaskStatus.TODO && depsSatisfied(it) }

    /** Unfinished tasks held back by an unmet dependency. */
    fun blocked(): List<GoalTask> =
        tasks.filter { it.status != TaskStatus.DONE && !depsSatisfied(it) }

    /** Fraction of tasks DONE, 0f..1f (0f for an empty goal). */
    fun progress(): Float =
        if (tasks.isEmpty()) 0f else tasks.count { it.status == TaskStatus.DONE }.toFloat() / tasks.size

    fun isComplete(): Boolean = tasks.isNotEmpty() && tasks.all { it.status == TaskStatus.DONE }

    /** True if the dependency graph has a cycle (no valid execution order). */
    fun hasCycle(): Boolean = topoSort() == null

    /**
     * Dependency-respecting execution order (prerequisites first), or empty if the graph has a
     * cycle. Ties (independent tasks) keep declaration order for determinism.
     */
    fun order(): List<GoalTask> = topoSort() ?: emptyList()

    private fun topoSort(): List<GoalTask>? {
        val indegree = tasks.associate { it.id to it.deps.count { d -> byId.containsKey(d) } }.toMutableMap()
        // dependents[d] = tasks that depend on d
        val dependents = HashMap<String, MutableList<String>>()
        tasks.forEach { t -> t.deps.filter { byId.containsKey(it) }.forEach { d -> dependents.getOrPut(d) { mutableListOf() }.add(t.id) } }

        val ready = ArrayDeque(tasks.filter { indegree[it.id] == 0 }.map { it.id })
        val result = mutableListOf<GoalTask>()
        while (ready.isNotEmpty()) {
            val id = ready.removeFirst()
            byId[id]?.let { result.add(it) }
            dependents[id]?.forEach { dep ->
                val v = (indegree[dep] ?: 0) - 1
                indegree[dep] = v
                if (v == 0) ready.addLast(dep)
            }
        }
        return if (result.size == tasks.size) result else null // leftover ⇒ cycle
    }
}
