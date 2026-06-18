package com.kianirani.jarvis.data.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRolesTest {

    @Test fun `every named agent has a behaviour role`() {
        for (id in AgentId.entries) {
            val role = AgentRoles.forId(id)
            assertEquals(id.name, role.id)
            assertTrue("role blank for $id", role.role.isNotBlank())
            assertTrue("goal blank for $id", role.goal.isNotBlank())
            assertTrue("backstory blank for $id", role.backstory.isNotBlank())
            assertTrue("prompt missing goal for $id", role.systemPrompt().contains(role.goal))
        }
    }

    @Test fun `all four named agents are mapped`() {
        assertEquals(AgentId.entries.size, AgentRoles.all.size)
    }

    @Test fun `device agent may use sensitive tools and developer uses none`() {
        assertTrue(AgentRoles.forId(AgentId.DEVICE).tools.contains("send_sms"))
        assertTrue(AgentRoles.forId(AgentId.DEVELOPER).tools.isEmpty())
    }
}
