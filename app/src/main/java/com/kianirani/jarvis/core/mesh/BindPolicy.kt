package com.kianirani.jarvis.core.mesh

/**
 * W1 — selective bind (PRD §, "وقتی «Accept remote nodes» روشن است، Brain روی 0.0.0.0 bind شود
 * — نه فقط loopback — + هشدار امنیتی + توکن اجباری"). The pure decision of which address the
 * Brain-Lite server binds to and whether a token is mandatory, from the user's settings — so the
 * server is loopback-only (private) by default and only exposed on the LAN when the user opts in
 * AND a token is set. Pure → JVM-tested; actually binding the socket is the server half.
 */
data class BindDecision(val address: String, val requireToken: Boolean, val warn: Boolean)

object BindPolicy {

    const val LOOPBACK = "127.0.0.1"
    const val ALL_INTERFACES = "0.0.0.0"

    /**
     * Decide the bind. Remote nodes off → loopback only (no warning, no token needed). Remote
     * nodes on → bind all interfaces, **require a token**, and warn (exposing the brain on the
     * LAN). If the user enabled remote but set no token, we still require one and warn loudly.
     */
    fun decide(acceptRemoteNodes: Boolean, hasToken: Boolean): BindDecision = when {
        !acceptRemoteNodes -> BindDecision(LOOPBACK, requireToken = false, warn = false)
        else -> BindDecision(ALL_INTERFACES, requireToken = true, warn = true)
    }

    /** True if it's safe to actually start listening on the LAN (remote on AND a token exists). */
    fun canExpose(acceptRemoteNodes: Boolean, hasToken: Boolean): Boolean = acceptRemoteNodes && hasToken
}
