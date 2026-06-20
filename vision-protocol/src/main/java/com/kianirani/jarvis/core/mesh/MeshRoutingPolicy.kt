package com.kianirani.jarvis.core.mesh

/**
 * MX4 — orchestrator mesh routing (PRD §14, completes WAN-Mesh W5). Decides **where a task
 * runs**: a strong mesh peer's local model, the cloud, the phone's own on-device model, or
 * nowhere. Heavy work prefers the strongest peer; **private work never leaves the mesh** (no
 * cloud); light work stays cheap and local. Pure decision logic over [MeshModelRouter] → it's
 * fully JVM-tested; the actual dispatch (remote `/chat`, cloud call, local inference) is the
 * on-device/network half.
 */
enum class Sensitivity { NORMAL, PRIVATE }

data class RoutingTask(val model: String, val sensitivity: Sensitivity = Sensitivity.NORMAL, val heavy: Boolean = false)

/** Live capabilities the policy weighs besides the mesh. */
data class RoutingContext(val cloudAvailable: Boolean, val onDeviceModelReady: Boolean)

sealed interface RouteDecision {
    /** Run on this mesh peer's local model. */
    data class Mesh(val nodeId: String) : RouteDecision

    /** Send to a cloud provider. */
    data object Cloud : RouteDecision

    /** Run on the phone's own on-device model. */
    data object LocalOnDevice : RouteDecision

    /** Nothing can serve this task right now. */
    data object Unavailable : RouteDecision
}

object MeshRoutingPolicy {

    /**
     * Choose a route for [task] given the current mesh [nodes] and [ctx]. Priority:
     * - **PRIVATE:** Mesh → on-device → Unavailable (cloud is never used for private work).
     * - **NORMAL + heavy:** Mesh (most compute) → Cloud → on-device → Unavailable.
     * - **NORMAL + light:** on-device (cheapest) → Cloud → Mesh → Unavailable.
     */
    fun route(task: RoutingTask, nodes: List<MeshNode>, ctx: RoutingContext): RouteDecision {
        val mesh = MeshModelRouter.best(nodes, task.model)?.let { RouteDecision.Mesh(it.id) }
        val cloud = RouteDecision.Cloud.takeIf { ctx.cloudAvailable }
        val onDevice = RouteDecision.LocalOnDevice.takeIf { ctx.onDeviceModelReady }

        val order: List<RouteDecision?> = when {
            task.sensitivity == Sensitivity.PRIVATE -> listOf(mesh, onDevice)
            task.heavy -> listOf(mesh, cloud, onDevice)
            else -> listOf(onDevice, cloud, mesh)
        }
        return order.firstOrNull { it != null } ?: RouteDecision.Unavailable
    }
}
