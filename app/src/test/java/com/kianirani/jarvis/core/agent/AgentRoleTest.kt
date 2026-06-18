package com.kianirani.jarvis.core.agent

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRoleTest {

    private val role = AgentRole("R1", "research assistant", "find facts", "You never fabricate.", setOf("search"))

    @Test fun `system prompt includes role goal and backstory`() {
        val prompt = role.systemPrompt()
        assertTrue(prompt.contains("research assistant"))
        assertTrue(prompt.contains("find facts"))
        assertTrue(prompt.contains("You never fabricate."))
    }

    @Test fun `round-trips through json`() {
        val json = Json
        assertEquals(role, json.decodeFromString<AgentRole>(json.encodeToString(role)))
    }
}
