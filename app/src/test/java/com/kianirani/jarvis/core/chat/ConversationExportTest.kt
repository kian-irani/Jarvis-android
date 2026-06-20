package com.kianirani.jarvis.core.chat

import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Conversation export acceptance: markdown/plain formatting, system turns dropped. Pure. */
class ConversationExportTest {

    private val msgs = listOf(
        VisionMessage.text(Role.SYSTEM, "rules"),
        VisionMessage.text(Role.USER, "hello"),
        VisionMessage.text(Role.ASSISTANT, "hi there"),
    )

    @Test fun `markdown labels speakers and drops system by default`() {
        val md = ConversationExport.toMarkdown(msgs)
        assertTrue(md.contains("**You:** hello"))
        assertTrue(md.contains("**Vision:** hi there"))
        assertFalse(md.contains("rules"))
    }

    @Test fun `plain text format`() {
        assertEquals("You: hello\nVision: hi there", ConversationExport.toPlainText(msgs))
    }

    @Test fun `includeSystem keeps system turns`() {
        assertTrue(ConversationExport.toPlainText(msgs, includeSystem = true).contains("System: rules"))
    }

    @Test fun `custom assistant name`() {
        assertTrue(ConversationExport.toPlainText(msgs, assistantName = "Jarvis").contains("Jarvis: hi there"))
    }
}
