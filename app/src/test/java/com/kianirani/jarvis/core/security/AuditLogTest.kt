package com.kianirani.jarvis.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Audit log acceptance: newest-first, capacity bound, category filter, secret redaction. Pure. */
class AuditLogTest {

    @Test fun `records newest-first and filters by category`() {
        val log = AuditLog()
        log.record(AuditCategory.TOOL_RUN, "call", atMillis = 1)
        log.record(AuditCategory.MESH, "join", atMillis = 2)
        assertEquals(listOf("join", "call"), log.entries().map { it.action })
        assertEquals(listOf("join"), log.entries(AuditCategory.MESH).map { it.action })
    }

    @Test fun `capacity drops oldest`() {
        val log = AuditLog(capacity = 2)
        (1..3).forEach { log.record(AuditCategory.TOOL_RUN, "a$it", atMillis = it.toLong()) }
        assertEquals(listOf("a3", "a2"), log.entries().map { it.action })
    }

    @Test fun `token-like detail is redacted`() {
        val log = AuditLog()
        log.record(AuditCategory.PERMISSION, "grant", detail = "token sk-ABCDEFGHIJKLMNOPQRSTUV granted", atMillis = 1)
        val detail = log.entries().first().detail
        assertFalse(detail.contains("ABCDEFGHIJKLMNOPQRSTUV"))
        assertTrue(detail.contains("redacted"))
    }

    @Test fun `short normal text is not redacted`() {
        val log = AuditLog()
        log.record(AuditCategory.TOOL_RUN, "open", detail = "opened Maps", atMillis = 1)
        assertEquals("opened Maps", log.entries().first().detail)
    }
}
