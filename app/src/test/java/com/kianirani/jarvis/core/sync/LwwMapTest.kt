package com.kianirani.jarvis.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * DS-C3 acceptance: the LWW map converges regardless of merge order, lets newer writes and
 * deletes win, breaks concurrent ties deterministically, and is idempotent. Pure CRDT, no
 * device.
 */
class LwwMapTest {

    @Test fun `newer write wins over older for the same key`() {
        val m = LwwMap<String>()
        m.put("slot1", "calc", timestamp = 1, nodeId = "phone")
        m.put("slot1", "maps", timestamp = 2, nodeId = "phone")
        assertEquals("maps", m["slot1"])
    }

    @Test fun `an older write cannot overwrite a newer one`() {
        val m = LwwMap<String>()
        m.put("slot1", "maps", timestamp = 5, nodeId = "phone")
        m.put("slot1", "calc", timestamp = 3, nodeId = "phone") // stale, arrives late
        assertEquals("maps", m["slot1"])
    }

    @Test fun `delete tombstone wins over an older write and survives re-merge of a stale replica`() {
        val live = LwwMap<String>().apply { put("slot1", "calc", 1, "phone") }
        val deleter = LwwMap<String>().apply { remove("slot1", 2, "desktop") }
        live.merge(deleter)
        assertNull(live["slot1"])
        // re-merging the original stale replica must NOT resurrect the deleted entry
        live.merge(LwwMap<String>().apply { put("slot1", "calc", 1, "phone") })
        assertNull(live["slot1"])
    }

    @Test fun `concurrent equal-timestamp writes resolve to the higher nodeId on both replicas`() {
        val a = LwwMap<String>().apply { put("slot1", "A", 1, "alpha") }
        val b = LwwMap<String>().apply { put("slot1", "B", 1, "beta") }
        val ab = LwwMap<String>().merge(a).merge(b)
        val ba = LwwMap<String>().merge(b).merge(a)
        assertEquals("B", ab["slot1"]) // "beta" > "alpha"
        assertEquals(ab["slot1"], ba["slot1"]) // order-independent
    }

    @Test fun `merge is commutative across many keys`() {
        val phone = LwwMap<String>().apply {
            put("s1", "calc", 3, "phone"); put("s2", "maps", 1, "phone")
        }
        val desktop = LwwMap<String>().apply {
            put("s1", "notes", 2, "desktop"); put("s3", "mail", 4, "desktop")
        }
        val forward = LwwMap<String>().merge(phone).merge(desktop).value()
        val backward = LwwMap<String>().merge(desktop).merge(phone).value()
        assertEquals(forward, backward)
        assertEquals(mapOf("s1" to "calc", "s2" to "maps", "s3" to "mail"), forward)
    }

    @Test fun `merge is idempotent`() {
        val base = LwwMap<String>().apply { put("s1", "calc", 1, "phone"); remove("s2", 2, "phone") }
        val once = LwwMap<String>().merge(base)
        val twice = LwwMap<String>().merge(base).merge(base)
        assertEquals(once.value(), twice.value())
        assertEquals(once.snapshot(), twice.snapshot())
    }

    @Test fun `value omits tombstones and from() round-trips a snapshot`() {
        val m = LwwMap<String>().apply {
            put("s1", "calc", 1, "phone"); put("s2", "maps", 1, "phone"); remove("s2", 2, "phone")
        }
        assertEquals(mapOf("s1" to "calc"), m.value())
        val rebuilt = LwwMap.from(m.snapshot())
        assertEquals(m.value(), rebuilt.value())
        assertNull(rebuilt["s2"])
    }
}
