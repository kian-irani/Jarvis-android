package com.kianirani.jarvis.core.memory

import com.kianirani.jarvis.core.agent.ModelClient
import com.kianirani.jarvis.core.agent.ModelResponse
import com.kianirani.jarvis.core.graph.Role
import com.kianirani.jarvis.core.graph.VisionMessage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationSummarizerTest {

    private fun messages(n: Int) = (1..n).map { VisionMessage.text(Role.USER, "m$it") }

    @Test fun `does not summarize a short conversation and never calls the model`() {
        val summarizer = ConversationSummarizer(ModelClient { _, _ -> throw AssertionError("should not call") }, threshold = 8)
        assertFalse(summarizer.shouldSummarize(messages(8)))
        assertEquals("", runBlocking { summarizer.summarize(messages(8)) })
    }

    @Test fun `summarizes once past the threshold`() {
        val summarizer = ConversationSummarizer(
            ModelClient { _, _ -> ModelResponse(VisionMessage.text(Role.ASSISTANT, "  user likes dark mode  ")) },
            threshold = 8,
        )
        assertTrue(summarizer.shouldSummarize(messages(9)))
        assertEquals("user likes dark mode", runBlocking { summarizer.summarize(messages(9)) })
    }

    @Test fun `a model failure yields an empty summary`() {
        val summarizer = ConversationSummarizer(ModelClient { _, _ -> throw RuntimeException("down") }, threshold = 2)
        assertEquals("", runBlocking { summarizer.summarize(messages(5)) })
    }
}
