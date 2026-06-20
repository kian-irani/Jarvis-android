package com.kianirani.jarvis.core.notif

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * B4 acceptance: muted apps ignore, VIP senders and urgent keywords escalate, message/call
 * categories rank high, promo/social/ongoing rank low, and `important` filters. Pure.
 */
class NotificationTriageTest {

    private fun n(pkg: String, title: String, text: String, cat: String = "", ongoing: Boolean = false) =
        NotificationInfo(pkg, title, text, cat, ongoing)

    @Test fun `muted package is always ignored`() {
        val policy = TriagePolicy(mutedPackages = setOf("com.spam"))
        assertEquals(Importance.IGNORE, NotificationTriage.classify(n("com.spam", "Mom", "urgent", "msg"), policy))
    }

    @Test fun `vip sender escalates to important`() {
        val policy = TriagePolicy(vipSenders = setOf("mom"))
        assertEquals(Importance.IMPORTANT, NotificationTriage.classify(n("com.wa", "Mom", "hi", "social"), policy))
    }

    @Test fun `urgent keyword escalates to important`() {
        assertEquals(Importance.IMPORTANT, NotificationTriage.classify(n("com.bank", "Bank", "your OTP code is 1234", "msg")))
    }

    @Test fun `message and call categories are important`() {
        assertEquals(Importance.IMPORTANT, NotificationTriage.classify(n("com.wa", "Ali", "are you there?", "msg")))
        assertEquals(Importance.IMPORTANT, NotificationTriage.classify(n("com.dialer", "Unknown", "missed call", "call")))
    }

    @Test fun `promo and social are ignored, ongoing transport is low`() {
        assertEquals(Importance.IGNORE, NotificationTriage.classify(n("com.shop", "Sale", "50% off", "promo")))
        assertEquals(Importance.IGNORE, NotificationTriage.classify(n("com.maps", "Nav", "turn left", "transport", ongoing = true)))
    }

    @Test fun `unknown category is medium`() {
        assertEquals(Importance.MEDIUM, NotificationTriage.classify(n("com.app", "App", "something happened")))
    }

    @Test fun `important filters the list`() {
        val items = listOf(
            n("com.wa", "Ali", "hi", "msg"),
            n("com.shop", "Sale", "big discount today", "promo"),
            n("com.app", "App", "fyi"),
        )
        assertEquals(listOf("com.wa"), NotificationTriage.important(items).map { it.pkg })
    }
}
