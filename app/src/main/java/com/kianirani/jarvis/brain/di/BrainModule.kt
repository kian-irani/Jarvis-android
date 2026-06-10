package com.kianirani.jarvis.brain.di

import android.content.Context
import androidx.room.Room
import com.kianirani.jarvis.BuildConfig
import com.kianirani.jarvis.brain.data.ChatRepository
import com.kianirani.jarvis.brain.data.EmbeddingRepository
import com.kianirani.jarvis.brain.data.EventBus
import com.kianirani.jarvis.brain.data.FileRepository
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.data.ModelManager
import com.kianirani.jarvis.brain.data.NodeRepository
import com.kianirani.jarvis.brain.data.OnnxEmbedder
import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.brain.data.db.VisionDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrainModule {
    private const val MODEL_URL = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx"
    // Placeholder checksum fails closed until the real model hash is pinned (tracked in PLAN.md).
    private const val MODEL_SHA256 = "0000000000000000000000000000000000000000000000000000000000000000"
    private const val BUDGET = 2L * 1024 * 1024 * 1024

    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): VisionDatabase =
        Room.databaseBuilder(ctx, VisionDatabase::class.java, "vision-brain.db").build()

    @Provides @Singleton
    fun chatRepo(): ChatRepository =
        ChatRepository(keys = BuildConfig.GROQ_KEYS.split(",").filter { it.isNotBlank() })

    @Provides @Singleton
    fun embeddingRepo(@ApplicationContext ctx: Context): EmbeddingRepository {
        val dir = File(ctx.filesDir, "brain-models")
        val manager = ModelManager(dir, MODEL_SHA256, BUDGET, download = {
            OkHttpClient().newCall(Request.Builder().url(MODEL_URL).build()).execute().use { it.body!!.bytes() }
        })
        return EmbeddingRepository(manager) { OnnxEmbedder(manager.modelFile) }
    }

    @Provides @Singleton
    fun memoryRepo(db: VisionDatabase, emb: EmbeddingRepository): MemoryRepository =
        MemoryRepository(db.memoryDao()) { emb.embed(it) }

    @Provides @Singleton fun nodeRepo(db: VisionDatabase) = NodeRepository(db.nodeDao())
    @Provides @Singleton fun eventBus() = EventBus()

    @Provides @Singleton
    fun fileRepo(@ApplicationContext ctx: Context) = FileRepository(File(ctx.filesDir, "brain-files"))

    @Provides @Singleton
    fun taskRepo(db: VisionDatabase, emb: EmbeddingRepository): TaskRepository =
        TaskRepository(db.taskDao(), handlers = mapOf(
            "model.download" to { _ -> emb.ensureModel(); """{"downloaded":true}""" },
        ))
}
