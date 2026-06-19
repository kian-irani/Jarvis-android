package com.kianirani.jarvis.core.plugin

import com.kianirani.jarvis.core.graph.ContentPart
import com.kianirani.jarvis.core.tools.ToolContext
import com.kianirani.jarvis.core.tools.ToolSpec
import com.kianirani.jarvis.core.tools.VisionTool
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DS-X1 acceptance: the registry installs plugins under a capability sandbox, fails closed on
 * ungranted capabilities, drops over-reaching tools, exposes survivors as a ToolRegistry, and
 * upgrades/uninstalls cleanly. Pure, no device.
 */
class PluginRegistryTest {

    private fun tool(name: String) = object : VisionTool {
        override val spec = ToolSpec(name = name, description = name)
        override suspend fun execute(args: JsonObject, ctx: ToolContext) =
            ContentPart.ToolResult("c", name, listOf(ContentPart.Text("ok")))
    }

    private fun plugin(
        id: String,
        caps: Set<Capability>,
        tools: List<PluginTool>,
    ) = object : VisionPlugin {
        override val manifest = PluginManifest(id, id, "1.0", caps)
        override fun tools() = tools
    }

    @Test fun `a plugin requesting only granted capabilities installs and exposes its tools`() {
        val reg = PluginRegistry()
        val p = plugin("weather", setOf(Capability.NETWORK), listOf(PluginTool(tool("forecast"), setOf(Capability.NETWORK))))
        val r = reg.install(p, granted = setOf(Capability.NETWORK))
        assertTrue(r is InstallResult.Installed)
        assertEquals(listOf("forecast"), (r as InstallResult.Installed).exposedTools)
        assertEquals(listOf("forecast"), reg.tools().map { it.spec.name })
        assertTrue(reg.toolRegistry().byName("forecast") != null)
    }

    @Test fun `install fails closed when the manifest requests an ungranted capability`() {
        val reg = PluginRegistry()
        val p = plugin("spy", setOf(Capability.NETWORK, Capability.CONTACTS), listOf(PluginTool(tool("read"))))
        val r = reg.install(p, granted = setOf(Capability.NETWORK))
        assertTrue(r is InstallResult.Denied)
        assertEquals(setOf(Capability.CONTACTS), (r as InstallResult.Denied).missing)
        assertTrue(reg.installed().isEmpty())
        assertTrue(reg.tools().isEmpty())
    }

    @Test fun `a tool needing more capabilities than granted is dropped, not exposed`() {
        val reg = PluginRegistry()
        val p = plugin(
            "mixed",
            caps = setOf(Capability.NETWORK),
            tools = listOf(
                PluginTool(tool("fetch"), setOf(Capability.NETWORK)),
                PluginTool(tool("sendSms"), setOf(Capability.MESSAGING)), // over-reaches
            ),
        )
        val r = reg.install(p, granted = setOf(Capability.NETWORK)) as InstallResult.Installed
        assertEquals(listOf("fetch"), r.exposedTools)
        assertEquals(listOf("sendSms"), r.deniedTools)
        assertEquals(listOf("fetch"), reg.tools().map { it.spec.name })
    }

    @Test fun `re-installing a plugin upgrades it in place without duplicating tools`() {
        val reg = PluginRegistry()
        reg.install(plugin("p", emptySet(), listOf(PluginTool(tool("v1")))), emptySet())
        reg.install(plugin("p", emptySet(), listOf(PluginTool(tool("v2")))), emptySet())
        assertEquals(1, reg.installed().size)
        assertEquals(listOf("v2"), reg.tools().map { it.spec.name })
    }

    @Test fun `uninstall removes the plugin and its tools`() {
        val reg = PluginRegistry()
        reg.install(plugin("p", emptySet(), listOf(PluginTool(tool("t")))), emptySet())
        assertTrue(reg.uninstall("p"))
        assertFalse(reg.uninstall("p")) // already gone
        assertTrue(reg.tools().isEmpty())
    }

    @Test fun `tools from multiple plugins compose into one registry`() {
        val reg = PluginRegistry()
        reg.install(plugin("a", setOf(Capability.NETWORK), listOf(PluginTool(tool("alpha"), setOf(Capability.NETWORK)))), setOf(Capability.NETWORK))
        reg.install(plugin("b", setOf(Capability.MEMORY), listOf(PluginTool(tool("beta"), setOf(Capability.MEMORY)))), setOf(Capability.MEMORY))
        assertEquals(setOf("alpha", "beta"), reg.tools().map { it.spec.name }.toSet())
    }
}
