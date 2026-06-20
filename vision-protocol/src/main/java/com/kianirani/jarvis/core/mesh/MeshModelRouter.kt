package com.kianirani.jarvis.core.mesh

/**
 * MX1 — mesh model advertisement + selection (PRD §14). Each mesh node advertises the local
 * models it can run (e.g. Ollama tags) alongside its live resources; this router picks the
 * **best node to run a requested model on**, so a phone with no local model can borrow a
 * beefier desktop/VPS peer instead of falling back to the cloud.
 *
 * Pure & deterministic → JVM-tested. The heartbeat/registry that keeps [MeshNode]s fresh and
 * the actual remote inference call are the on-device/network half (extends `NodeMetrics`).
 */
data class MeshNode(
    val id: String,
    /** Advertised local model tags this node can serve. */
    val models: Set<String>,
    val freeRamGb: Float,
    /** Current CPU load, 0f (idle) .. 1f (saturated). */
    val cpuLoad: Float,
    val online: Boolean = true,
)

object MeshModelRouter {

    /** Online nodes that advertise [model], in input order. */
    fun candidates(nodes: List<MeshNode>, model: String): List<MeshNode> =
        nodes.filter { it.online && model in it.models }

    /**
     * Fitness of a node for running a model: more free RAM and lower CPU load score higher. A
     * saturated node (load ≥ 1) or one with no free RAM scores 0 — never picked while a better
     * peer exists.
     */
    fun score(node: MeshNode): Double =
        node.freeRamGb.coerceAtLeast(0f) * (1.0 - node.cpuLoad.coerceIn(0f, 1f))

    /**
     * The best node to run [model] on, or null if no online node advertises it. Highest score
     * wins; ties break by node id (lexicographically first) so every caller agrees.
     */
    fun best(nodes: List<MeshNode>, model: String): MeshNode? =
        candidates(nodes, model)
            .sortedWith(compareByDescending<MeshNode> { score(it) }.thenBy { it.id })
            .firstOrNull()
}
