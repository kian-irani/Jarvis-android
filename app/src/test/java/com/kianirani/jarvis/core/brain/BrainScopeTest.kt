package com.kianirani.jarvis.core.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** C3 acceptance: personal/shared/org read scoping + visible filter. Pure. */
class BrainScopeTest {

    private val viewer = Viewer("alice", groups = setOf("team-x"), orgs = setOf("acme"))

    @Test fun `personal items only readable by owner`() {
        assertTrue(BrainScope.canRead(viewer, ScopedItem("1", BrainTier.PERSONAL, "alice", "x")))
        assertFalse(BrainScope.canRead(viewer, ScopedItem("2", BrainTier.PERSONAL, "bob", "x")))
    }

    @Test fun `shared items readable by group members`() {
        assertTrue(BrainScope.canRead(viewer, ScopedItem("1", BrainTier.SHARED, "team-x", "x")))
        assertFalse(BrainScope.canRead(viewer, ScopedItem("2", BrainTier.SHARED, "team-y", "x")))
    }

    @Test fun `org items readable by org members`() {
        assertTrue(BrainScope.canRead(viewer, ScopedItem("1", BrainTier.ORG, "acme", "x")))
        assertFalse(BrainScope.canRead(viewer, ScopedItem("2", BrainTier.ORG, "other", "x")))
    }

    @Test fun `visible filters to readable items`() {
        val items = listOf(
            ScopedItem("a", BrainTier.PERSONAL, "alice", "1"),
            ScopedItem("b", BrainTier.PERSONAL, "bob", "2"),
            ScopedItem("c", BrainTier.SHARED, "team-x", "3"),
            ScopedItem("d", BrainTier.ORG, "acme", "4"),
        )
        assertEquals(listOf("a", "c", "d"), BrainScope.visible(viewer, items).map { it.id })
    }
}
