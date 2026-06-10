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
