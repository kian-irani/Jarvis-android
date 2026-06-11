package com.kianirani.jarvis.brain.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

/** A brain found on the local network via mDNS. */
data class BrainCandidate(val name: String, val host: String, val port: Int)

/**
 * mDNS advertise/scan for Brain Discovery (ROADMAP Phase 1), wrapping
 * [NsdManager]. The brain advertises [SERVICE_TYPE]; joining devices scan and
 * surface [BrainCandidate]s in the Setup Wizard. Android-framework bound —
 * covered by instrumented tests only (JVM suite exercises JoinPayload instead).
 */
class NsdDiscovery(context: Context) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var registration: NsdManager.RegistrationListener? = null
    private var discovery: NsdManager.DiscoveryListener? = null

    fun advertise(name: String, port: Int) {
        stopAdvertise()
        val info = NsdServiceInfo().apply {
            serviceName = name
            serviceType = SERVICE_TYPE
            setPort(port)
        }
        registration = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {}
            override fun onRegistrationFailed(i: NsdServiceInfo, error: Int) {}
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, error: Int) {}
        }.also { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, it) }
    }

    fun stopAdvertise() {
        registration?.let { runCatching { nsd.unregisterService(it) } }
        registration = null
    }

    // NsdManager allows only ONE concurrent resolve below API 34 — serialize via queue.
    private val resolveQueue = ArrayDeque<NsdServiceInfo>()
    private var resolving = false

    private fun resolveNext(onFound: (BrainCandidate) -> Unit) {
        val next = synchronized(resolveQueue) {
            if (resolving) return
            resolveQueue.removeFirstOrNull()?.also { resolving = true }
        } ?: return
        nsd.resolveService(
            next,
            object : NsdManager.ResolveListener {
                override fun onServiceResolved(r: NsdServiceInfo) {
                    r.host?.hostAddress?.let { onFound(BrainCandidate(r.serviceName, it, r.port)) }
                    synchronized(resolveQueue) { resolving = false }
                    resolveNext(onFound)
                }
                override fun onResolveFailed(r: NsdServiceInfo, error: Int) {
                    synchronized(resolveQueue) { resolving = false }
                    resolveNext(onFound)
                }
            },
        )
    }

    fun discover(onFound: (BrainCandidate) -> Unit, onLost: (String) -> Unit = {}) {
        stopDiscovery()
        discovery = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(s: NsdServiceInfo) {
                synchronized(resolveQueue) { resolveQueue.addLast(s) }
                resolveNext(onFound)
            }
            override fun onServiceLost(s: NsdServiceInfo) = onLost(s.serviceName)
            override fun onDiscoveryStarted(type: String) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onStartDiscoveryFailed(type: String, error: Int) {}
            override fun onStopDiscoveryFailed(type: String, error: Int) {}
        }.also { nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, it) }
    }

    fun stopDiscovery() {
        discovery?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        discovery = null
    }

    companion object {
        const val SERVICE_TYPE = "_visionbrain._tcp."
    }
}
