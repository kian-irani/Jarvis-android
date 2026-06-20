package com.kianirani.jarvis.core.email

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PAE acceptance: mailto URIs encode subject/body, omit blanks, and recipient validation. Pure. */
class EmailComposerTest {

    @Test fun `full draft builds an encoded mailto`() {
        val uri = EmailComposer.mailtoUri(EmailDraft("ali@example.com", "Meeting", "see you at 5"))
        assertEquals("mailto:ali@example.com?subject=Meeting&body=see%20you%20at%205", uri)
    }

    @Test fun `blank subject and body are omitted`() {
        assertEquals("mailto:x@y.com", EmailComposer.mailtoUri(EmailDraft("x@y.com")))
        assertEquals("mailto:x@y.com?body=hi", EmailComposer.mailtoUri(EmailDraft("x@y.com", body = "hi")))
    }

    @Test fun `encode percent-encodes specials`() {
        assertEquals("a%26b%3Dc", EmailComposer.encode("a&b=c"))
    }

    @Test fun `recipient validation`() {
        assertTrue(EmailComposer.isValidRecipient("a@b.com"))
        assertFalse(EmailComposer.isValidRecipient("nope"))
        assertFalse(EmailComposer.isValidRecipient("a@@b.com"))
        assertFalse(EmailComposer.isValidRecipient("a@b."))
    }
}
