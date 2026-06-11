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
    @PrimaryKey val id: String,
    val type: String,
    val content: String,
    val metadata: String,
    val embedding: ByteArray?,
    val created_at: Long,
    val updated_at: Long,
)

@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val capabilities: String,
    val brain_score: Int,
    val last_seen: Long,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val payload: String,
    val status: String,
    val result: String?,
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
    @Query("SELECT * FROM nodes ORDER BY brain_score DESC") fun observe(): kotlinx.coroutines.flow.Flow<List<NodeEntity>>
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
