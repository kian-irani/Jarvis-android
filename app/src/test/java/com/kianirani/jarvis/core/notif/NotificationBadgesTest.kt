package com.kianirani.jarvis.core.notif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * NEO12 acceptance: badges include only unread apps (most first), dots reflect presence, the
 * display label caps at max, and the total sums unread. Pure.
 */
class NotificationBadgesTest {

    private val counts = mapOf("whatsapp" to 5, "gmail" to 0, "telegram" to 12, "calendar" to 1)

    @Test fun `from lists only unread apps, most first`() {
        val badges = NotificationBadges.from(counts)
        assertEquals(listOf("telegram", "whatsapp", "calendar"), badges.map { it.packageName })
        assertFalse(badges.any { it.packageName == "gmail" })
    }

    @Test fun `hasDot reflects presence of unread`() {
        assertTrue(NotificationBadges.hasDot("whatsapp", counts))
        assertFalse(NotificationBadges.hasDot("gmail", counts))
        assertFalse(NotificationBadges.hasDot("unknown", counts))
    }

    @Test fun `display caps at max and hides zero`() {
        assertEquals("", NotificationBadges.display(0))
        assertEquals("7", NotificationBadges.display(7))
        assertEquals("99+", NotificationBadges.display(150))
        assertEquals("9+", NotificationBadges.display(12, max = 9))
    }

    @Test fun `total sums unread across apps`() {
        assertEquals(18, NotificationBadges.total(counts)) // 5 + 12 + 1
    }

    @Test fun `empty counts yield no badges`() {
        assertTrue(NotificationBadges.from(emptyMap()).isEmpty())
        assertEquals(0, NotificationBadges.total(emptyMap()))
    }
}
