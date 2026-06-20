package com.kianirani.jarvis.core.mesh

import kotlinx.serialization.Serializable

/**
 * W4 — "Add remote server" + W6 security. The pure model + validation for manually adding a WAN
 * brain node by host/port/token (not just QR/mDNS), plus token-rotation bookkeeping (W6). Pure →
 * JVM-tested; the connection test + persistence + TLS are the device/network half.
 */
@Serializable
data class RemoteServer(val host: String, val port: Int, val token: String, val label: String = "")

object RemoteServerValidator {

    private val HOST = Regex("""^[A-Za-z0-9._-]+$""") // hostname or IPv4 (no scheme/spaces)

    /** Validation errors (empty = valid): host shape, port range, token non-blank. */
    fun validate(server: RemoteServer): List<String> = buildList {
        if (server.host.isBlank() || !HOST.matches(server.host.trim())) add("invalid host")
        if (server.port !in 1..65535) add("port out of range")
        if (server.token.isBlank()) add("token is required")
    }

    fun isValid(server: RemoteServer): Boolean = validate(server).isEmpty()

    /** The `host:port` address to connect to (after validation). */
    fun address(server: RemoteServer): String = "${server.host.trim()}:${server.port}"
}

/**
 * W6 — token rotation. Decides when a node's auth token should be rotated (age-based) so a leaked
 * token has a bounded lifetime. Pure (caller supplies "now") → JVM-tested.
 */
object TokenRotation {

    const val DEFAULT_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000 // 7 days

    /** True if [issuedAtMillis] is older than [maxAgeMillis] and should be rotated. */
    fun shouldRotate(issuedAtMillis: Long, now: Long, maxAgeMillis: Long = DEFAULT_MAX_AGE_MS): Boolean =
        now - issuedAtMillis >= maxAgeMillis

    /** Milliseconds until the token must rotate (0 once due). */
    fun remainingMillis(issuedAtMillis: Long, now: Long, maxAgeMillis: Long = DEFAULT_MAX_AGE_MS): Long =
        (issuedAtMillis + maxAgeMillis - now).coerceAtLeast(0)
}
