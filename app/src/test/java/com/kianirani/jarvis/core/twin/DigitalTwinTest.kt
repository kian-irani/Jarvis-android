package com.kianirani.jarvis.core.twin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TWIN acceptance: copy-on-write updates accumulate the user model, usage ranks apps, merge
 * is order-independent across devices (newer scalars win, counts sum, sets union), and the
 * summary is prompt-ready. Pure, no device.
 */
class DigitalTwinTest {

    @Test fun `preferences and usage accumulate immutably`() {
        var t = DigitalTwin()
        t = DigitalTwinOps.setPreference(t, "language", "fa", now = 1)
        t = DigitalTwinOps.recordUsage(t, "telegram", now = 2)
        t = DigitalTwinOps.recordUsage(t, "telegram", now = 3)
        assertEquals("fa", t.preferences["language"])
        assertEquals(2, t.usage["telegram"])
        assertEquals(3L, t.updatedAtMillis)
    }

    @Test fun `topApps ranks by usage then id`() {
        var t = DigitalTwin()
        repeat(3) { t = DigitalTwinOps.recordUsage(t, "maps", 1) }
        repeat(5) { t = DigitalTwinOps.recordUsage(t, "mail", 1) }
        t = DigitalTwinOps.recordUsage(t, "calc", 1)
        assertEquals(listOf("mail", "maps", "calc"), DigitalTwinOps.topApps(t, 3))
    }

    @Test fun `putRoutine replaces by id and addProject dedupes`() {
        var t = DigitalTwin()
        t = DigitalTwinOps.putRoutine(t, Routine("morning", "commute music", 8 * 60), 1)
        t = DigitalTwinOps.putRoutine(t, Routine("morning", "gym", 7 * 60), 2) // same id replaces
        assertEquals(1, t.routines.size)
        assertEquals("gym", t.routines.single().description)
        t = DigitalTwinOps.addProject(t, "vision", 3)
        t = DigitalTwinOps.addProject(t, "vision", 4) // dup ignored
        assertEquals(listOf("vision"), t.projects)
    }

    @Test fun `merge is order-independent — newer scalars win, counts sum, sets union`() {
        val phone = DigitalTwin(
            preferences = mapOf("theme" to "dark"),
            projects = listOf("vision"),
            usage = mapOf("telegram" to 3),
            updatedAtMillis = 10,
        )
        val desktop = DigitalTwin(
            preferences = mapOf("theme" to "light", "font" to "Inter"),
            projects = listOf("kicdn"),
            usage = mapOf("telegram" to 2, "code" to 5),
            updatedAtMillis = 20, // newer
        )
        val ab = DigitalTwinOps.merge(phone, desktop)
        val ba = DigitalTwinOps.merge(desktop, phone)
        assertEquals(ab, ba) // order-independent
        assertEquals("light", ab.preferences["theme"]) // newer (desktop) wins
        assertEquals("Inter", ab.preferences["font"])
        assertEquals(5, ab.usage["telegram"]) // 3 + 2 summed
        assertEquals(setOf("vision", "kicdn"), ab.projects.toSet())
        assertEquals(20L, ab.updatedAtMillis)
    }

    @Test fun `summary is a compact prompt fragment`() {
        var t = DigitalTwin()
        t = DigitalTwinOps.setPreference(t, "language", "fa", 1)
        t = DigitalTwinOps.addProject(t, "vision", 2)
        t = DigitalTwinOps.upsertContact(t, ContactFact("mom", "family"), 3)
        repeat(4) { t = DigitalTwinOps.recordUsage(t, "telegram", 4) }
        val s = DigitalTwinOps.summary(t)
        assertTrue(s.contains("language=fa"))
        assertTrue(s.contains("vision"))
        assertTrue(s.contains("mom"))
        assertTrue(s.contains("telegram"))
    }

    @Test fun `an empty twin summarizes to an empty string`() {
        assertEquals("", DigitalTwinOps.summary(DigitalTwin()))
    }
}
