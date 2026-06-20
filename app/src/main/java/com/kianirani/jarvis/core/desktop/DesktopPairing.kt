package com.kianirani.jarvis.core.desktop

/**
 * DS-WIN1 — desktop ↔ Brain-Lite pairing + peer election (PRD §, "Compose-MP + pairingِ peerِ
 * Brain-Lite mDNS/QR/election"). The pure logic a desktop peer uses to discover, validate and
 * pick which node hosts the brain: leader election by capability score (highest wins, id breaks
 * ties so every peer agrees) and pairing-token checks. mDNS discovery + the QR/Compose-MP UI are
 * the platform half; this is the deterministic brain → JVM-tested.
 */
data class Peer(val id: String, val host: String, val port: Int, val score: Double, val online: Boolean = true)

object DesktopPairing {

    /**
     * Elect the brain leader from [peers]: the online peer with the highest [Peer.score]; ties
     * break by the lexicographically smallest id so all peers converge on the same leader without
     * coordination. Null if no peer is online.
     */
    fun electLeader(peers: List<Peer>): Peer? =
        peers.filter { it.online }
            .sortedWith(compareByDescending<Peer> { it.score }.thenBy { it.id })
            .firstOrNull()

    /** The `host:port` to connect to for the elected leader, or null. */
    fun leaderAddress(peers: List<Peer>): String? = electLeader(peers)?.let { "${it.host}:${it.port}" }

    /**
     * Validate a pairing token a desktop presents against the brain's [expected] token. Constant
     * in shape (non-blank, exact match) — the QR/mDNS layer transports it; we just gate the join.
     */
    fun isPaired(presented: String?, expected: String): Boolean =
        !presented.isNullOrBlank() && expected.isNotBlank() && presented == expected

    /** A short, human-typeable pairing code from a longer token (first 6 hex-ish chars, upper). */
    fun pairingCode(token: String): String =
        token.filter { it.isLetterOrDigit() }.take(6).uppercase()
}
