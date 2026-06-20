package com.kianirani.jarvis.di

import com.kianirani.jarvis.core.agent.VisionAgent
import com.kianirani.jarvis.core.brain.VisionBrain
import com.kianirani.jarvis.core.gateway.VisionGateway
import com.kianirani.jarvis.core.graph.Checkpointer
import com.kianirani.jarvis.core.memory.MemoryEngine
import com.kianirani.jarvis.core.sdk.InProcessVisionSdk
import com.kianirani.jarvis.core.sdk.VisionSdk
import com.kianirani.jarvis.core.tools.ToolRegistry
import com.kianirani.jarvis.data.ai.RouterModelClient
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
    fun provideVisionGateway(client: RouterModelClient, checkpointer: Checkpointer): VisionGateway {
        // One ReAct agent per channel; tools are added by the device tool layer over time.
        val tools = ToolRegistry(emptyList())
        return VisionGateway { _ -> VisionAgent(client, tools, checkpointer) }
    }

    @Provides
    @Singleton
    fun provideVisionBrain(gateway: VisionGateway, memory: MemoryEngine): VisionBrain =
        VisionBrain(gateway, memory)

    @Provides
    @Singleton
    fun provideVisionSdk(brain: VisionBrain): VisionSdk = InProcessVisionSdk(brain)
}
