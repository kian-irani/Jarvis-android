package com.kianirani.jarvis.core.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

/** C5 acceptance: requests classify to action categories (EN/FA); target extracted. Pure. */
class AppActionResolverTest {

    @Test fun `classifies common actions`() {
        assertEquals(AppAction.CALL, AppActionResolver.classify("call Ali"))
        assertEquals(AppAction.CALL, AppActionResolver.classify("با علی تماس بگیر"))
        assertEquals(AppAction.MESSAGE, AppActionResolver.classify("text mom on whatsapp"))
        assertEquals(AppAction.NAVIGATE, AppActionResolver.classify("navigate to the office"))
        assertEquals(AppAction.MUSIC, AppActionResolver.classify("play some music"))
        assertEquals(AppAction.CAMERA, AppActionResolver.classify("take a photo"))
    }

    @Test fun `unknown when no keyword`() {
        assertEquals(AppAction.UNKNOWN, AppActionResolver.classify("what is the weather"))
    }

    @Test fun `target strips the verb`() {
        assertEquals("Ali", AppActionResolver.target("call Ali"))
        assertEquals("the office", AppActionResolver.target("navigate to the office"))
    }
}
