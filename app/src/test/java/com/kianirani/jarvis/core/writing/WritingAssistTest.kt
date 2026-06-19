package com.kianirani.jarvis.core.writing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PAW acceptance: the gate suggests only for editable, non-sensitive fields with enough
 * content, and supplies the improve prompt when eligible. Pure.
 */
class WritingAssistTest {

    @Test fun `an eligible message is suggested with a prompt`() {
        val d = WritingAssist.evaluate(FieldContext("hey can we meet tomorrow", isEditable = true))
        assertTrue(d.shouldSuggest)
        assertTrue(d.prompt!!.contains("hey can we meet tomorrow"))
    }

    @Test fun `read-only fields are skipped`() {
        val d = WritingAssist.evaluate(FieldContext("some long enough text", isEditable = false))
        assertFalse(d.shouldSuggest)
        assertNull(d.prompt)
        assertEquals("field is read-only", d.reason)
    }

    @Test fun `password and otp fields are never assisted`() {
        assertFalse(WritingAssist.evaluate(FieldContext("hunter2 secret value", true, "password")).shouldSuggest)
        assertFalse(WritingAssist.evaluate(FieldContext("123456 code here", true, "otpCode")).shouldSuggest)
    }

    @Test fun `too-short input is skipped`() {
        val d = WritingAssist.evaluate(FieldContext("ok", isEditable = true))
        assertFalse(d.shouldSuggest)
        assertEquals("too short", d.reason)
    }

    @Test fun `blank input is skipped`() {
        assertFalse(WritingAssist.evaluate(FieldContext("    ", isEditable = true)).shouldSuggest)
    }

    @Test fun `minWords is configurable`() {
        val field = FieldContext("two words", isEditable = true)
        assertFalse(WritingAssist.evaluate(field, minWords = 3).shouldSuggest)
        assertTrue(WritingAssist.evaluate(field, minWords = 2).shouldSuggest)
    }
}
