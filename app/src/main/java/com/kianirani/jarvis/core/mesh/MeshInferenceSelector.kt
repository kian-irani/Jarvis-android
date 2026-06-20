package com.kianirani.jarvis.core.mesh

/**
 * MX3 — MeshModelClient target selection (PRD §, "ارسال inference به /chat نودِ منتخب … انتخاب
 * نود با BrainScore × AvailabilityGraph"). Given the model a request wants and the mesh nodes
 * advertising it (MX1) with their live BrainScore × availability, pick the node to send the `/chat`
 * to and produce its address. Pure selection (the HTTP call is the network half, e.g. via
 * [com.kianirani.jarvis.data.stream.StreamClient]) → JVM-tested.
 */
data class MeshTarget(val nodeId: String, val host: String, val port: Int, val model: String) {
    val baseUrl: String get() = "http://$host:$port"
}

data class InferenceNode(
    val id: String,
    val host: String,
    val port: Int,
    val models: Set<String>,
    /** BrainScore × availability, higher = better (0 = unusable). */
    val score: Double,
    val online: Boolean = true,
)

object MeshInferenceSelector {

    /** Online nodes that serve [model] with a positive score, best first (ties by id). */
    fun candidates(nodes: List<InferenceNode>, model: String): List<InferenceNode> =
        nodes.filter { it.online && it.score > 0.0 && model in it.models }
            .sortedWith(compareByDescending<InferenceNode> { it.score }.thenBy { it.id })

    /** The best target for [model], or null if no node can serve it. */
    fun select(nodes: List<InferenceNode>, model: String): MeshTarget? =
        candidates(nodes, model).firstOrNull()?.let { MeshTarget(it.id, it.host, it.port, model) }
}
