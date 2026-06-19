package com.kianirani.jarvis.brain.server

import com.kianirani.jarvis.brain.data.EventBus
import com.kianirani.jarvis.brain.data.FileRepository
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.data.NodeRepository
import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.brain.server.routes.ChatPort
import com.kianirani.jarvis.brain.server.routes.EmbedPort
import com.kianirani.jarvis.brain.server.routes.HealthState
import com.kianirani.jarvis.brain.server.routes.chatRoutes
import com.kianirani.jarvis.brain.server.routes.embedRoutes
import com.kianirani.jarvis.brain.server.routes.eventRoutes
import com.kianirani.jarvis.brain.server.routes.fileRoutes
import com.kianirani.jarvis.brain.server.routes.healthRoutes
import com.kianirani.jarvis.brain.server.routes.memoryRoutes
import com.kianirani.jarvis.brain.server.routes.nodeRoutes
import com.kianirani.jarvis.brain.server.routes.statusRoutes
import com.kianirani.jarvis.brain.server.routes.streamRoutes
import com.kianirani.jarvis.brain.server.routes.taskRoutes
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import java.util.concurrent.atomic.AtomicLong

class KtorServer(
    private val healthState: HealthState,
    private val chat: ChatPort,
    private val embedPort: EmbedPort,
    private val memory: MemoryRepository,
    private val nodes: NodeRepository,
    private val tasks: TaskRepository,
    private val files: FileRepository,
    private val bus: EventBus,
    private val keyStatus: () -> List<String>,
) {
    private var engine: EmbeddedServer<*, *>? = null
    private val requests = AtomicLong(0)

    fun start(port: Int = 7799) {
        engine = embeddedServer(CIO, port = port, host = "127.0.0.1") {
            installBrainPlugins()
            intercept(ApplicationCallPipeline.Monitoring) { requests.incrementAndGet() }
            routing {
                healthRoutes(healthState)
                statusRoutes(keyStatus) { requests.get() }
                chatRoutes(chat)
                embedRoutes(embedPort, memory)
                memoryRoutes(memory)
                nodeRoutes(nodes)
                taskRoutes(tasks)
                fileRoutes(files)
                eventRoutes(bus)
                streamRoutes(chat, bus)
            }
        }.also { it.start(wait = false) }
    }

    fun stop() { engine?.stop(1000, 2000); engine = null }
}
