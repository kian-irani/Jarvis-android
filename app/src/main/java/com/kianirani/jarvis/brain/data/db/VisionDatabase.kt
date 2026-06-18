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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    @Query("SELECT COUNT(*) FROM memories WHERE (:type IS NULL OR type = :type)") suspend fun count(type: String?): Int
    @Query("DELETE FROM memories WHERE id = :id") suspend fun delete(id: String)
    @Query("DELETE FROM memories") suspend fun clear()
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
    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'pending'") suspend fun pendingCount(): Int
}

@Entity(tableName = "checkpoints", primaryKeys = ["threadId", "seq"])
data class CheckpointEntity(
    val threadId: String,
    val seq: Int,
    val cursor: String,
    val stateJson: String,
    val ts: Long,
)

@Dao
interface CheckpointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(c: CheckpointEntity)
    @Query("SELECT * FROM checkpoints WHERE threadId = :threadId ORDER BY seq DESC LIMIT 1")
    suspend fun latest(threadId: String): CheckpointEntity?
    @Query("SELECT MAX(seq) FROM checkpoints WHERE threadId = :threadId") suspend fun maxSeq(threadId: String): Int?
    @Query("SELECT * FROM checkpoints WHERE threadId = :threadId ORDER BY seq ASC") suspend fun all(threadId: String): List<CheckpointEntity>
}

/** v1→v2 (VCF-G3): additive — adds the graph checkpoint table; existing tables untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `checkpoints` (`threadId` TEXT NOT NULL, `seq` INTEGER NOT NULL, " +
                "`cursor` TEXT NOT NULL, `stateJson` TEXT NOT NULL, `ts` INTEGER NOT NULL, " +
                "PRIMARY KEY(`threadId`, `seq`))",
        )
    }
}

@Database(
    entities = [MemoryEntity::class, NodeEntity::class, TaskEntity::class, CheckpointEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class VisionDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun nodeDao(): NodeDao
    abstract fun taskDao(): TaskDao
    abstract fun checkpointDao(): CheckpointDao
}
