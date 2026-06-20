package com.kianirani.jarvis.di

import com.kianirani.jarvis.core.agent.VisionAgent
import com.kianirani.jarvis.core.brain.VisionBrain
import com.kianirani.jarvis.core.gateway.VisionGateway
import com.kianirani.jarvis.core.graph.Checkpointer
import com.kianirani.jarvis.core.memory.MemoryEngine
import com.kianirani.jarvis.core.memory.MemoryType
import com.kianirani.jarvis.core.sdk.InProcessVisionSdk
import com.kianirani.jarvis.core.sdk.VisionSdk
import com.kianirani.jarvis.core.tools.DeviceCommandTool
import com.kianirani.jarvis.core.tools.RecallTool
import com.kianirani.jarvis.core.tools.RememberTool
import com.kianirani.jarvis.core.tools.ToolRegistry
import com.kianirani.jarvis.data.agent.CommandInterpreter
import com.kianirani.jarvis.data.ai.RouterModelClient
import com.kianirani.jarvis.data.settings.VisionSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * DS-C1 — the in-process plane. Wires the agent stack into the [VisionBrain] facade and exposes
 * a transport-agnostic [VisionSdk] so any surface (the floating widget, the launcher, a future
 * desktop shell) can inject one object and `send(...)`/`recall(...)` without knowing about the
 * gateway, the ReAct graph, or the router. The same brain, no IPC — that's the in-process plane;
 * the network plane (DS-C2) reaches the same brain over `/v1/stream`.
 */
@Module
@InstallIn(SingletonComponent::class)
object BrainFacadeModule {

    @Provides
    @Singleton
    fun provideVisionGateway(
        client: RouterModelClient,
        checkpointer: Checkpointer,
        interpreter: CommandInterpreter,
        memory: MemoryEngine,
        settings: VisionSettings,
    ): VisionGateway {
        // VCF-LIVE-2 — give the (previously dormant, tool-less) agent its real capabilities:
        // the on-device action layer (open app / time / battery / settings, via the working
        // CommandInterpreter) plus long-term memory (remember/recall over the MemoryEngine).
        // This replaces the placeholder ToolRegistry(emptyList()) so the ReAct loop can act,
        // not just chat. More device tools (Accessibility/automation) join this list later.
        val tools = ToolRegistry(
            listOf(
                DeviceCommandTool { command -> interpreter.tryHandle(command) },
                RememberTool { content -> memory.remember(content, MemoryType.FACT) != null },
                RecallTool { query, topK -> memory.recall(query, topK).map { it.content } },
            ),
        )
        // The model advertises these via native function-calling (VCF-T2); RouterModelClient
        // falls back to the text TOOL_PROTOCOL when a provider can't do FC.
        val schema = tools.functionSchema(supportsFunctionCalling = true)
        // Give the agent an identity, a language preference, and the directive to actually
        // call tools (it had none — so it answered like a bare chat model). Evaluated per run
        // so it tracks the live persona/language settings.
        return VisionGateway { _ -> VisionAgent(client, tools, checkpointer, schema, systemPrompt = { agentSystemPrompt(settings) }) }
    }

    /** Persona + tool-use + language guidance for the VCF agent's SYSTEM turn. */
    private fun agentSystemPrompt(settings: VisionSettings): String {
        val name = settings.personaName.value.ifBlank { "VISION" }
        val user = settings.userName.value.trim()
        val lang = when (settings.language.value) {
            VisionSettings.LANG_FA -> "Always reply in Persian (فارسی)."
            VisionSettings.LANG_EN -> "Always reply in English."
            else -> "Reply in the same language the user used (Persian or English)."
        }
        return buildString {
            append("You are $name, a sovereign personal AI operating system on the user's own device. ")
            if (user.isNotEmpty()) append("The user's name is $user. ")
            append(
                "You can call tools to actually act on the device: open apps, search the web or YouTube, " +
                    "tap/scroll/type on screen, call or text contacts, toggle settings, and remember/recall facts. " +
                    "Prefer calling a tool to do the task rather than describing it. Never claim you performed an " +
                    "action unless a tool returned a successful result — if a tool fails, say so honestly. ",
            )
            append(lang)
            append(" Keep replies concise and natural.")
        }
    }

    @Provides
    @Singleton
    fun provideVisionBrain(gateway: VisionGateway, memory: MemoryEngine): VisionBrain =
        VisionBrain(gateway, memory)

    @Provides
    @Singleton
    fun provideVisionSdk(brain: VisionBrain): VisionSdk = InProcessVisionSdk(brain)
}
