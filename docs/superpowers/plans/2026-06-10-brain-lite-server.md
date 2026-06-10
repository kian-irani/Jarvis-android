# Brain-Lite Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A ForegroundService-hosted Ktor server on `127.0.0.1:7799` inside the Vision Android app, exposing the 10 Brain-Lite endpoints per the spec at `docs/superpowers/specs/2026-06-10-brain-lite-server-design.md`.

**Architecture:** Pragmatic layered (Approach A): `BrainLiteService` owns a `KtorServer`; route files delegate to Hilt-injected repositories (Room, Groq HTTP, ONNX embedder). Routes never touch storage/network directly.

**Tech Stack:** Kotlin 2.1, Ktor 3.1 (server-cio + client-okhttp), Room 2.6 (KSP), Hilt 2.54, kotlinx.serialization, ONNX Runtime Android, JUnit + ktor-server-test-host + MockK + OkHttp MockWebServer.

**IMPORTANT — module location:** The buildable project is the **repo-root `app/` module** (`com.kianirani.jarvis`), NOT `android/app/` (that tree is a README skeleton). All paths below are under `app/src/`. Build with `./gradlew :app:assembleDebug` and test with `./gradlew :app:testDebugUnitTest` from the repo root.

---

## File Structure

```
app/src/main/java/com/kianirani/jarvis/brain/
├── BrainLiteService.kt
├── server/KtorServer.kt
├── server/ApiEnvelope.kt              # envelope + error codes + StatusPages config
├── server/routes/HealthRoutes.kt      # /health, /status
├── server/routes/ChatRoutes.kt        # /chat
├── server/routes/EmbedRoutes.kt       # /embed, /search
├── server/routes/MemoryRoutes.kt      # /memory
├── server/routes/NodeRoutes.kt        # /nodes
├── server/routes/TaskRoutes.kt        # /task, /task/{id}
├── server/routes/FileRoutes.kt        # /files
├── server/routes/EventRoutes.kt       # /events, /events/stream
├── data/db/VisionDatabase.kt          # Room DB + entities + DAOs (one file, 3 tables)
├── data/ChatRepository.kt             # Groq + 3-key rotation
├── data/EmbeddingRepository.kt        # Embedder interface + OnnxEmbedder + ModelManager
├── data/MemoryRepository.kt
├── data/NodeRepository.kt
├── data/TaskRepository.kt             # Room queue + coroutine worker
├── data/FileRepository.kt
├── data/EventBus.kt                   # SharedFlow event hub
└── di/BrainModule.kt
app/src/test/java/com/kianirani/jarvis/brain/   # JVM tests (one file per route/repo group)
```

Execution order below is dependency order. Commit after every task.

---

### Task 1: Dependencies + envelope + /health skeleton

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/ApiEnvelope.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/KtorServer.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/routes/HealthRoutes.kt`
- Test: `app/src/test/java/com/kianirani/jarvis/brain/HealthRoutesTest.kt`

- [ ] **Step 1: Add versions/libs to `gradle/libs.versions.toml`**

Append under `[versions]`:
```toml
room                 = "2.6.1"
onnxruntime          = "1.20.0"
mockk                = "1.13.16"
mockwebserver        = "4.12.0"
junit                = "4.13.2"
```
Append under `[libraries]`:
```toml
ktor-server-cio          = { group = "io.ktor", name = "ktor-server-cio",                        version.ref = "ktor" }
ktor-server-content-neg  = { group = "io.ktor", name = "ktor-server-content-negotiation",        version.ref = "ktor" }
ktor-server-status-pages = { group = "io.ktor", name = "ktor-server-status-pages",               version.ref = "ktor" }
ktor-server-sse          = { group = "io.ktor", name = "ktor-server-sse",                        version.ref = "ktor" }
ktor-server-test-host    = { group = "io.ktor", name = "ktor-server-test-host",                  version.ref = "ktor" }
room-runtime             = { group = "androidx.room", name = "room-runtime",                     version.ref = "room" }
room-ktx                 = { group = "androidx.room", name = "room-ktx",                         version.ref = "room" }
room-compiler            = { group = "androidx.room", name = "room-compiler",                    version.ref = "room" }
onnxruntime-android      = { group = "com.microsoft.onnxruntime", name = "onnxruntime-android",  version.ref = "onnxruntime" }
junit                    = { group = "junit", name = "junit",                                    version.ref = "junit" }
mockk                    = { group = "io.mockk", name = "mockk",                                 version.ref = "mockk" }
mockwebserver            = { group = "com.squareup.okhttp3", name = "mockwebserver",             version.ref = "mockwebserver" }
coroutines-test          = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test",  version.ref = "coroutines" }
```

- [ ] **Step 2: Add dependencies to `app/build.gradle.kts`** (inside the existing `dependencies {}` block)

```kotlin
implementation(libs.ktor.server.cio)
implementation(libs.ktor.server.content.neg)
implementation(libs.ktor.server.status.pages)
implementation(libs.ktor.server.sse)
implementation(libs.room.runtime)
implementation(libs.room.ktx)
ksp(libs.room.compiler)
implementation(libs.onnxruntime.android)
testImplementation(libs.junit)
testImplementation(libs.mockk)
testImplementation(libs.ktor.server.test.host)
testImplementation(libs.mockwebserver)
testImplementation(libs.coroutines.test)
```

- [ ] **Step 3: Create `ApiEnvelope.kt`**

```kotlin
package com.kianirani.jarvis.brain.server

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiError(val code: String, val message: String)

@Serializable
data class ApiResponse<T>(val ok: Boolean, val data: T? = null, val error: ApiError? = null)

fun <T> success(data: T) = ApiResponse(ok = true, data = data)
fun failure(code: String, message: String) =
    ApiResponse<JsonElement>(ok = false, error = ApiError(code, message))

class BrainException(val code: String, val status: HttpStatusCode, message: String) :
    Exception(message) {
    companion object {
        fun modelNotReady() = BrainException("MODEL_NOT_READY", HttpStatusCode.ServiceUnavailable, "Embedding model not downloaded yet")
        fun allKeysLimited() = BrainException("ALL_KEYS_RATE_LIMITED", HttpStatusCode.ServiceUnavailable, "All provider keys are rate limited")
        fun storageBudget() = BrainException("STORAGE_BUDGET_EXCEEDED", HttpStatusCode.InsufficientStorage, "2GB Brain-Lite storage budget exceeded")
        fun notFound(what: String) = BrainException("NOT_FOUND", HttpStatusCode.NotFound, "$what not found")
        fun validation(msg: String) = BrainException("VALIDATION", HttpStatusCode.BadRequest, msg)
    }
}

val brainJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun Application.installBrainPlugins() {
    install(ContentNegotiation) { json(brainJson) }
    install(StatusPages) {
        exception<BrainException> { call, e ->
            call.respond(e.status, failure(e.code, e.message ?: e.code))
        }
        exception<Throwable> { call, e ->
            call.respond(HttpStatusCode.InternalServerError, failure("INTERNAL", e.message ?: "internal error"))
        }
    }
}
```

- [ ] **Step 4: Write the failing test `HealthRoutesTest.kt`**

```kotlin
package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.server.installBrainPlugins
import com.kianirani.jarvis.brain.server.routes.HealthState
import com.kianirani.jarvis.brain.server.routes.healthRoutes
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthRoutesTest {
    @Test
    fun `health returns ok envelope with capabilities`() = testApplication {
        application {
            installBrainPlugins()
            routing { healthRoutes(HealthState(version = "16.0.0", embedReady = { false }, storageUsedBytes = { 0L })) }
        }
        val res = client.get("/health")
        assertEquals(HttpStatusCode.OK, res.status)
        val body = res.bodyAsText()
        assertTrue(body.contains("\"ok\":true"))
        assertTrue(body.contains("\"chat\":\"cloud\""))
        assertTrue(body.contains("\"embed\":\"local\""))
    }
}
```

- [ ] **Step 5: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*.HealthRoutesTest" 2>&1 | tail -20`
Expected: compilation FAILURE (healthRoutes not defined).

- [ ] **Step 6: Create `HealthRoutes.kt`**

```kotlin
package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.server.success
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

class HealthState(
    val version: String,
    val embedReady: () -> Boolean,
    val storageUsedBytes: () -> Long,
    val startedAtMs: Long = System.currentTimeMillis(),
)

@Serializable
data class HealthData(
    val version: String,
    val uptimeMs: Long,
    val capabilities: Map<String, String>,
    val modelReady: Boolean,
    val storageUsedBytes: Long,
)

fun Route.healthRoutes(state: HealthState) {
    get("/health") {
        call.respond(
            success(
                HealthData(
                    version = state.version,
                    uptimeMs = System.currentTimeMillis() - state.startedAtMs,
                    capabilities = mapOf("chat" to "cloud", "embed" to "local"),
                    modelReady = state.embedReady(),
                    storageUsedBytes = state.storageUsedBytes(),
                )
            )
        )
    }
}
```

- [ ] **Step 7: Create `KtorServer.kt`** (routes get wired in as later tasks land; start with health)

```kotlin
package com.kianirani.jarvis.brain.server

import com.kianirani.jarvis.brain.server.routes.HealthState
import com.kianirani.jarvis.brain.server.routes.healthRoutes
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing

class KtorServer(private val healthState: HealthState) {
    private var engine: EmbeddedServer<*, *>? = null

    fun start(port: Int = 7799) {
        engine = embeddedServer(CIO, port = port, host = "127.0.0.1") {
            installBrainPlugins()
            routing { healthRoutes(healthState) }
        }.also { it.start(wait = false) }
    }

    fun stop() { engine?.stop(1000, 2000); engine = null }
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*.HealthRoutesTest" 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/kianirani/jarvis/brain app/src/test
git commit -m "feat(brain-lite): ktor server skeleton with /health + envelope"
```

---

### Task 2: Room database (memories, nodes, tasks)

**Files:**
- Create: `app/src/main/java/com/kianirani/jarvis/brain/data/db/VisionDatabase.kt`
- Test: (instrumented-free) covered indirectly via repository tests in Tasks 5–7; DAO compile is verified by KSP in this task.

- [ ] **Step 1: Create `VisionDatabase.kt`** — schema mirrors Brain-Full PostgreSQL names

```kotlin
package com.kianirani.jarvis.brain.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,                  // UUID string
    val type: String,                            // "episodic" | "semantic"
    val content: String,
    val metadata: String,                        // JSON string
    val embedding: ByteArray?,                   // float32[384] little-endian
    val created_at: Long,
    val updated_at: Long,
)

@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val capabilities: String,                    // JSON string
    val brain_score: Int,
    val last_seen: Long,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val payload: String,                         // JSON string
    val status: String,                          // pending | running | done | failed
    val result: String?,                         // JSON string
    val created_at: Long,
    val finished_at: Long?,
)

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(m: MemoryEntity)
    @Query("SELECT * FROM memories WHERE (:type IS NULL OR type = :type) ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun list(type: String?, limit: Int, offset: Int): List<MemoryEntity>
    @Query("SELECT * FROM memories WHERE embedding IS NOT NULL") suspend fun allWithEmbedding(): List<MemoryEntity>
}

@Dao
interface NodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(n: NodeEntity)
    @Query("SELECT * FROM nodes ORDER BY brain_score DESC") suspend fun list(): List<NodeEntity>
}

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(t: TaskEntity)
    @Update suspend fun update(t: TaskEntity)
    @Query("SELECT * FROM tasks WHERE id = :id") suspend fun byId(id: String): TaskEntity?
    @Query("SELECT * FROM tasks WHERE status = 'pending' ORDER BY created_at LIMIT 1") suspend fun nextPending(): TaskEntity?
}

@Database(entities = [MemoryEntity::class, NodeEntity::class, TaskEntity::class], version = 1, exportSchema = false)
abstract class VisionDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun nodeDao(): NodeDao
    abstract fun taskDao(): TaskDao
}
```

- [ ] **Step 2: Verify it compiles (KSP generates DAO impls)**

Run: `./gradlew :app:compileDebugKotlin 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/kianirani/jarvis/brain/data/db
git commit -m "feat(brain-lite): Room schema (memories/nodes/tasks) mirroring brain-full"
```

---

### Task 3: ChatRepository — Groq with 3-key rotation

**Files:**
- Create: `app/src/main/java/com/kianirani/jarvis/brain/data/ChatRepository.kt`
- Test: `app/src/test/java/com/kianirani/jarvis/brain/ChatRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.ChatMessage
import com.kianirani.jarvis.brain.data.ChatRepository
import com.kianirani.jarvis.brain.server.BrainException
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ChatRepositoryTest {
    private val okBody = """{"choices":[{"message":{"role":"assistant","content":"hi"}}]}"""

    @Test
    fun `rotates to next key on 429`() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(429))
        server.enqueue(MockResponse().setResponseCode(200).setBody(okBody).addHeader("Content-Type", "application/json"))
        server.start()
        val repo = ChatRepository(keys = listOf("k1", "k2", "k3"), baseUrl = server.url("/").toString())
        val reply = repo.chat(listOf(ChatMessage("user", "hello")), model = null)
        assertEquals("hi", reply.content)
        assertEquals(2, server.requestCount)
        server.shutdown()
    }

    @Test
    fun `throws ALL_KEYS_RATE_LIMITED when every key 429s`() = runTest {
        val server = MockWebServer()
        repeat(3) { server.enqueue(MockResponse().setResponseCode(429)) }
        server.start()
        val repo = ChatRepository(keys = listOf("k1", "k2", "k3"), baseUrl = server.url("/").toString())
        try {
            repo.chat(listOf(ChatMessage("user", "hello")), model = null)
            fail("expected BrainException")
        } catch (e: BrainException) {
            assertEquals("ALL_KEYS_RATE_LIMITED", e.code)
        }
        server.shutdown()
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ChatRepositoryTest" 2>&1 | tail -10`
Expected: compilation FAILURE (ChatRepository not defined).

- [ ] **Step 3: Implement `ChatRepository.kt`** (OkHttp directly — already a dependency via Ktor okhttp engine)

```kotlin
package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.brainJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.atomic.AtomicInteger

@Serializable data class ChatMessage(val role: String, val content: String)
@Serializable data class ChatReply(val role: String = "assistant", val content: String)

@Serializable private data class GroqRequest(val model: String, val messages: List<ChatMessage>)
@Serializable private data class GroqChoice(val message: ChatMessage)
@Serializable private data class GroqResponse(val choices: List<GroqChoice>)

class ChatRepository(
    private val keys: List<String>,
    private val baseUrl: String = "https://api.groq.com/openai/v1/",
    private val client: OkHttpClient = OkHttpClient(),
    private val defaultModel: String = "llama-3.3-70b-versatile",
) {
    private val keyIndex = AtomicInteger(0)
    val keyStatus = Array(keys.size) { "ok" }   // surfaced by /status

    suspend fun chat(messages: List<ChatMessage>, model: String?): ChatReply = withContext(Dispatchers.IO) {
        val body = brainJson.encodeToString(GroqRequest.serializer(), GroqRequest(model ?: defaultModel, messages))
        repeat(keys.size) {
            val i = keyIndex.get() % keys.size
            val req = Request.Builder()
                .url(baseUrl + "chat/completions")
                .header("Authorization", "Bearer ${keys[i]}")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(req).execute().use { res ->
                if (res.code == 429) {
                    keyStatus[i] = "rate_limited"
                    keyIndex.incrementAndGet()
                } else if (res.isSuccessful) {
                    keyStatus[i] = "ok"
                    val parsed = brainJson.decodeFromString(GroqResponse.serializer(), res.body!!.string())
                    return@withContext ChatReply(content = parsed.choices.first().message.content)
                } else {
                    throw BrainException("PROVIDER_ERROR", io.ktor.http.HttpStatusCode.BadGateway, "Groq HTTP ${res.code}")
                }
            }
        }
        throw BrainException.allKeysLimited()
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ChatRepositoryTest" 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kianirani/jarvis/brain/data/ChatRepository.kt app/src/test/java/com/kianirani/jarvis/brain/ChatRepositoryTest.kt
git commit -m "feat(brain-lite): ChatRepository with groq 3-key rotation"
```

---

### Task 4: EmbeddingRepository — ModelManager (checksum + budget) and Embedder interface

**Files:**
- Create: `app/src/main/java/com/kianirani/jarvis/brain/data/EmbeddingRepository.kt`
- Test: `app/src/test/java/com/kianirani/jarvis/brain/ModelManagerTest.kt`

The ONNX session itself is device-only; JVM tests cover download/checksum/budget logic via a fake downloader, per the spec. `OnnxEmbedder` implements `Embedder` and is verified by an instrumented test in Task 10.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.ModelManager
import com.kianirani.jarvis.brain.server.BrainException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class ModelManagerTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun sha256(bytes: ByteArray) =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `download verifies checksum and reports ready`() = runTest {
        val payload = "model-bytes".toByteArray()
        val mm = ModelManager(
            modelDir = tmp.root, expectedSha256 = sha256(payload),
            budgetBytes = 1024, download = { payload },
        )
        mm.ensureModel()
        assertTrue(mm.isReady())
    }

    @Test
    fun `bad checksum deletes file and stays not-ready`() = runTest {
        val mm = ModelManager(
            modelDir = tmp.root, expectedSha256 = "deadbeef",
            budgetBytes = 1024, download = { "corrupt".toByteArray() },
        )
        try { mm.ensureModel(); fail("expected") } catch (e: BrainException) {
            assertEquals("MODEL_CHECKSUM_MISMATCH", e.code)
        }
        assertTrue(!mm.isReady())
    }

    @Test
    fun `budget exceeded throws STORAGE_BUDGET_EXCEEDED`() = runTest {
        val payload = ByteArray(2048)
        val mm = ModelManager(
            modelDir = tmp.root, expectedSha256 = sha256(payload),
            budgetBytes = 1024, download = { payload },
        )
        try { mm.ensureModel(); fail("expected") } catch (e: BrainException) {
            assertEquals("STORAGE_BUDGET_EXCEEDED", e.code)
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ModelManagerTest" 2>&1 | tail -10`
Expected: compilation FAILURE.

- [ ] **Step 3: Implement `EmbeddingRepository.kt`**

```kotlin
package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.server.BrainException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

interface Embedder { fun embed(texts: List<String>): List<FloatArray>; val dim: Int }

class ModelManager(
    private val modelDir: File,
    private val expectedSha256: String,
    private val budgetBytes: Long,
    private val download: suspend () -> ByteArray,
) {
    val modelFile: File get() = File(modelDir, "minilm-l12-v2.onnx")
    fun isReady(): Boolean = modelFile.exists()
    fun usedBytes(): Long = modelDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    suspend fun ensureModel() = withContext(Dispatchers.IO) {
        if (isReady()) return@withContext
        val bytes = download()
        if (usedBytes() + bytes.size > budgetBytes) throw BrainException.storageBudget()
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        if (sha != expectedSha256) {
            throw BrainException("MODEL_CHECKSUM_MISMATCH", HttpStatusCode.ServiceUnavailable, "Model checksum mismatch")
        }
        modelFile.parentFile?.mkdirs()
        modelFile.writeBytes(bytes)
    }
}

class EmbeddingRepository(private val manager: ModelManager, private val embedderFactory: () -> Embedder) {
    private var embedder: Embedder? = null
    fun isReady() = manager.isReady()
    fun usedBytes() = manager.usedBytes()
    suspend fun ensureModel() = manager.ensureModel()

    fun embed(texts: List<String>): List<FloatArray> {
        if (!manager.isReady()) throw BrainException.modelNotReady()
        val e = embedder ?: embedderFactory().also { embedder = it }
        return e.embed(texts)
    }
}
```

`OnnxEmbedder` (same file, device-only — exercised by instrumented test in Task 10):

```kotlin
class OnnxEmbedder(modelFile: File) : Embedder {
    override val dim = 384
    private val env = ai.onnxruntime.OrtEnvironment.getEnvironment()
    private val session = env.createSession(modelFile.absolutePath)

    override fun embed(texts: List<String>): List<FloatArray> =
        texts.map { text ->
            // MVP tokenizer: byte-level fallback (proper WordPiece tokenizer tracked in PLAN.md).
            val ids = text.encodeToByteArray().take(256).map { it.toLong() }.toLongArray()
            val shape = longArrayOf(1, ids.size.toLong())
            ai.onnxruntime.OnnxTensor.createTensor(env, java.nio.LongBuffer.wrap(ids), shape).use { input ->
                ai.onnxruntime.OnnxTensor.createTensor(env, java.nio.LongBuffer.wrap(LongArray(ids.size) { 1L }), shape).use { mask ->
                    session.run(mapOf("input_ids" to input, "attention_mask" to mask)).use { out ->
                        @Suppress("UNCHECKED_CAST")
                        val raw = (out[0].value as Array<Array<FloatArray>>)[0]
                        FloatArray(dim) { d -> raw.map { it[d] }.average().toFloat() }  // mean pooling
                    }
                }
            }
        }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*.ModelManagerTest" 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kianirani/jarvis/brain/data/EmbeddingRepository.kt app/src/test/java/com/kianirani/jarvis/brain/ModelManagerTest.kt
git commit -m "feat(brain-lite): embedding model manager with checksum + 2GB budget"
```

---

### Task 5: MemoryRepository + cosine search

**Files:**
- Create: `app/src/main/java/com/kianirani/jarvis/brain/data/MemoryRepository.kt`
- Test: `app/src/test/java/com/kianirani/jarvis/brain/MemoryRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests** (Mockk DAO + fake embedder — no Android runtime needed)

```kotlin
package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.Embedder
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.data.db.MemoryDao
import com.kianirani.jarvis.brain.data.db.MemoryEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeEmbedder : Embedder {
    override val dim = 4
    override fun embed(texts: List<String>) = texts.map { t ->
        if (t.contains("cat")) floatArrayOf(1f, 0f, 0f, 0f) else floatArrayOf(0f, 1f, 0f, 0f)
    }
}

class MemoryRepositoryTest {
    @Test
    fun `store embeds content and persists`() = runTest {
        val dao = mockk<MemoryDao>()
        val saved = slot<MemoryEntity>()
        coEvery { dao.insert(capture(saved)) } returns Unit
        val repo = MemoryRepository(dao) { FakeEmbedder().embed(it) }
        repo.store(type = "episodic", content = "the cat sat", metadata = "{}")
        coVerify { dao.insert(any()) }
        assertEquals("episodic", saved.captured.type)
        assertEquals(4 * 4, saved.captured.embedding!!.size)  // 4 floats LE
    }

    @Test
    fun `search ranks by cosine similarity`() = runTest {
        val dao = mockk<MemoryDao>()
        val repo0 = MemoryRepository(dao) { FakeEmbedder().embed(it) }
        val cat = repo0.toBlob(floatArrayOf(1f, 0f, 0f, 0f))
        val dog = repo0.toBlob(floatArrayOf(0f, 1f, 0f, 0f))
        coEvery { dao.allWithEmbedding() } returns listOf(
            MemoryEntity("a", "semantic", "about dogs", "{}", dog, 0, 0),
            MemoryEntity("b", "semantic", "about cats", "{}", cat, 0, 0),
        )
        val results = repo0.search("cat stuff", topK = 1)
        assertEquals("b", results.single().id)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*.MemoryRepositoryTest" 2>&1 | tail -10`
Expected: compilation FAILURE.

- [ ] **Step 3: Implement `MemoryRepository.kt`**

```kotlin
package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.data.db.MemoryDao
import com.kianirani.jarvis.brain.data.db.MemoryEntity
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.sqrt

@Serializable
data class SearchHit(val id: String, val content: String, val score: Float)

class MemoryRepository(
    private val dao: MemoryDao,
    private val embed: (List<String>) -> List<FloatArray>,
) {
    fun toBlob(v: FloatArray): ByteArray {
        val buf = ByteBuffer.allocate(v.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        v.forEach { buf.putFloat(it) }
        return buf.array()
    }

    private fun fromBlob(b: ByteArray): FloatArray {
        val buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(b.size / 4) { buf.getFloat(it * 4) }
    }

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var na = 0f; var nb = 0f
        for (i in a.indices) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i] }
        return if (na == 0f || nb == 0f) 0f else dot / (sqrt(na) * sqrt(nb))
    }

    suspend fun store(type: String, content: String, metadata: String): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val vec = embed(listOf(content)).first()
        dao.insert(MemoryEntity(id, type, content, metadata, toBlob(vec), now, now))
        return id
    }

    suspend fun list(type: String?, limit: Int, offset: Int) = dao.list(type, limit, offset)

    suspend fun search(query: String, topK: Int): List<SearchHit> {
        val q = embed(listOf(query)).first()
        return dao.allWithEmbedding()
            .map { SearchHit(it.id, it.content, cosine(q, fromBlob(it.embedding!!))) }
            .sortedByDescending { it.score }
            .take(topK)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*.MemoryRepositoryTest" 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kianirani/jarvis/brain/data/MemoryRepository.kt app/src/test/java/com/kianirani/jarvis/brain/MemoryRepositoryTest.kt
git commit -m "feat(brain-lite): memory repository with embedding store + cosine search"
```

---

### Task 6: TaskRepository (queue + coroutine worker), NodeRepository, FileRepository, EventBus

**Files:**
- Create: `app/src/main/java/com/kianirani/jarvis/brain/data/TaskRepository.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/data/NodeRepository.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/data/FileRepository.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/data/EventBus.kt`
- Test: `app/src/test/java/com/kianirani/jarvis/brain/TaskRepositoryTest.kt`
- Test: `app/src/test/java/com/kianirani/jarvis/brain/FileRepositoryTest.kt`

- [ ] **Step 1: Write failing tests**

`TaskRepositoryTest.kt`:
```kotlin
package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.brain.data.db.TaskDao
import com.kianirani.jarvis.brain.data.db.TaskEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskRepositoryTest {
    @Test
    fun `enqueue inserts pending task and worker completes it`() = runTest {
        val dao = mockk<TaskDao>(relaxed = true)
        val inserted = slot<TaskEntity>()
        coEvery { dao.insert(capture(inserted)) } returns Unit
        val repo = TaskRepository(dao, handlers = mapOf("echo" to { payload -> """{"echo":$payload}""" }))
        val id = repo.enqueue("echo", """{"x":1}""")
        assertEquals("pending", inserted.captured.status)
        coEvery { dao.nextPending() } returns inserted.captured andThen null
        repo.drainOnce()
        coVerify { dao.update(match { it.id == id && it.status == "done" && it.result == """{"echo":{"x":1}}""" }) }
    }

    @Test
    fun `handler exception marks task failed`() = runTest {
        val dao = mockk<TaskDao>(relaxed = true)
        val inserted = slot<TaskEntity>()
        coEvery { dao.insert(capture(inserted)) } returns Unit
        val repo = TaskRepository(dao, handlers = mapOf("boom" to { error("nope") }))
        repo.enqueue("boom", "{}")
        coEvery { dao.nextPending() } returns inserted.captured andThen null
        repo.drainOnce()
        coVerify { dao.update(match { it.status == "failed" }) }
    }
}
```

`FileRepositoryTest.kt`:
```kotlin
package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.FileRepository
import com.kianirani.jarvis.brain.server.BrainException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `write then read roundtrips within root`() {
        val repo = FileRepository(tmp.root)
        repo.write("notes/a.txt", "hello")
        assertEquals("hello", repo.read("notes/a.txt"))
        assertEquals(listOf("notes/a.txt"), repo.list("notes"))
    }

    @Test
    fun `path traversal is rejected`() {
        val repo = FileRepository(tmp.root)
        try { repo.read("../etc/passwd"); fail("expected") } catch (e: BrainException) {
            assertEquals("VALIDATION", e.code)
        }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*.TaskRepositoryTest" --tests "*.FileRepositoryTest" 2>&1 | tail -10`
Expected: compilation FAILURE.

- [ ] **Step 3: Implement the four files**

`TaskRepository.kt`:
```kotlin
package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.data.db.TaskDao
import com.kianirani.jarvis.brain.data.db.TaskEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class TaskRepository(
    private val dao: TaskDao,
    private val handlers: Map<String, suspend (payload: String) -> String>,
) {
    private var workerJob: Job? = null

    suspend fun enqueue(kind: String, payload: String): String {
        val id = UUID.randomUUID().toString()
        dao.insert(TaskEntity(id, kind, payload, "pending", null, System.currentTimeMillis(), null))
        return id
    }

    suspend fun byId(id: String): TaskEntity? = dao.byId(id)

    /** Process all currently-pending tasks once. Used by tests and the worker loop. */
    suspend fun drainOnce() {
        while (true) {
            val task = dao.nextPending() ?: return
            dao.update(task.copy(status = "running"))
            val handler = handlers[task.kind]
            try {
                if (handler == null) error("no handler for kind=${task.kind}")
                val result = handler(task.payload)
                dao.update(task.copy(status = "done", result = result, finished_at = System.currentTimeMillis()))
            } catch (e: Exception) {
                dao.update(task.copy(status = "failed", result = """{"error":"${e.message}"}""", finished_at = System.currentTimeMillis()))
            }
        }
    }

    fun startWorker(scope: CoroutineScope, pollMs: Long = 1000) {
        workerJob = scope.launch { while (true) { drainOnce(); delay(pollMs) } }
    }

    fun stopWorker() { workerJob?.cancel(); workerJob = null }
}
```

`NodeRepository.kt`:
```kotlin
package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.data.db.NodeDao
import com.kianirani.jarvis.brain.data.db.NodeEntity
import java.util.UUID

class NodeRepository(private val dao: NodeDao) {
    suspend fun register(name: String, address: String, capabilities: String, brainScore: Int): String {
        val id = UUID.randomUUID().toString()
        dao.upsert(NodeEntity(id, name, address, capabilities, brainScore, System.currentTimeMillis()))
        return id
    }
    suspend fun list(): List<NodeEntity> = dao.list()
}
```

`FileRepository.kt`:
```kotlin
package com.kianirani.jarvis.brain.data

import com.kianirani.jarvis.brain.server.BrainException
import java.io.File

class FileRepository(private val root: File) {
    private fun resolve(rel: String): File {
        val f = File(root, rel).canonicalFile
        if (!f.path.startsWith(root.canonicalFile.path)) throw BrainException.validation("path escapes storage root")
        return f
    }
    fun read(rel: String): String {
        val f = resolve(rel)
        if (!f.exists()) throw BrainException.notFound("file $rel")
        return f.readText()
    }
    fun write(rel: String, content: String) {
        val f = resolve(rel); f.parentFile?.mkdirs(); f.writeText(content)
    }
    fun list(rel: String): List<String> {
        val dir = resolve(rel)
        if (!dir.isDirectory) return emptyList()
        return dir.walkTopDown().filter { it.isFile }
            .map { it.relativeTo(root).path.replace(File.separatorChar, '/') }.sorted().toList()
    }
}
```

`EventBus.kt`:
```kotlin
package com.kianirani.jarvis.brain.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.Serializable

@Serializable
data class BrainEvent(val kind: String, val payload: String, val ts: Long = System.currentTimeMillis())

class EventBus {
    private val _events = MutableSharedFlow<BrainEvent>(replay = 16, extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    suspend fun publish(e: BrainEvent) = _events.emit(e)
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*.TaskRepositoryTest" --tests "*.FileRepositoryTest" 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kianirani/jarvis/brain/data app/src/test/java/com/kianirani/jarvis/brain
git commit -m "feat(brain-lite): task queue, node registry, scoped files, event bus"
```

---

### Task 7: Routes — chat, embed, search, memory

**Files:**
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/routes/ChatRoutes.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/routes/EmbedRoutes.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/routes/MemoryRoutes.kt`
- Test: `app/src/test/java/com/kianirani/jarvis/brain/CoreRoutesTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.ChatMessage
import com.kianirani.jarvis.brain.data.ChatReply
import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.data.SearchHit
import com.kianirani.jarvis.brain.server.installBrainPlugins
import com.kianirani.jarvis.brain.server.routes.ChatPort
import com.kianirani.jarvis.brain.server.routes.EmbedPort
import com.kianirani.jarvis.brain.server.routes.chatRoutes
import com.kianirani.jarvis.brain.server.routes.embedRoutes
import com.kianirani.jarvis.brain.server.routes.memoryRoutes
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreRoutesTest {
    @Test
    fun `POST chat returns assistant reply`() = testApplication {
        val chat = mockk<ChatPort>()
        coEvery { chat.chat(any(), any()) } returns ChatReply(content = "salam")
        application { installBrainPlugins(); routing { chatRoutes(chat) } }
        val res = client.post("/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"messages":[{"role":"user","content":"hi"}]}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("salam"))
    }

    @Test
    fun `POST chat with empty messages is 400 VALIDATION`() = testApplication {
        application { installBrainPlugins(); routing { chatRoutes(mockk<ChatPort>()) } }
        val res = client.post("/chat") {
            contentType(ContentType.Application.Json)
            setBody("""{"messages":[]}""")
        }
        assertEquals(HttpStatusCode.BadRequest, res.status)
        assertTrue(res.bodyAsText().contains("VALIDATION"))
    }

    @Test
    fun `POST embed before model ready is 503 MODEL_NOT_READY`() = testApplication {
        val embedPort = mockk<EmbedPort>()
        every { embedPort.isReady() } returns false
        application { installBrainPlugins(); routing { embedRoutes(embedPort, mockk<MemoryRepository>()) } }
        val res = client.post("/embed") {
            contentType(ContentType.Application.Json)
            setBody("""{"texts":["a"]}""")
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, res.status)
        assertTrue(res.bodyAsText().contains("MODEL_NOT_READY"))
    }

    @Test
    fun `POST search delegates to memory repository`() = testApplication {
        val embedPort = mockk<EmbedPort>()
        every { embedPort.isReady() } returns true
        val mem = mockk<MemoryRepository>()
        coEvery { mem.search("cats", 5) } returns listOf(SearchHit("id1", "about cats", 0.9f))
        application { installBrainPlugins(); routing { embedRoutes(embedPort, mem) } }
        val res = client.post("/search") {
            contentType(ContentType.Application.Json)
            setBody("""{"query":"cats","top_k":5}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("id1"))
    }

    @Test
    fun `POST memory stores and returns id`() = testApplication {
        val mem = mockk<MemoryRepository>()
        coEvery { mem.store("episodic", "note", "{}") } returns "uuid-1"
        application { installBrainPlugins(); routing { memoryRoutes(mem) } }
        val res = client.post("/memory") {
            contentType(ContentType.Application.Json)
            setBody("""{"type":"episodic","content":"note","metadata":"{}"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("uuid-1"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*.CoreRoutesTest" 2>&1 | tail -10`
Expected: compilation FAILURE.

- [ ] **Step 3: Implement the three route files**

`ChatRoutes.kt` (introduces `ChatPort` so routes depend on an interface, not the concrete repo):
```kotlin
package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.ChatMessage
import com.kianirani.jarvis.brain.data.ChatReply
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

interface ChatPort { suspend fun chat(messages: List<ChatMessage>, model: String?): ChatReply }

@Serializable
data class ChatRequest(val messages: List<ChatMessage>, val model: String? = null)

fun Route.chatRoutes(chat: ChatPort) {
    post("/chat") {
        val req = call.receive<ChatRequest>()
        if (req.messages.isEmpty()) throw BrainException.validation("messages must not be empty")
        call.respond(success(chat.chat(req.messages, req.model)))
    }
}
```

(Make `ChatRepository` implement `ChatPort`: change its declaration to `class ChatRepository(...) : ChatPort` and add `override` to `chat`.)

`EmbedRoutes.kt`:
```kotlin
package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

interface EmbedPort { fun isReady(): Boolean; fun embed(texts: List<String>): List<FloatArray> }

@Serializable data class EmbedRequest(val texts: List<String>)
@Serializable data class SearchRequest(val query: String, val top_k: Int = 5)

fun Route.embedRoutes(embedPort: EmbedPort, memory: MemoryRepository) {
    post("/embed") {
        val req = call.receive<EmbedRequest>()
        if (!embedPort.isReady()) throw BrainException.modelNotReady()
        if (req.texts.isEmpty()) throw BrainException.validation("texts must not be empty")
        call.respond(success(embedPort.embed(req.texts).map { it.toList() }))
    }
    post("/search") {
        val req = call.receive<SearchRequest>()
        if (!embedPort.isReady()) throw BrainException.modelNotReady()
        call.respond(success(memory.search(req.query, req.top_k)))
    }
}
```

(Make `EmbeddingRepository` implement `EmbedPort`: `class EmbeddingRepository(...) : EmbedPort` with `override fun isReady()` / `override fun embed(...)`.)

`MemoryRoutes.kt`:
```kotlin
package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.MemoryRepository
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class MemoryStoreRequest(val type: String, val content: String, val metadata: String = "{}")

@Serializable
data class MemoryListItem(val id: String, val type: String, val content: String, val metadata: String, val created_at: Long)

fun Route.memoryRoutes(memory: MemoryRepository) {
    post("/memory") {
        val req = call.receive<MemoryStoreRequest>()
        if (req.type !in setOf("episodic", "semantic")) throw BrainException.validation("type must be episodic|semantic")
        call.respond(success(mapOf("id" to memory.store(req.type, req.content, req.metadata))))
    }
    get("/memory") {
        val type = call.request.queryParameters["type"]
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
        val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
        val items = memory.list(type, limit, offset)
            .map { MemoryListItem(it.id, it.type, it.content, it.metadata, it.created_at) }
        call.respond(success(items))
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "*.CoreRoutesTest" 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kianirani/jarvis/brain app/src/test/java/com/kianirani/jarvis/brain
git commit -m "feat(brain-lite): chat/embed/search/memory routes"
```

---

### Task 8: Routes — nodes, task, files, status, events (SSE)

**Files:**
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/routes/NodeRoutes.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/routes/TaskRoutes.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/routes/FileRoutes.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/server/routes/EventRoutes.kt`
- Modify: `app/src/main/java/com/kianirani/jarvis/brain/server/routes/HealthRoutes.kt` (add `/status`)
- Test: `app/src/test/java/com/kianirani/jarvis/brain/AuxRoutesTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
package com.kianirani.jarvis.brain

import com.kianirani.jarvis.brain.data.EventBus
import com.kianirani.jarvis.brain.data.FileRepository
import com.kianirani.jarvis.brain.data.NodeRepository
import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.brain.data.db.TaskEntity
import com.kianirani.jarvis.brain.server.installBrainPlugins
import com.kianirani.jarvis.brain.server.routes.eventRoutes
import com.kianirani.jarvis.brain.server.routes.fileRoutes
import com.kianirani.jarvis.brain.server.routes.nodeRoutes
import com.kianirani.jarvis.brain.server.routes.statusRoutes
import com.kianirani.jarvis.brain.server.routes.taskRoutes
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AuxRoutesTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun `POST nodes registers and GET lists`() = testApplication {
        val nodes = mockk<NodeRepository>()
        coEvery { nodes.register("phone", "127.0.0.1:7799", "{}", 80) } returns "n1"
        coEvery { nodes.list() } returns emptyList()
        application { installBrainPlugins(); routing { nodeRoutes(nodes) } }
        val res = client.post("/nodes") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"phone","address":"127.0.0.1:7799","capabilities":"{}","brain_score":80}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("n1"))
        assertEquals(HttpStatusCode.OK, client.get("/nodes").status)
    }

    @Test
    fun `POST task enqueues and GET by id returns status`() = testApplication {
        val tasksRepo = mockk<TaskRepository>()
        coEvery { tasksRepo.enqueue("echo", """{"x":1}""") } returns "t1"
        coEvery { tasksRepo.byId("t1") } returns TaskEntity("t1", "echo", """{"x":1}""", "pending", null, 0, null)
        coEvery { tasksRepo.byId("missing") } returns null
        application { installBrainPlugins(); routing { taskRoutes(tasksRepo) } }
        val res = client.post("/task") {
            contentType(ContentType.Application.Json)
            setBody("""{"kind":"echo","payload":{"x":1}}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("t1"))
        assertTrue(client.get("/task/t1").bodyAsText().contains("pending"))
        assertEquals(HttpStatusCode.NotFound, client.get("/task/missing").status)
    }

    @Test
    fun `files roundtrip via routes`() = testApplication {
        application { installBrainPlugins(); routing { fileRoutes(FileRepository(tmp.root)) } }
        val w = client.post("/files") {
            contentType(ContentType.Application.Json)
            setBody("""{"op":"write","path":"a/b.txt","content":"hey"}""")
        }
        assertEquals(HttpStatusCode.OK, w.status)
        val r = client.post("/files") {
            contentType(ContentType.Application.Json)
            setBody("""{"op":"read","path":"a/b.txt"}""")
        }
        assertTrue(r.bodyAsText().contains("hey"))
        assertTrue(client.get("/files?path=a").bodyAsText().contains("a/b.txt"))
    }

    @Test
    fun `status reports key pool`() = testApplication {
        application {
            installBrainPlugins()
            routing { statusRoutes(keyStatus = { listOf("ok", "rate_limited", "ok") }, requestCount = { 42 }) }
        }
        val body = client.get("/status").bodyAsText()
        assertTrue(body.contains("rate_limited"))
        assertTrue(body.contains("42"))
    }

    @Test
    fun `POST events publishes to bus`() = testApplication {
        val bus = EventBus()
        application { installBrainPlugins(); routing { eventRoutes(bus) } }
        val res = client.post("/events") {
            contentType(ContentType.Application.Json)
            setBody("""{"kind":"ui.tap","payload":"{}"}""")
        }
        assertEquals(HttpStatusCode.OK, res.status)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :app:testDebugUnitTest --tests "*.AuxRoutesTest" 2>&1 | tail -10`
Expected: compilation FAILURE.

- [ ] **Step 3: Implement route files**

`NodeRoutes.kt`:
```kotlin
package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.NodeRepository
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class NodeRegisterRequest(val name: String, val address: String, val capabilities: String = "{}", val brain_score: Int = 0)

@Serializable
data class NodeItem(val id: String, val name: String, val address: String, val brain_score: Int, val last_seen: Long)

fun Route.nodeRoutes(nodes: NodeRepository) {
    post("/nodes") {
        val req = call.receive<NodeRegisterRequest>()
        call.respond(success(mapOf("id" to nodes.register(req.name, req.address, req.capabilities, req.brain_score))))
    }
    get("/nodes") {
        call.respond(success(nodes.list().map { NodeItem(it.id, it.name, it.address, it.brain_score, it.last_seen) }))
    }
}
```

`TaskRoutes.kt`:
```kotlin
package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.TaskRepository
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class TaskCreateRequest(val kind: String, val payload: JsonObject)

@Serializable
data class TaskStatusItem(val id: String, val kind: String, val status: String, val result: String?)

fun Route.taskRoutes(tasks: TaskRepository) {
    post("/task") {
        val req = call.receive<TaskCreateRequest>()
        call.respond(success(mapOf("id" to tasks.enqueue(req.kind, req.payload.toString()))))
    }
    get("/task/{id}") {
        val id = call.parameters["id"]!!
        val t = tasks.byId(id) ?: throw BrainException.notFound("task $id")
        call.respond(success(TaskStatusItem(t.id, t.kind, t.status, t.result)))
    }
}
```

`FileRoutes.kt`:
```kotlin
package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.FileRepository
import com.kianirani.jarvis.brain.server.BrainException
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable

@Serializable
data class FileOpRequest(val op: String, val path: String, val content: String? = null)

fun Route.fileRoutes(files: FileRepository) {
    post("/files") {
        val req = call.receive<FileOpRequest>()
        when (req.op) {
            "read" -> call.respond(success(mapOf("content" to files.read(req.path))))
            "write" -> {
                files.write(req.path, req.content ?: throw BrainException.validation("content required for write"))
                call.respond(success(mapOf("written" to req.path)))
            }
            else -> throw BrainException.validation("op must be read|write")
        }
    }
    get("/files") {
        call.respond(success(files.list(call.request.queryParameters["path"] ?: "")))
    }
}
```

`EventRoutes.kt`:
```kotlin
package com.kianirani.jarvis.brain.server.routes

import com.kianirani.jarvis.brain.data.BrainEvent
import com.kianirani.jarvis.brain.data.EventBus
import com.kianirani.jarvis.brain.server.brainJson
import com.kianirani.jarvis.brain.server.success
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent

fun Route.eventRoutes(bus: EventBus) {
    post("/events") {
        val e = call.receive<BrainEvent>()
        bus.publish(e)
        call.respond(success(mapOf("published" to e.kind)))
    }
    sse("/events/stream") {
        bus.events.collect { e ->
            send(ServerSentEvent(data = brainJson.encodeToString(BrainEvent.serializer(), e), event = e.kind))
        }
    }
}
```

Note: `sse()` requires `install(SSE)` — add `install(io.ktor.server.sse.SSE)` to `installBrainPlugins()` in `ApiEnvelope.kt`.

Append to `HealthRoutes.kt`:
```kotlin
@Serializable
data class StatusData(val keys: List<String>, val requestCount: Long)

fun Route.statusRoutes(keyStatus: () -> List<String>, requestCount: () -> Long) {
    get("/status") { call.respond(success(StatusData(keyStatus(), requestCount()))) }
}
```

- [ ] **Step 4: Run all tests**

Run: `./gradlew :app:testDebugUnitTest 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL, all suites green.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/kianirani/jarvis/brain app/src/test/java/com/kianirani/jarvis/brain
git commit -m "feat(brain-lite): nodes/task/files/status/events routes with SSE"
```

---

### Task 9: Hilt wiring + BrainLiteService + manifest

**Files:**
- Create: `app/src/main/java/com/kianirani/jarvis/brain/di/BrainModule.kt`
- Create: `app/src/main/java/com/kianirani/jarvis/brain/BrainLiteService.kt`
- Modify: `app/src/main/java/com/kianirani/jarvis/brain/server/KtorServer.kt` (accept all deps, mount all routes)
- Modify: `app/src/main/AndroidManifest.xml` (root app module's manifest — service + FOREGROUND_SERVICE permissions)

- [ ] **Step 1: Update `KtorServer.kt` to mount everything**

```kotlin
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
import com.kianirani.jarvis.brain.server.routes.taskRoutes
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
            intercept(io.ktor.server.application.ApplicationCallPipeline.Monitoring) { requests.incrementAndGet() }
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
            }
        }.also { it.start(wait = false) }
    }

    fun stop() { engine?.stop(1000, 2000); engine = null }
}
```

- [ ] **Step 2: Create `BrainModule.kt`**

```kotlin
package com.kianirani.jarvis.brain.di

import android.content.Context
import androidx.room.Room
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
import com.kianirani.jarvis.BuildConfig
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
    // TODO-VALUE: pin actual sha256 of the published model file before release; placeholder fails closed.
    private const val MODEL_SHA256 = "0000000000000000000000000000000000000000000000000000000000000000"
    private const val BUDGET = 2L * 1024 * 1024 * 1024  // 2GB per spec

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
```

Add to `app/build.gradle.kts` inside `defaultConfig {}`: `buildConfigField("String", "GROQ_KEYS", "\"${System.getenv("GROQ_KEYS") ?: ""}\"")` and inside `buildFeatures {}`: `buildConfig = true`.

- [ ] **Step 3: Create `BrainLiteService.kt`**

```kotlin
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
import com.kianirani.jarvis.brain.data.TaskRepository
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var server: KtorServer? = null

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
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
```

Note: WakeLock-per-request is deferred to a follow-up noted in PLAN.md — the CIO engine + foreground service keeps the process alive for MVP; per-request WakeLock needs an interceptor measured against battery impact.

- [ ] **Step 4: Register in the root app's `app/src/main/AndroidManifest.xml`**

Add inside `<manifest>`:
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
Add inside `<application>`:
```xml
<service
    android:name=".brain.BrainLiteService"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

- [ ] **Step 5: Full build + tests**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit + push**

```bash
git add -A
git commit -m "feat(brain-lite): BrainLiteService + Hilt wiring + full route mounting"
git push "https://kian-irani:${GH_TOKEN}@github.com/kian-irani/Jarvis-android.git" main
```

---

### Task 10: Instrumented ONNX smoke test (device-only, CI-skipped)

**Files:**
- Create: `app/src/androidTest/java/com/kianirani/jarvis/brain/OnnxEmbedderTest.kt`

- [ ] **Step 1: Add androidTest deps to `app/build.gradle.kts`**

```kotlin
androidTestImplementation("androidx.test.ext:junit:1.2.1")
androidTestImplementation("androidx.test:runner:1.6.2")
```
And in `defaultConfig {}`: `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`.

- [ ] **Step 2: Create the test** (guarded — skips when no model file present)

```kotlin
package com.kianirani.jarvis.brain

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kianirani.jarvis.brain.data.OnnxEmbedder
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OnnxEmbedderTest {
    @Test
    fun embeddingHasCorrectShape() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val model = File(ctx.filesDir, "brain-models/minilm-l12-v2.onnx")
        assumeTrue("model not downloaded; skipping", model.exists())
        val vectors = OnnxEmbedder(model).embed(listOf("hello world"))
        assertEquals(1, vectors.size)
        assertEquals(384, vectors.first().size)
    }
}
```

- [ ] **Step 3: Verify it compiles**

Run: `./gradlew :app:compileDebugAndroidTestKotlin 2>&1 | tail -3`
Expected: BUILD SUCCESSFUL. (Execution requires a device; CI runs JVM tests only.)

- [ ] **Step 4: Commit + push**

```bash
git add app/src/androidTest app/build.gradle.kts
git commit -m "test(brain-lite): instrumented ONNX shape smoke test"
git push "https://kian-irani:${GH_TOKEN}@github.com/kian-irani/Jarvis-android.git" main
```

---

## Post-plan follow-ups (add to PLAN.md when executing)

- Pin real `MODEL_SHA256` for the published MiniLM ONNX file (Task 9 fails closed until then).
- Proper WordPiece tokenizer for `OnnxEmbedder` (MVP uses byte-level fallback — embedding quality is degraded but shape-correct).
- WakeLock-per-request interceptor + battery measurement.
- `/chat` SSE streaming (`stream: true`) — non-streaming only in this plan.
- Wire `CI · Android` to run `:app:testDebugUnitTest` (currently ktlint-only).
