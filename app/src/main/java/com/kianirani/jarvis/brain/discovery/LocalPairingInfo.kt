package com.kianirani.jarvis.brain.discovery

import android.content.Context
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

/**
 * Brain-side pairing identity: this device's LAN address + a persistent pairing
 * token, rendered as a QR (spec §5) for other devices to scan.
 */
class LocalPairingInfoProvider(context: Context) {

    private val prefs = context.getSharedPreferences("brain_lite", Context.MODE_PRIVATE)

    /** Persistent pairing token for THIS brain (created once). */
    fun pairToken(): String =
        prefs.getString("pair_token", null) ?: UUID.randomUUID().toString()
            .also { prefs.edit().putString("pair_token", it).commit() }

    /** First non-loopback IPv4 of this device, or null when offline. */
    fun localIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
    }.getOrNull()

    /** QR payload for this brain, or null when no LAN address is available. */
    fun payload(): JoinPayload? = localIp()?.let { JoinPayload(it, 7799, pairToken()) }
}
