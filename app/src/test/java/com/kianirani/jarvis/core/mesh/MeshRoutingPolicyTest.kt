package com.kianirani.jarvis.core.mesh

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MX4 acceptance: private work never goes to cloud, heavy work prefers the strongest mesh
 * peer, light work stays cheap/local, and Unavailable is returned only when nothing can
 * serve. Pure, no network.
 */
class MeshRoutingPolicyTest {

    private val qwen = "qwen2.5:0.5b"
    private val strongMesh = listOf(MeshNode("desktop", setOf(qwen), 16f, 0.1f))
    private val noMesh = emptyList<MeshNode>()

    @Test fun `private work uses the mesh and never the cloud`() {
        val d = MeshRoutingPolicy.route(
            RoutingTask(qwen, Sensitivity.PRIVATE),
            strongMesh,
            RoutingContext(cloudAvailable = true, onDeviceModelReady = true),
        )
        assertEquals(RouteDecision.Mesh("desktop"), d)
    }

    @Test fun `private work with no mesh falls back to on-device, not cloud`() {
        val d = MeshRoutingPolicy.route(
            RoutingTask(qwen, Sensitivity.PRIVATE),
            noMesh,
            RoutingContext(cloudAvailable = true, onDeviceModelReady = true),
        )
        assertEquals(RouteDecision.LocalOnDevice, d)
    }

    @Test fun `private work with neither mesh nor on-device is unavailable (cloud excluded)`() {
        val d = MeshRoutingPolicy.route(
            RoutingTask(qwen, Sensitivity.PRIVATE),
            noMesh,
            RoutingContext(cloudAvailable = true, onDeviceModelReady = false),
        )
        assertEquals(RouteDecision.Unavailable, d)
    }

    @Test fun `heavy normal work prefers the strongest mesh peer`() {
        val d = MeshRoutingPolicy.route(
            RoutingTask(qwen, Sensitivity.NORMAL, heavy = true),
            strongMesh,
            RoutingContext(cloudAvailable = true, onDeviceModelReady = true),
        )
        assertEquals(RouteDecision.Mesh("desktop"), d)
    }

    @Test fun `heavy normal work uses cloud when there is no mesh`() {
        val d = MeshRoutingPolicy.route(
            RoutingTask(qwen, Sensitivity.NORMAL, heavy = true),
            noMesh,
            RoutingContext(cloudAvailable = true, onDeviceModelReady = true),
        )
        assertEquals(RouteDecision.Cloud, d)
    }

    @Test fun `light normal work stays cheap on-device`() {
        val d = MeshRoutingPolicy.route(
            RoutingTask(qwen, Sensitivity.NORMAL, heavy = false),
            strongMesh,
            RoutingContext(cloudAvailable = true, onDeviceModelReady = true),
        )
        assertEquals(RouteDecision.LocalOnDevice, d)
    }

    @Test fun `light work falls to cloud when on-device is not ready`() {
        val d = MeshRoutingPolicy.route(
            RoutingTask(qwen, Sensitivity.NORMAL, heavy = false),
            strongMesh,
            RoutingContext(cloudAvailable = true, onDeviceModelReady = false),
        )
        assertEquals(RouteDecision.Cloud, d)
    }

    @Test fun `light work falls to mesh when neither on-device nor cloud is ready`() {
        val d = MeshRoutingPolicy.route(
            RoutingTask(qwen, Sensitivity.NORMAL, heavy = false),
            strongMesh,
            RoutingContext(cloudAvailable = false, onDeviceModelReady = false),
        )
        assertEquals(RouteDecision.Mesh("desktop"), d)
    }

    @Test fun `nothing available yields Unavailable`() {
        val d = MeshRoutingPolicy.route(
            RoutingTask(qwen),
            noMesh,
            RoutingContext(cloudAvailable = false, onDeviceModelReady = false),
        )
        assertEquals(RouteDecision.Unavailable, d)
    }
}
