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
