package com.kianirani.jarvis.core.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * AGSK acceptance: a goal routes to the specialist whose role/tools match it, ties break
 * deterministically, and an unmatched goal returns null (caller falls back to general). Pure.
 */
class SkillRouterTest {

    private val device = AgentRole(
        id = "device",
        role = "Device operator",
        goal = "control the phone — call, send SMS, open apps",
        backstory = "Handles on-device actions.",
        tools = setOf("call_phone", "send_sms", "open_app"),
    )
    private val research = AgentRole(
        id = "research",
        role = "Researcher",
        goal = "search the web and summarize news and facts",
        backstory = "Finds and synthesizes information.",
        tools = setOf("web_search"),
    )
    private val developer = AgentRole(
        id = "developer",
        role = "Developer",
        goal = "write and debug code",
        backstory = "Writes software.",
        tools = emptySet(),
    )
    private val agents = listOf(device, research, developer)

    @Test fun `a device action routes to the device agent`() {
        assertEquals("device", SkillRouter.route("call mom on the phone", agents)?.id)
    }

    @Test fun `a research request routes to the research agent`() {
        assertEquals("research", SkillRouter.route("search the web for today's news", agents)?.id)
    }

    @Test fun `a coding request routes to the developer agent`() {
        assertEquals("developer", SkillRouter.route("write code to debug this", agents)?.id)
    }

    @Test fun `tool-name segments contribute to the match`() {
        // "sms" only appears inside the device tool name send_sms
        assertEquals("device", SkillRouter.route("send an sms", agents)?.id)
    }

    @Test fun `an unmatched goal returns null`() {
        assertNull(SkillRouter.route("xyzzy frobnicate", agents))
    }

    @Test fun `ties break by agent id`() {
        val a = AgentRole("alpha", "helper", "open apps", "")
        val b = AgentRole("beta", "helper", "open apps", "")
        assertEquals("alpha", SkillRouter.route("open apps", listOf(b, a))?.id)
    }

    @Test fun `a blank goal scores zero`() {
        assertEquals(0, SkillRouter.score("   ", device))
        assertNull(SkillRouter.route("", agents))
    }
}
