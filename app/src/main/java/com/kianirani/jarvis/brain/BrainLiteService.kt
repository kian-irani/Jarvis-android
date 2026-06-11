package com.kianirani.jarvis.brain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kianirani.jarvis.brain.data.ChatRepository
import com.kianirani.jarvis.brain.data.EmbeddingRepository
import com.kianirani.jarvis.brain.data.EventBus
import com.kianirani.jarvis.brain.data.FileRepository
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.data.NodeRepository
import com.kianirani.jarvis.brain.data.HeartbeatSender
import com.kianirani.jarvis.brain.discovery.NsdDiscovery
import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.brain.score.LocalDeviceMetricsProvider
import com.kianirani.jarvis.brain.server.KtorServer
import com.kianirani.jarvis.brain.server.routes.HealthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class BrainLiteService : Service() {
    @Inject lateinit var chat: ChatRepository
    @Inject lateinit var embedding: EmbeddingRepository
    @Inject lateinit var memory: MemoryRepository
    @Inject lateinit var nodes: NodeRepository
    @Inject lateinit var tasks: TaskRepository
    @Inject lateinit var files: FileRepository
    @Inject lateinit var bus: EventBus
    @Inject lateinit var localMetrics: LocalDeviceMetricsProvider
    @Inject lateinit var brainStore: com.kianirani.jarvis.brain.discovery.BrainSelectionStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var server: KtorServer? = null
    private var nsd: NsdDiscovery? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification("Brain-Lite running on :7799"))
        server = KtorServer(
            healthState = HealthState(version = "16.0.0", embedReady = embedding::isReady, storageUsedBytes = embedding::usedBytes),
            chat = chat, embedPort = embedding, memory = memory, nodes = nodes,
            tasks = tasks, files = files, bus = bus,
            keyStatus = { chat.keyStatus.toList() },
        ).also { it.start() }
        tasks.startWorker(scope)
        HeartbeatSender(
            nodeId = stableNodeId(),
            nodeName = android.os.Build.MODEL ?: "android-device",
            address = "127.0.0.1:7799",
            brainBaseUrl = {
                brainStore.load()?.let { "http://${it.host}:${it.port}" } ?: "http://127.0.0.1:7799"
            },
            metrics = localMetrics::current,
        ).start(scope)
        nsd = NsdDiscovery(this).also {
            it.advertise(name = "Vision-${android.os.Build.MODEL ?: "node"}", port = 7799)
        }
    }

    /** Stable per-install node id so heartbeats refresh one registry row. */
    private fun stableNodeId(): String {
        val prefs = getSharedPreferences("brain_lite", MODE_PRIVATE)
        return prefs.getString("node_id", null) ?: java.util.UUID.randomUUID().toString()
            .also { prefs.edit().putString("node_id", it).commit() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        nsd?.stopAdvertise()
        tasks.stopWorker()
        server?.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        val channelId = "brain_lite"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(channelId, "Brain-Lite", NotificationManager.IMPORTANCE_LOW))
        return Notification.Builder(this, channelId)
            .setContentTitle("Vision Brain-Lite")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()
    }

    companion object { const val NOTIF_ID = 7799 }
}
