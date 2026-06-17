package com.kianirani.jarvis.core.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceLearnerTest {

    @Test fun `first two rejections do not blacklist`() {
        val pl = PreferenceLearner()
        assertFalse(pl.reject("neumorphism"))
        assertFalse(pl.reject("neumorphism"))
    }

    @Test fun `third rejection blacklists`() {
        val pl = PreferenceLearner()
        pl.reject("neumorphism"); pl.reject("neumorphism")
        assertTrue(pl.reject("neumorphism"))
        assertTrue(pl.isBlacklisted("neumorphism"))
        assertEquals(3, pl.rejections("neumorphism"))
    }

    @Test fun `unknown key is not blacklisted`() {
        val pl = PreferenceLearner()
        assertFalse(pl.isBlacklisted("brutalism"))
        assertEquals(0, pl.rejections("brutalism"))
    }

    @Test fun `custom threshold blacklists sooner`() {
        val pl = PreferenceLearner(threshold = 2)
        assertFalse(pl.reject("x"))
        assertTrue(pl.reject("x"))
    }

    @Test fun `reset clears a key`() {
        val pl = PreferenceLearner()
        repeat(3) { pl.reject("x") }
        assertTrue(pl.isBlacklisted("x"))
        pl.reset("x")
        assertFalse(pl.isBlacklisted("x"))
        assertEquals(0, pl.rejections("x"))
    }
}
