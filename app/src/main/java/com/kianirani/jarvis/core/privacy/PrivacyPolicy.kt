package com.kianirani.jarvis.core.privacy

import com.kianirani.jarvis.core.mesh.RouteDecision
import com.kianirani.jarvis.core.mesh.Sensitivity

/**
 * LM5 — Privacy Mode (PRD §, "اجبارِ local-only … نمایش در Trust gate"). When the user turns on
 * local-only, **nothing may leave the device/mesh**: every task is treated as PRIVATE and any
 * cloud route is refused. This pure policy plugs into the mesh router (MX4): feed
 * [effectiveSensitivity] into `MeshRoutingPolicy.route`, and [sanitize] any decision as a
 * belt-and-suspenders guard. Pure → JVM-tested; the toggle UI + Trust-gate badge are on-device.
 */
object PrivacyPolicy {

    /** Local-only forces PRIVATE; otherwise the caller's requested sensitivity stands. */
    fun effectiveSensitivity(requested: Sensitivity, localOnly: Boolean): Sensitivity =
        if (localOnly) Sensitivity.PRIVATE else requested

    /** Whether a cloud route is permitted at all. */
    fun allowsCloud(localOnly: Boolean): Boolean = !localOnly

    /**
     * Guard a route decision: under local-only a [RouteDecision.Cloud] is never allowed and is
     * downgraded to [RouteDecision.Unavailable] (the caller then surfaces "needs local model /
     * mesh"). All non-cloud decisions pass through unchanged.
     */
    fun sanitize(decision: RouteDecision, localOnly: Boolean): RouteDecision =
        if (localOnly && decision is RouteDecision.Cloud) RouteDecision.Unavailable else decision

    /** Trust-gate label for the current mode. */
    fun label(localOnly: Boolean): String =
        if (localOnly) "Private — on-device / mesh only" else "Standard — cloud allowed"
}
