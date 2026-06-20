package com.kianirani.jarvis.core.mesh

/**
 * Multi-node task coordination (PRD §, "هماهنگی task بین نودها"). The pure scheduler that
 * assigns a batch of work items across the available mesh nodes by capacity — least-loaded
 * first, respecting each node's max concurrent slots — so heavy work spreads instead of piling
 * on one peer. Pure → JVM-tested; dispatching over the network is the mesh half.
 */
data class WorkerNode(val id: String, val maxSlots: Int, val activeTasks: Int = 0) {
    val freeSlots: Int get() = (maxSlots - activeTasks).coerceAtLeast(0)
}

object TaskCoordinator {

    /**
     * Assign [tasks] (by id) to nodes, filling the least-loaded node with free capacity first.
     * Returns task-id → node-id. Tasks beyond total free capacity are left unassigned (not in the
     * map) so the caller can queue them. Deterministic: ties break by node id.
     */
    fun assign(tasks: List<String>, nodes: List<WorkerNode>): Map<String, String> {
        // mutable free-slot counters, seeded from current load
        val free = nodes.associate { it.id to it.freeSlots }.toMutableMap()
        val result = LinkedHashMap<String, String>()
        for (task in tasks) {
            val node = nodes
                .filter { (free[it.id] ?: 0) > 0 }
                .maxWithOrNull(compareBy<WorkerNode> { free[it.id] ?: 0 }.thenByDescending { it.id })
                ?: break // no capacity left → remaining tasks queue
            result[task] = node.id
            free[node.id] = (free[node.id] ?: 0) - 1
        }
        return result
    }

    /** Total free capacity across [nodes]. */
    fun totalFreeSlots(nodes: List<WorkerNode>): Int = nodes.sumOf { it.freeSlots }
}
