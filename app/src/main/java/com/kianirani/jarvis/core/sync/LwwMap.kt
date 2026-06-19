package com.kianirani.jarvis.core.sync

/**
 * DS-C3 — cross-device sync (PRD §, "memory/clipboard/handoff روی mesh؛ CRDT برای stateِ
 * چیدمان"). A **state-based Last-Writer-Wins map CRDT**: each key holds a value stamped with
 * a logical timestamp + the id of the device that wrote it. Two replicas that drift (the
 * phone reorders the home grid offline while the desktop edits the same layout) can [merge]
 * to a single agreed state with no central server and no coordination.
 *
 * Merge is a semilattice join, so it is **commutative, associative, and idempotent** — apply
 * peers' states in any order, repeatedly, and converge to the same result. Deletes are
 * tombstones (a null value with a timestamp) so a delete can win over an older write and is
 * not silently resurrected by re-merging a stale replica.
 *
 * Pure Kotlin, no Android/clock deps (the caller supplies timestamps — a Lamport/HLC clock
 * or wall-clock millis) → JVM-tested. Persisting/transmitting [snapshot] over the mesh
 * (DS-C network plane) is the on-device half.
 */
class LwwMap<V> {

    /** A value (or tombstone) tagged with the write's logical time and origin device. */
    data class Versioned<V>(val value: V?, val timestamp: Long, val nodeId: String) {
        val isTombstone: Boolean get() = value == null
    }

    private val entries = HashMap<String, Versioned<V>>()

    /**
     * True when [candidate] should replace [current] — strictly newer timestamp wins; equal
     * timestamps break the tie by the higher [nodeId] so every replica decides identically
     * (without this, concurrent equal-stamp writes would diverge).
     */
    private fun dominates(candidate: Versioned<V>, current: Versioned<V>?): Boolean {
        if (current == null) return true
        if (candidate.timestamp != current.timestamp) return candidate.timestamp > current.timestamp
        return candidate.nodeId > current.nodeId
    }

    private fun apply(key: String, incoming: Versioned<V>) {
        if (dominates(incoming, entries[key])) entries[key] = incoming
    }

    /** Record a write of [value] to [key] from [nodeId] at logical time [timestamp]. */
    fun put(key: String, value: V, timestamp: Long, nodeId: String) =
        apply(key, Versioned(value, timestamp, nodeId))

    /** Record a delete of [key] (a tombstone) from [nodeId] at logical time [timestamp]. */
    fun remove(key: String, timestamp: Long, nodeId: String) =
        apply(key, Versioned(null, timestamp, nodeId))

    /** The current value for [key], or null if absent or deleted. */
    operator fun get(key: String): V? = entries[key]?.value

    /** Live (non-tombstoned) entries — the materialized layout/state map. */
    fun value(): Map<String, V> =
        entries.entries.asSequence()
            .filter { !it.value.isTombstone }
            .associate { it.key to it.value.value!! }

    /** Full internal state including tombstones — what you ship to a peer to merge. */
    fun snapshot(): Map<String, Versioned<V>> = entries.toMap()

    /**
     * Fold [other]'s state into this one, keeping the dominant version of every key. Returns
     * `this` for chaining. Commutative/associative/idempotent.
     */
    fun merge(other: LwwMap<V>): LwwMap<V> {
        other.entries.forEach { (k, v) -> apply(k, v) }
        return this
    }

    companion object {
        /** Rebuild a map from a peer's [snapshot] (e.g. decoded off the wire). */
        fun <V> from(snapshot: Map<String, Versioned<V>>): LwwMap<V> =
            LwwMap<V>().also { m -> snapshot.forEach { (k, v) -> m.apply(k, v) } }
    }
}
